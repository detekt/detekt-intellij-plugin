package io.gitlab.arturbosch.detekt.idea.config.ui

import com.intellij.jarRepository.RepositoryArtifactDescription
import io.gitlab.arturbosch.detekt.idea.MockProjectTestCase
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration.Companion.milliseconds

class MavenArtifactFetcherTest : MockProjectTestCase() {

    @Nested
    inner class MavenArtifactTest {

        @Test
        fun `latestVersion returns first version`() {
            val artifact = MavenArtifact("group", "artifact", listOf("2.0.0", "1.0.0"))
            assertThat(artifact.latestVersion).isEqualTo("2.0.0")
        }

        @Test
        fun `latestVersion returns null for empty versions`() {
            val artifact = MavenArtifact("group", "artifact", emptyList())
            assertThat(artifact.latestVersion).isNull()
        }
    }

    @Nested
    inner class DeduplicateByVersion {

        @Test
        fun `deduplicates artifacts by highest version`() {
            val artifacts =
                listOf(
                    createArtifact("group", "artifact", "1.0.0"),
                    createArtifact("group", "artifact", "1.1.0"),
                    createArtifact("group", "artifact", "1.0.1"),
                    createArtifact("other", "artifact", "2.0.0"),
                )

            val result = MavenArtifactFetcher.deduplicateByVersion(artifacts)

            assertThat(result).hasSize(2)
            val groupArtifact = result.find { it.groupId == "group" && it.artifactId == "artifact" }
            assertThat(groupArtifact?.version).isEqualTo("1.1.0")

            val otherArtifact = result.find { it.groupId == "other" && it.artifactId == "artifact" }
            assertThat(otherArtifact?.version).isEqualTo("2.0.0")
        }

        @Test
        fun `handles empty collection`() {
            val result = MavenArtifactFetcher.deduplicateByVersion(emptyList())
            assertThat(result).isEmpty()
        }

        @Test
        fun `handles single artifact`() {
            val artifacts = listOf(createArtifact("group", "artifact", "1.0.0"))

            val result = MavenArtifactFetcher.deduplicateByVersion(artifacts)

            assertThat(result).hasSize(1)
            assertThat(result[0].version).isEqualTo("1.0.0")
        }

        @Test
        fun `compares semantic versions correctly`() {
            val artifacts =
                listOf(
                    createArtifact("group", "artifact", "1.9.0"),
                    createArtifact("group", "artifact", "1.10.0"),
                    createArtifact("group", "artifact", "1.2.0"),
                )

            val result = MavenArtifactFetcher.deduplicateByVersion(artifacts)

            assertThat(result).hasSize(1)
            assertThat(result[0].version).isEqualTo("1.10.0")
        }

        @Test
        fun `handles prerelease versions`() {
            val artifacts =
                listOf(
                    createArtifact("group", "artifact", "1.0.0-alpha"),
                    createArtifact("group", "artifact", "1.0.0"),
                    createArtifact("group", "artifact", "1.0.0-beta"),
                )

            val result = MavenArtifactFetcher.deduplicateByVersion(artifacts)

            assertThat(result).hasSize(1)
            assertThat(result[0].version).isEqualTo("1.0.0")
        }

        @Test
        fun `keeps multiple artifacts with different groupIds`() {
            val artifacts =
                listOf(
                    createArtifact("io.detekt", "api", "1.0.0"),
                    createArtifact("io.other", "api", "2.0.0"),
                    createArtifact("io.another", "api", "3.0.0"),
                )

            val result = MavenArtifactFetcher.deduplicateByVersion(artifacts)

            assertThat(result).hasSize(3)
        }

        @Test
        fun `keeps multiple artifacts with different artifactIds`() {
            val artifacts =
                listOf(
                    createArtifact("io.detekt", "api", "1.0.0"),
                    createArtifact("io.detekt", "core", "2.0.0"),
                    createArtifact("io.detekt", "rules", "3.0.0"),
                )

            val result = MavenArtifactFetcher.deduplicateByVersion(artifacts)

            assertThat(result).hasSize(3)
        }
    }

