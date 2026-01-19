package io.gitlab.arturbosch.detekt.idea.config.ui

import com.intellij.jarRepository.JarRepositoryManager
import com.intellij.jarRepository.RepositoryArtifactDescription
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.util.concurrency.AppExecutorUtil
import com.intellij.util.text.VersionComparatorUtil
import io.gitlab.arturbosch.detekt.idea.util.MIN_COORDINATES_PARTS
import org.jetbrains.annotations.VisibleForTesting
import org.jetbrains.idea.maven.utils.library.RepositoryLibraryDescription
import org.jetbrains.idea.maven.utils.library.RepositoryLibraryProperties
import java.util.concurrent.Callable
import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Default timeout for synchronous version fetching operations.
 */
private val MAVEN_FETCH_REQUEST_TIMEOUT = 5000.milliseconds

/**
 * Default timeout for Maven search operations.
 */
private val MAVEN_SEARCH_REQUEST_TIMEOUT = 10_000.milliseconds

/**
 * Timeout for executor shutdown; allows running tasks to respond to interrupt.
 */
private const val SHUTDOWN_TIMEOUT_MS = 5000L

/**
 * Maximum number of concurrent version fetch requests.
 */
private val maxConcurrentVersionFetches = Runtime.getRuntime().availableProcessors().coerceAtLeast(1)

/**
 * Represents a Maven artifact with all its available versions pre-fetched.
 *
 * @property groupId The Maven group ID (e.g., "io.gitlab.arturbosch.detekt").
 * @property artifactId The Maven artifact ID (e.g., "detekt-formatting").
 * @property versions All available versions, sorted in descending order (latest first).
 */
data class MavenArtifact(val groupId: String, val artifactId: String, val versions: List<String>) {
    /**
     * The latest (highest) available version, or `null` if no versions are available.
     */
    val latestVersion: String?
        get() = versions.firstOrNull()

    /**
     * Represents a parsed Maven GAV coordinate.
     */
    data class MavenGav(val groupId: String, val artifactId: String, val version: String?)
}

fun interface ArtifactSearchBackend {
    fun search(project: Project, query: String, onResults: (List<RepositoryArtifactDescription>) -> Unit)
}

fun interface VersionsFetcher {
    fun fetch(artifacts: List<RepositoryArtifactDescription>): List<MavenArtifact>
}

/**
 * Fetches Maven artifacts from configured repositories.
 *
 * This class provides a simple API to search for Maven artifacts. It handles all the complexity of:
 * - Searching repositories via [JarRepositoryManager]
 * - Deduplicating results by groupId:artifactId (keeping the highest version)
 * - Pre-fetching all available versions for each artifact
 * - Sorting versions in descending order (latest first)
 *
 * Usage:
 * ```kotlin
 * val fetcher = MavenArtifactFetcher(project)
 * fetcher.searchArtifacts("detekt") { artifacts ->
 *     // artifacts is List<MavenArtifact> with versions pre-fetched
 *     artifacts.forEach { println("${it.groupId}:${it.artifactId} - ${it.latestVersion}") }
 * }
 * ```
 *
 * @param project The IntelliJ project context used for Maven repository operations.
 * @param requestTimeout Timeout for version fetching. Defaults to [MAVEN_FETCH_REQUEST_TIMEOUT].
 * @param searchTimeout Timeout for Maven search operations. Defaults to [MAVEN_SEARCH_REQUEST_TIMEOUT].
 */
