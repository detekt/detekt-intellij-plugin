package io.gitlab.arturbosch.detekt.idea.util

import com.google.gson.GsonBuilder
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import io.gitlab.arturbosch.detekt.idea.DetektBundle
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.createDirectories
import kotlin.io.path.name

/**
 * Service responsible for managing Detekt plugin dependencies. It handles downloading plugins from Maven repositories
 * and keeping them in a local folder within the project's .idea directory.
 *
 * Reconciliation is triggered from two places: [io.gitlab.arturbosch.detekt.idea.config.DetektPluginSettings.loadState]
 * (when settings are loaded or migrated) and [io.gitlab.arturbosch.detekt.idea.config.DetektConfig.apply]
 * (when the user clicks Apply/OK in the settings UI). If the user applies settings shortly after a load
 * (e.g. project open), both can queue a background reconciliation with different plugin lists. Without
 * serialization, two runs could execute concurrently and interleave file operations (downloads, cleanup,
 * writing downloaded.json), leading to inconsistent on-disk state or the loadState run overwriting the
 * apply() result. [reconciliationLock] ensures only one reconciliation runs at a time; the other task
 * waits and then runs with its plugin list, so the final state is always consistent with the last request.
 */
@Suppress("TooManyFunctions") // Unavoidable due to complex logic
@Service(Service.Level.PROJECT)
class PluginDependencyService(private val project: Project) {
    private val logger = logger<PluginDependencyService>()
    private val gson = GsonBuilder().setPrettyPrinting().create()

    /**
     * Serializes reconciliation so only one download/cleanup run executes at a time.
     */
    private val reconciliationLock = Any()

    /**
     * Returns the path to the folder where Detekt plugins are stored.
     */
    fun getPluginFolder(): Path {
        val basePath = project.basePath ?: return Paths.get(".idea", "detektPlugins")
        return Paths.get(basePath, ".idea", "detektPlugins")
    }

    /**
     * Returns the path to the JSON file tracking downloaded plugins.
     */
    private fun getDownloadedFile(): Path = getPluginFolder().resolve("downloaded.json")

    /**
     * Reconciles the local plugin folder with the provided Detekt plugins. This will trigger a background task to
     * download any missing plugins and clean up unused ones.
     */
    fun reconcilePlugins(plugins: List<DetektPlugin>) {
        object : Task.Backgroundable(project, DetektBundle.message("detekt.progress.downloadingPlugins"), true) {
            override fun run(indicator: ProgressIndicator) {
                synchronized(reconciliationLock) {
                    downloadPlugins(plugins, indicator)
                }
            }

            override fun onSuccess() {
                DaemonCodeAnalyzer.getInstance(project).restart()
            }
        }
            .queue()
    }

    /**
     * Downloads the specified plugins and their transitive dependencies. Core Detekt and Kotlin dependencies are
     * automatically filtered out. Granular exclusions defined in [DetektPlugin] are also respected.
     */
    @RequiresBackgroundThread
    private fun downloadPlugins(plugins: List<DetektPlugin>, indicator: ProgressIndicator) {
        val folder = getPluginFolder()
        val stateFile = getDownloadedFile()
        logger.info("Starting download/reconcile of ${plugins.size} plugins in $folder")

        // 1. Resolve all plugins to determine allowed files
        val resolver = MavenDependencyResolver(project)
        val allowedFilesStr = mutableSetOf<String>() // Relative paths
        val resolvedArtifacts = mutableListOf<ResolvedArtifact>()

        val newStatePlugins = mutableListOf<PluginState>()
        val transitiveFilesMap = mutableMapOf<String, TransitiveFile>()

        for (plugin in plugins) {
            indicator.text = "Resolving ${plugin.coordinate}..."
            logger.debug("Resolving plugin: ${plugin.coordinate}")
            resolvePlugin(
                resolver,
                plugin,
                indicator,
                allowedFilesStr,
                resolvedArtifacts,
                transitiveFilesMap,
                newStatePlugins,
            )
        }

        if (!Files.exists(folder)) {
            folder.createDirectories()
        }

        // 2. Delete orphaned files and empty folders
        cleanupOrphanedFiles(folder, allowedFilesStr)

        // 3. Copy missing jars
        syncMissingJars(resolvedArtifacts, folder)

        // 4. Save new state
        saveNewState(newStatePlugins, transitiveFilesMap, stateFile)
    }

    @Suppress("LongParameterList") // Not really simplifiable
    private fun resolvePlugin(
        resolver: MavenDependencyResolver,
        plugin: DetektPlugin,
        indicator: ProgressIndicator,
        allowedFilesStr: MutableSet<String>,
        resolvedArtifacts: MutableList<ResolvedArtifact>,
        transitiveFilesMap: MutableMap<String, TransitiveFile>,
        newStatePlugins: MutableList<PluginState>,
    ) {
        val rootNode =
            resolver.resolve(
                coordinate = plugin.coordinate,
                shouldDownload = true,
                exclusions = plugin.exclusions.toList(),
                indicator = indicator,
            )

        if (rootNode != null) {
            logger.debug("Successfully resolved ${plugin.coordinate}")
            val allowedNodes = collectAllowedNodes(rootNode, plugin.exclusions)

            val transitiveCoords = mutableListOf<String>()
            var pluginPath: String? = null

            for (node in allowedNodes) {
                val file = node.file ?: continue
                val relativePath = coordToRelativePath(node.coordinate, file.name)

                // Add to global set for orphan cleanup and copying
                allowedFilesStr.add(relativePath)
                resolvedArtifacts.add(ResolvedArtifact(node.coordinate, relativePath, file))

                if (node.coordinate == plugin.coordinate) {
                    pluginPath = relativePath
                } else {
                    transitiveCoords.add(node.coordinate)
                    transitiveFilesMap[node.coordinate] = TransitiveFile(relativePath)
                }
            }

            newStatePlugins.add(
                PluginState(
                    coordinate = plugin.coordinate,
                    transitiveDependencies = transitiveCoords,
                    path = pluginPath ?: "",
                )
            )
        } else {
            logger.error("Failed to resolve plugin: ${plugin.coordinate}. It will be skipped.")
        }
    }