    @Nested
    inner class SortVersionsDescending {

        @Test
        fun `sorts versions in descending order`() {
            val versions = listOf("1.0.0", "2.0.0", "1.5.0", "3.0.0")

            val result = MavenArtifactFetcher.sortVersionsDescending(versions)

            assertThat(result).containsExactly("3.0.0", "2.0.0", "1.5.0", "1.0.0")
        }

        @Test
        fun `handles empty collection`() {
            val result = MavenArtifactFetcher.sortVersionsDescending(emptyList())
            assertThat(result).isEmpty()
        }

        @Test
        fun `handles single version`() {
            val result = MavenArtifactFetcher.sortVersionsDescending(listOf("1.0.0"))
            assertThat(result).containsExactly("1.0.0")
        }

        @Test
        fun `sorts semantic versions correctly`() {
            val versions = listOf("1.9.0", "1.10.0", "1.2.0", "2.0.0")

            val result = MavenArtifactFetcher.sortVersionsDescending(versions)

            assertThat(result).containsExactly("2.0.0", "1.10.0", "1.9.0", "1.2.0")
        }

        @Test
        fun `sorts prerelease versions correctly`() {
            val versions = listOf("1.0.0-alpha", "1.0.0", "1.0.0-beta", "1.0.0-rc1")

            val result = MavenArtifactFetcher.sortVersionsDescending(versions)

            assertThat(result[0]).isEqualTo("1.0.0")
        }

        @Test
        fun `sorts complex version strings`() {
            val versions = listOf("1.0.0-SNAPSHOT", "1.0.0", "1.0.1", "1.0.0-beta.2", "1.0.0-beta.1")

            val result = MavenArtifactFetcher.sortVersionsDescending(versions)

            assertThat(result[0]).isEqualTo("1.0.1")
            assertThat(result[1]).isEqualTo("1.0.0")
        }
    }

    @Nested
    inner class MavenGavParseTest {

        @Test
        fun `parses standard GAV coordinate`() {
            val result = MavenArtifactFetcher.parseGav("io.detekt:detekt-api:1.23.0")
            assertThat(result).isNotNull
            assertThat(result?.groupId).isEqualTo("io.detekt")
            assertThat(result?.artifactId).isEqualTo("detekt-api")
            assertThat(result?.version).isEqualTo("1.23.0")
        }

        @Test
        fun `parses group and artifact coordinate`() {
            val result = MavenArtifactFetcher.parseGav("io.detekt:detekt-api")
            assertThat(result).isNotNull
            assertThat(result?.groupId).isEqualTo("io.detekt")
            assertThat(result?.artifactId).isEqualTo("detekt-api")
            assertThat(result?.version).isNull()
        }

        @Test
        fun `parses GAV with packaging`() {
            val result = MavenArtifactFetcher.parseGav("io.detekt:detekt-api:jar:1.23.0")
            assertThat(result).isNotNull
            assertThat(result?.groupId).isEqualTo("io.detekt")
            assertThat(result?.artifactId).isEqualTo("detekt-api")
            assertThat(result?.version).isEqualTo("1.23.0")
        }

        @Test
        fun `trims parts during parsing`() {
            val result = MavenArtifactFetcher.parseGav(" io.detekt : detekt-api : 1.23.0 ")
            assertThat(result).isNotNull
            assertThat(result?.groupId).isEqualTo("io.detekt")
            assertThat(result?.artifactId).isEqualTo("detekt-api")
            assertThat(result?.version).isEqualTo("1.23.0")
        }

        @Test
        fun `returns null for incomplete GAV`() {
            assertThat(MavenArtifactFetcher.parseGav("io.detekt")).isNull()
        }

        @Test
        fun `returns null for blank parts`() {
            assertThat(MavenArtifactFetcher.parseGav("io.detekt::1.23.0")).isNull()
            assertThat(MavenArtifactFetcher.parseGav(":detekt-api:1.23.0")).isNull()
            assertThat(MavenArtifactFetcher.parseGav("io.detekt:detekt-api:")).isNull()
        }

        @Test
        fun `returns null for non-GAV strings`() {
            assertThat(MavenArtifactFetcher.parseGav("just a search term")).isNull()
            assertThat(MavenArtifactFetcher.parseGav("")).isNull()
        }
    }

    @Nested
    inner class CreateArtifactKey {

        @Test
        fun `creates key from groupId and artifactId`() {
            val key = MavenArtifactFetcher.createArtifactKey("io.detekt", "api")
            assertThat(key).isEqualTo("io.detekt:api")
        }

        @Test
        fun `handles complex groupIds`() {
            val key = MavenArtifactFetcher.createArtifactKey("io.gitlab.arturbosch.detekt", "detekt-formatting")
            assertThat(key).isEqualTo("io.gitlab.arturbosch.detekt:detekt-formatting")
        }
    }