class MavenArtifactFetcher(
    private val project: Project,
    private val requestTimeout: Duration = MAVEN_FETCH_REQUEST_TIMEOUT,
    private val searchTimeout: Duration = MAVEN_SEARCH_REQUEST_TIMEOUT,
    internal val searchBackend: ArtifactSearchBackend = JarRepositoryArtifactSearchBackend,
    versionsFetcher: VersionsFetcher? = null,
    internal val scheduler: ScheduledExecutorService = AppExecutorUtil.getAppScheduledExecutorService(),
) {
    private val logger = thisLogger()
    private val versionsFetcher: VersionsFetcher =
        versionsFetcher ?: VersionsFetcher { artifacts -> fetchVersionsInParallel(artifacts) }

    /**
     * Searches for Maven artifacts matching the given query.
     *
     * This method searches Maven Central and other configured repositories, deduplicates results, and pre-fetches all
     * available versions for each artifact in parallel (up to [maxConcurrentVersionFetches] at a time). The callback
     * is invoked with the complete results.
     *
     * The search and version fetching happen asynchronously. The callback is invoked on a background thread - callers
     * should handle EDT switching if needed for UI updates.
     *
     * @param query The search query (e.g., "detekt" or "io.gitlab.arturbosch").
     * @param onResults Callback invoked with the list of artifacts, each with pre-fetched versions sorted in descending
     *   order.
     */
    fun searchArtifacts(query: String, onResults: (List<MavenArtifact>) -> Unit) {
        logger.debug("Searching for artifacts with query: $query")

        val completed = AtomicBoolean(false)
        val timeoutFuture =
            scheduler.schedule(
                {
                    if (completed.compareAndSet(false, true)) {
                        logger.warn("Maven search timed out after ${searchTimeout.inWholeMilliseconds}ms for $query")
                        onResults(emptyList())
                    }
                },
                searchTimeout.inWholeMilliseconds,
                TimeUnit.MILLISECONDS,
            )

        try {
            searchBackend.search(project, query) { artifacts ->
                if (!completed.compareAndSet(false, true)) return@search
                timeoutFuture.cancel(false)

                logger.debug("Search backend returned ${artifacts.size} results for query: $query")
                val deduplicated = deduplicateByVersion(artifacts)

                fetchVersionsAsync(deduplicated, query, onResults)
            }
        } catch (e: IllegalStateException) {
            if (completed.compareAndSet(false, true)) {
                timeoutFuture.cancel(false)
                logger.warn("Maven search failed for query: $query", e)
                onResults(emptyList())
            }
        } catch (e: IllegalArgumentException) {
            if (completed.compareAndSet(false, true)) {
                timeoutFuture.cancel(false)
                logger.warn("Maven search failed for query: $query", e)
                onResults(emptyList())
            }
        }
    }

    private fun fetchVersionsAsync(
        deduplicated: List<RepositoryArtifactDescription>,
        query: String,
        onResults: (List<MavenArtifact>) -> Unit,
    ) {
        ApplicationManager.getApplication().executeOnPooledThread {
            logger.debug("Fetching versions for ${deduplicated.size} artifacts in parallel")

            val mavenArtifacts =
                try {
                    versionsFetcher.fetch(deduplicated)
                } catch (e: TimeoutException) {
                    logger.warn("Failed to fetch versions for query: $query. Falling back to latest only.", e)
                    fallbackArtifacts(deduplicated)
                } catch (e: ExecutionException) {
                    logger.warn("Failed to fetch versions for query: $query. Falling back to latest only.", e)
                    fallbackArtifacts(deduplicated)
                } catch (e: CancellationException) {
                    logger.warn("Failed to fetch versions for query: $query. Falling back to latest only.", e)
                    fallbackArtifacts(deduplicated)
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                    logger.warn("Failed to fetch versions for query: $query. Falling back to latest only.", e)
                    fallbackArtifacts(deduplicated)
                } catch (e: IllegalStateException) {
                    logger.warn("Failed to fetch versions for query: $query. Falling back to latest only.", e)
                    fallbackArtifacts(deduplicated)
                } catch (e: IllegalArgumentException) {
                    logger.warn("Failed to fetch versions for query: $query. Falling back to latest only.", e)
                    fallbackArtifacts(deduplicated)
                }

            val sortedResults = mavenArtifacts.sortedWith(compareBy({ it.groupId }, { it.artifactId }))
            logger.debug("Returning ${sortedResults.size} artifacts with pre-fetched versions")
            onResults(sortedResults)
        }
    }

    private fun fallbackArtifacts(artifacts: List<RepositoryArtifactDescription>): List<MavenArtifact> =
        artifacts.map {
            MavenArtifact(it.groupId, it.artifactId, listOf(it.version))
        }

    private fun fallbackArtifact(artifact: RepositoryArtifactDescription): MavenArtifact =
        MavenArtifact(artifact.groupId, artifact.artifactId, listOf(artifact.version))

    private fun fetchVersionsInParallel(artifacts: List<RepositoryArtifactDescription>): List<MavenArtifact> {
        if (artifacts.isEmpty()) return emptyList()

        val callables =
            artifacts.map { artifact ->
                Callable {
                    val versions = fetchVersions(artifact.groupId, artifact.artifactId)
                    MavenArtifact(
                        groupId = artifact.groupId,
                        artifactId = artifact.artifactId,
                        versions = versions.ifEmpty { listOf(artifact.version) },
                    )
                }
            }

        return runCallablesWithTimeoutAndShutdownUsingFallback(
            callables = callables,
            timeoutMs = requestTimeout.inWholeMilliseconds,
            shutdownTimeoutMs = SHUTDOWN_TIMEOUT_MS,
            onTimeout = { logger.warn("Version fetch timed out after ${requestTimeout.inWholeMilliseconds}ms", it) },
            onFailure = { logger.warn("Failed to fetch versions", it) },
            onShutdownInterrupted = { logger.debug("Interrupted while waiting for executor shutdown", it) },
            fallback = { index -> fallbackArtifact(artifacts[index]) },
        )
    }

    private fun fetchVersions(groupId: String, artifactId: String): List<String> {
        val key = createArtifactKey(groupId, artifactId)
        logger.debug("Fetching versions for $key")

        val props = RepositoryLibraryProperties(key, "jar", true)
        val libraryDescription = RepositoryLibraryDescription.findDescription(props)

        @Suppress("TooGenericExceptionCaught") // IJPL throws generic exceptions :(
        return try {
            val timeout = requestTimeout.inWholeMilliseconds.toInt()
            val versions =
                retryOnceOnTimeout {
                    JarRepositoryManager.getAvailableVersions(project, libraryDescription).blockingGet(timeout)
                }

            if (versions != null) {
                logger.debug("Fetched ${versions.size} versions for $key")
                sortVersionsDescending(versions)
            } else {
                logger.debug("No versions found for $key")
                emptyList()
            }
        } catch (e: TimeoutException) {
            logger.warn("Failed to fetch versions for $key after retry", e)
            emptyList()
        } catch (e: Exception) {
            logger.warn("Failed to fetch versions for $key", e)
            emptyList()
        }
    }

    companion object {
        @VisibleForTesting
        internal fun <T> retryOnceOnTimeout(action: () -> T): T =
            try {
                action()
            } catch (_: TimeoutException) {
                action()
            }

        /**
         * Runs callables on a fixed thread pool with per-future timeout and ensures the executor is shut down via
         * [java.util.concurrent.ExecutorService.shutdownNow] in all cases (success, timeout, or exception). Visible for
         * testing timeout and shutdown behavior.
         */
        @VisibleForTesting
        internal fun <T> runCallablesWithTimeoutAndShutdown(
            callables: List<Callable<T>>,
            timeoutMs: Long,
            shutdownTimeoutMs: Long,
            onTimeout: (TimeoutException) -> Unit = {},
            onShutdownInterrupted: (InterruptedException) -> Unit = {},
        ): List<T> {
            if (callables.isEmpty()) return emptyList()

            val poolSize = callables.size.coerceAtMost(maxConcurrentVersionFetches)
            val executor = Executors.newFixedThreadPool(poolSize)
            try {
                val futures = callables.map { executor.submit(it) }
                return futures.map { future ->
                    try {
                        future.get(timeoutMs, TimeUnit.MILLISECONDS)
                    } catch (e: TimeoutException) {
                        future.cancel(true)
                        onTimeout(e)
                        throw e
                    } catch (e: InterruptedException) {
                        future.cancel(true)
                        Thread.currentThread().interrupt()
                        throw e
                    }
                }
            } finally {
                executor.shutdownNow()
                try {
                    executor.awaitTermination(shutdownTimeoutMs, TimeUnit.MILLISECONDS)
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                    onShutdownInterrupted(e)
                }
            }
        }

        /**
         * Runs callables on a fixed thread pool with per-future timeout and ensures the executor is shut down via
         * [java.util.concurrent.ExecutorService.shutdownNow]. Failures or timeouts return a fallback value for that
         * callable instead of failing the whole batch.
         */
        @Suppress("LongParameterList") // Unavoidable within reason
        @VisibleForTesting
        internal fun <T> runCallablesWithTimeoutAndShutdownUsingFallback(
            callables: List<Callable<T>>,
            timeoutMs: Long,
            shutdownTimeoutMs: Long,
            onTimeout: (TimeoutException) -> Unit = {},
            onFailure: (Exception) -> Unit = {},
            onShutdownInterrupted: (InterruptedException) -> Unit = {},
            fallback: (Int) -> T,
        ): List<T> {
            if (callables.isEmpty()) return emptyList()

            val poolSize = callables.size.coerceAtMost(maxConcurrentVersionFetches)
            val executor = Executors.newFixedThreadPool(poolSize)
            try {
                val futures = callables.map { executor.submit(it) }
                return futures.mapIndexed { index, future ->
                    try {
                        future.get(timeoutMs, TimeUnit.MILLISECONDS)
                    } catch (e: TimeoutException) {
                        future.cancel(true)
                        onTimeout(e)
                        fallback(index)
                    } catch (e: ExecutionException) {
                        onFailure(e)
                        fallback(index)
                    } catch (e: CancellationException) {
                        onFailure(e)
                        fallback(index)
                    } catch (e: InterruptedException) {
                        future.cancel(true)
                        Thread.currentThread().interrupt()
                        onFailure(e)
                        fallback(index)
                    }
                }
            } finally {
                executor.shutdownNow()
                try {
                    executor.awaitTermination(shutdownTimeoutMs, TimeUnit.MILLISECONDS)
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                    onShutdownInterrupted(e)
                }
            }
        }

        /**
         * Creates a key for artifact identification.
         *
         * @return A key in the format "groupId:artifactId".
         */
        @JvmStatic
        fun createArtifactKey(groupId: String, artifactId: String): String = "$groupId:$artifactId"

        /**
         * Sorts versions in descending order (latest first).
         *
         * Uses [VersionComparatorUtil] for semantic version comparison.
         */
        @JvmStatic
        fun sortVersionsDescending(versions: Collection<String>): List<String> =
            versions.sortedWith(VersionComparatorUtil.COMPARATOR.reversed())

        /**
         * Deduplicates artifacts by groupId:artifactId, keeping the highest version.
         *
         * @param artifacts The artifacts to deduplicate.
         * @return A list of unique artifacts, each with the highest available version.
         * @see VersionComparatorUtil
         */
        @JvmStatic
        fun deduplicateByVersion(
            artifacts: Collection<RepositoryArtifactDescription>,
        ): List<RepositoryArtifactDescription> {
            val bestResults = mutableMapOf<String, RepositoryArtifactDescription>()
            artifacts.forEach { artifact ->
                val key = createArtifactKey(artifact.groupId, artifact.artifactId)
                val current = bestResults[key]
                if (current == null || VersionComparatorUtil.compare(artifact.version, current.version) > 0) {
                    bestResults[key] = artifact
                }
            }
            return bestResults.values.toList()
        }

        /**
         * Parses a Maven GAV coordinate from a string.
         *
         * Supports:
         * - groupId:artifactId:version
         * - groupId:artifactId:packaging:version
         *
         * @return The parsed [MavenArtifact.MavenGav], or `null` if the format is invalid.
         */
        @JvmStatic
        fun parseGav(query: String): MavenArtifact.MavenGav? {
            val parts = query.split(":").map { it.trim() }
            if (parts.size < 2 || parts.any { it.isEmpty() }) return null

            val groupId = parts[0]
            val artifactId = parts[1]
            val version = if (parts.size >= MIN_COORDINATES_PARTS) parts.last() else null

            return MavenArtifact.MavenGav(groupId, artifactId, version)
        }
    }
}

private object JarRepositoryArtifactSearchBackend : ArtifactSearchBackend {
    override fun search(project: Project, query: String, onResults: (List<RepositoryArtifactDescription>) -> Unit) {
        JarRepositoryManager.searchArtifacts(project, query) { results ->
            onResults(results.map { it.first })
        }
    }
}