    /**
     * Traverses the dependency tree and collects all nodes that are allowed to be downloaded. Nodes are excluded if
     * they are provided by the environment or explicitly unchecked by the user.
     *
     * Includes cycle detection to prevent [StackOverflowError] in case of cyclic dependencies.
     */
    internal fun collectAllowedNodes(
        node: MavenDependencyResolver.ResolvedNode,
        exclusions: Set<String>,
    ): List<MavenDependencyResolver.ResolvedNode> {
        val result = mutableListOf<MavenDependencyResolver.ResolvedNode>()

        @Suppress("ReturnCount") // No idea how to trim it further
        fun traverse(n: MavenDependencyResolver.ResolvedNode, visited: Set<String>) {
            if (visited.contains(n.coordinate)) {
                logger.warn("Cycle detected during collection at ${n.coordinate}. Skipping children.")
                return
            }

            if (ProvidedDependencies.isProvided(n.groupId, n.artifactId)) return

            val ga = n.coordinate.substringBeforeLast(':')
            if (exclusions.contains(n.coordinate) || exclusions.contains(ga)) {
                logger.debug("Dependency explicitly excluded: ${n.coordinate}")
                return
            }

            result.add(n)
            val newVisited = visited + n.coordinate
            n.children.forEach { traverse(it, newVisited) }
        }

        traverse(node, emptySet())
        return result.distinctBy { it.coordinate }
    }

    private fun cleanupOrphanedFiles(folder: Path, allowedFilesStr: MutableSet<String>) {
        Files.walk(folder).use { walk ->
            walk.filter { Files.isRegularFile(it) && it.name.endsWith(".jar") }
                .forEach { file -> cleanupOrphanedJars(folder, file, allowedFilesStr) }
        }

        // Cleanup empty directories (bottom-up)
        Files.walk(folder).use { walk ->
            walk.filter { Files.isDirectory(it) && it != folder }
                .sorted(Comparator.reverseOrder())
                .forEach { dir -> cleanupEmptyDirectory(dir, folder) }
        }
    }

    private fun cleanupOrphanedJars(folder: Path, file: Path, allowedFilesStr: MutableSet<String>) {
        val relative = FileUtil.toSystemIndependentName(folder.relativize(file).toString())
        if (relative !in allowedFilesStr) {
            logger.debug("Deleting orphaned jar: $relative")
            try {
                Files.delete(file)
            } catch (e: IOException) {
                logger.warn("Failed to delete orphaned jar: $relative", e)
            }
        }
    }

    private fun cleanupEmptyDirectory(dir: Path, folder: Path) {
        try {
            Files.newDirectoryStream(dir).use { stream ->
                if (!stream.iterator().hasNext()) {
                    logger.debug("Deleting empty directory: ${folder.relativize(dir)}")
                    Files.delete(dir)
                }
            }
        } catch (e: IOException) {
            logger.warn("Failed to delete potentially empty directory: $dir", e)
        }
    }

    private fun syncMissingJars(resolvedArtifacts: MutableList<ResolvedArtifact>, folder: Path) {
        resolvedArtifacts
            .distinctBy { it.relativePath }
            .forEach { artifact ->
                val target = folder.resolve(artifact.relativePath)
                if (!Files.exists(target)) {
                    logger.debug("Copying ${artifact.file.name} to $target")
                    try {
                        Files.createDirectories(target.parent)
                        FileUtil.copy(artifact.file, target.toFile())
                    } catch (e: IOException) {
                        logger.error("Failed to copy ${artifact.file} to $target", e)
                    }
                }
            }
    }

    private fun saveNewState(
        newStatePlugins: MutableList<PluginState>,
        transitiveFilesMap: MutableMap<String, TransitiveFile>,
        stateFile: Path,
    ) {
        val newState = DownloadedState(plugins = newStatePlugins, transitiveFiles = transitiveFilesMap)
        try {
            Files.writeString(stateFile, gson.toJson(newState))
            logger.info("Saved downloaded state to $stateFile")
        } catch (e: IOException) {
            logger.error("Failed to save downloaded state", e)
        }
    }

    /**
     * Represents the persistent state of downloaded plugins.
     */
    private data class DownloadedState(
        val plugins: List<PluginState> = emptyList(),
        val transitiveFiles: Map<String, TransitiveFile> = emptyMap(),
    )

    /**
     * Represents the state of a single plugin and its transitive dependencies.
     */
    private data class PluginState(val coordinate: String, val transitiveDependencies: List<String>, val path: String)

    /**
     * Represents a file path for a transitive dependency.
     */
    private data class TransitiveFile(val path: String)

    /**
     * Represents an artifact that has been resolved and is ready to be copied.
     */
    private data class ResolvedArtifact(val coord: String, val relativePath: String, val file: File)
}