    @Nested
    inner class RunCallablesWithTimeoutAndShutdown {

        @Test
        fun `returns results from all callables when they complete within timeout`() {
            val callables: List<Callable<Int>> =
                listOf(
                    Callable { 1 },
                    Callable { 2 },
                    Callable { 3 },
                )
            val result: List<Int> =
                MavenArtifactFetcher.runCallablesWithTimeoutAndShutdown(
                    callables = callables,
                    timeoutMs = 5000L,
                    shutdownTimeoutMs = 1000L,
                )
            assertThat(result).containsExactly(1, 2, 3)
        }

        @Test
        fun `returns empty list for empty callables`() {
            val result: List<Int> =
                MavenArtifactFetcher.runCallablesWithTimeoutAndShutdown(
                    callables = emptyList<Callable<Int>>(),
                    timeoutMs = 1000L,
                    shutdownTimeoutMs = 1000L,
                )
            assertThat(result).isEmpty()
        }

        @Test
        fun `throws TimeoutException when a callable exceeds timeout and executor is shut down`() {
            val interrupted = AtomicBoolean(false)
            val callables: List<Callable<Int>> =
                listOf(
                    Callable {
                        try {
                            Thread.sleep(60_000)
                        } catch (e: InterruptedException) {
                            interrupted.set(true)
                            throw e
                        }
                        1
                    },
                )
            assertThatThrownBy {
                MavenArtifactFetcher.runCallablesWithTimeoutAndShutdown(
                    callables = callables,
                    timeoutMs = 50L,
                    shutdownTimeoutMs = 2000L,
                )
            }.isInstanceOf(TimeoutException::class.java)

            // shutdownNow() interrupts running tasks; give the worker a moment to see it
            Thread.sleep(100)
            assertThat(interrupted.get()).describedAs("Executor must interrupt long-running task on timeout").isTrue()
        }

        @Test
        fun `throws ExecutionException when a callable throws and executor is still shut down`() {
            val callables: List<Callable<Int>> =
                listOf(
                    Callable { 1 },
                    Callable { error("task failed") },
                )
            assertThatThrownBy {
                MavenArtifactFetcher.runCallablesWithTimeoutAndShutdown(
                    callables = callables,
                    timeoutMs = 5000L,
                    shutdownTimeoutMs = 1000L,
                )
            }.isInstanceOf(ExecutionException::class.java)
                .hasCauseInstanceOf(RuntimeException::class.java)
                .hasMessageContaining("task failed")
        }

        @Test
        fun `propagates InterruptedException and shuts down executor`() {
            val callables: List<Callable<Int>> =
                listOf(
                    Callable {
                        Thread.sleep(30_000)
                        1
                    },
                )
            val thread =
                Thread {
                    try {
                        MavenArtifactFetcher.runCallablesWithTimeoutAndShutdown(
                            callables = callables,
                            timeoutMs = 5000L,
                            shutdownTimeoutMs = 1000L,
                        )
                    } catch (_: InterruptedException) {
                        // expected when we interrupt
                    }
                }
            thread.start()
            Thread.sleep(50)
            thread.interrupt()
            thread.join(3000)
            assertThat(thread.isAlive).describedAs("Caller thread should exit after interrupt").isFalse()
        }
    }

    @Nested
    inner class RetryOnceOnTimeout {

        @Test
        fun `retries once when timeout occurs`() {
            var attempts = 0
            val result =
                MavenArtifactFetcher.retryOnceOnTimeout {
                    attempts += 1
                    if (attempts == 1) throw TimeoutException("first attempt")
                    "ok"
                }

            assertThat(result).isEqualTo("ok")
            assertThat(attempts).isEqualTo(2)
        }

        @Test
        fun `propagates timeout when both attempts fail`() {
            assertThatThrownBy {
                MavenArtifactFetcher.retryOnceOnTimeout<String> {
                    throw TimeoutException("always")
                }
            }.isInstanceOf(TimeoutException::class.java)
        }
    }

    @Nested
    inner class RunCallablesWithTimeoutAndShutdownUsingFallback {

        @Test
        fun `returns fallback for timed out callable and keeps successful results`() {
            val callables: List<Callable<String>> =
                listOf(
                    Callable { "ok" },
                    Callable {
                        Thread.sleep(60_000)
                        "late"
                    },
                )

            val result =
                MavenArtifactFetcher.runCallablesWithTimeoutAndShutdownUsingFallback(
                    callables = callables,
                    timeoutMs = 50L,
                    shutdownTimeoutMs = 1000L,
                    fallback = { index -> "fallback-$index" },
                )

            assertThat(result).containsExactly("ok", "fallback-1")
        }

        @Test
        fun `returns fallback when callable throws`() {
            val callables: List<Callable<String>> =
                listOf(
                    Callable { "ok" },
                    Callable { error("boom") },
                )

            val result =
                MavenArtifactFetcher.runCallablesWithTimeoutAndShutdownUsingFallback(
                    callables = callables,
                    timeoutMs = 5000L,
                    shutdownTimeoutMs = 1000L,
                    fallback = { index -> "fallback-$index" },
                )

            assertThat(result).containsExactly("ok", "fallback-1")
        }
    }

    @Nested
    inner class SearchArtifacts {

        @Test
        fun `returns empty list when search backend throws`() {
            val scheduler = Executors.newSingleThreadScheduledExecutor()
            try {
                val fetcher =
                    MavenArtifactFetcher(
                        project = project,
                        searchTimeout = 50.milliseconds,
                        searchBackend =
                            ArtifactSearchBackend { _, _, _ ->
                                throw IllegalStateException("backend down")
                            },
                        versionsFetcher = VersionsFetcher { error("should not be called") },
                        scheduler = scheduler,
                    )

                val result = CompletableFuture<List<MavenArtifact>>()
                fetcher.searchArtifacts("detekt") { result.complete(it) }

                assertThat(result.get(1, TimeUnit.SECONDS)).isEmpty()
            } finally {
                scheduler.shutdownNow()
            }
        }

        @Test
        fun `keeps versions for successful artifacts when one times out`() {
            val scheduler = Executors.newSingleThreadScheduledExecutor()
            try {
                val artifactA = createArtifact("io.example", "a", "1.0.0")
                val artifactB = createArtifact("io.example", "b", "2.0.0")
                val fetcher =
                    MavenArtifactFetcher(
                        project = project,
                        searchTimeout = 50.milliseconds,
                        searchBackend = ArtifactSearchBackend { _, _, onResults ->
                            onResults(listOf(artifactA, artifactB))
                        },
                        versionsFetcher = VersionsFetcher { artifacts ->
                            artifacts.map { artifact ->
                                if (artifact.artifactId == "a") {
                                    try {
                                        throw TimeoutException("single timeout")
                                    } catch (_: TimeoutException) {
                                        MavenArtifact(artifact.groupId, artifact.artifactId, listOf(artifact.version))
                                    }
                                } else {
                                    MavenArtifact(artifact.groupId, artifact.artifactId, listOf("2.0.0", "1.0.0"))
                                }
                            }
                        },
                        scheduler = scheduler,
                    )

                val result = CompletableFuture<List<MavenArtifact>>()
                fetcher.searchArtifacts("example") { result.complete(it) }

                val artifacts = result.get(1, TimeUnit.SECONDS)
                val artifactAVersions = artifacts.first { it.artifactId == "a" }.versions
                val artifactBVersions = artifacts.first { it.artifactId == "b" }.versions

                assertThat(artifactAVersions).containsExactly("1.0.0")
                assertThat(artifactBVersions).containsExactly("2.0.0", "1.0.0")
            } finally {
                scheduler.shutdownNow()
            }
        }

        @Test
        fun `returns empty list when search backend never responds`() {
            val scheduler = Executors.newSingleThreadScheduledExecutor()
            try {
                val fetcher =
                    MavenArtifactFetcher(
                        project = project,
                        searchTimeout = 50.milliseconds,
                        searchBackend = ArtifactSearchBackend { _, _, _ -> },
                        versionsFetcher = VersionsFetcher { error("should not be called") },
                        scheduler = scheduler,
                    )

                val result = CompletableFuture<List<MavenArtifact>>()
                fetcher.searchArtifacts("detekt") { result.complete(it) }

                assertThat(result.get(1, TimeUnit.SECONDS)).isEmpty()
            } finally {
                scheduler.shutdownNow()
            }
        }

        @Test
        fun `falls back to latest version when version fetch fails`() {
            val scheduler = Executors.newSingleThreadScheduledExecutor()
            try {
                val artifact = createArtifact("io.detekt", "detekt-formatting", "1.0.0")
                val fetcher =
                    MavenArtifactFetcher(
                        project = project,
                        searchTimeout = 50.milliseconds,
                        searchBackend = ArtifactSearchBackend { _, _, onResults -> onResults(listOf(artifact)) },
                        versionsFetcher = VersionsFetcher { throw TimeoutException("backend timeout") },
                        scheduler = scheduler,
                    )

                val result = CompletableFuture<List<MavenArtifact>>()
                fetcher.searchArtifacts("detekt-formatting") { result.complete(it) }

                val artifacts = result.get(1, TimeUnit.SECONDS)
                assertThat(artifacts).hasSize(1)
                assertThat(artifacts[0].versions).containsExactly("1.0.0")
            } finally {
                scheduler.shutdownNow()
            }
        }
    }

    private fun createArtifact(groupId: String, artifactId: String, version: String): RepositoryArtifactDescription =
        RepositoryArtifactDescription(groupId, artifactId, version, "jar", null)
}
