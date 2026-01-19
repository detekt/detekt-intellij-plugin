package io.gitlab.arturbosch.detekt.idea.util

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import io.gitlab.arturbosch.detekt.idea.MockProjectTestCase
import org.eclipse.aether.artifact.Artifact
import org.eclipse.aether.artifact.DefaultArtifact
import org.jetbrains.idea.maven.aether.ArtifactDependencyNode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class MavenDependencyResolverTest : MockProjectTestCase() {

    @Test
    fun `resolve creates node with parsing logic`() {
        val mockArtifact =
            object : Artifact by DefaultArtifact("group:artifact:1.0.0") {
                override fun getFile(): File = File("artifact-1.0.0.jar")
            }
        val mockNode = ArtifactDependencyNode(mockArtifact, emptyList<ArtifactDependencyNode>(), false)

        val resolver = MavenDependencyResolver(
            project,
            object : MavenTreeResolver {
                override fun resolveDependenciesTree(
                    groupId: String,
                    artifactId: String,
                    version: String,
                    project: Project,
                    shouldDownload: Boolean,
                    exclusions: List<String>,
                    indicator: ProgressIndicator?,
                ): Pair<ArtifactDependencyNode?, Map<String, File>> = mockNode to emptyMap()
            },
        )

        val node = resolver.resolve("group:artifact:1.0.0")

        assertNotNull(node)
        assertEquals("group", node?.groupId)
        assertEquals("artifact", node?.artifactId)
        assertEquals("1.0.0", node?.version)
        assertEquals("group:artifact:1.0.0", node?.coordinate)
        assertEquals(false, node?.isProvided)
        assertEquals(false, node?.isRejected)
        assertEquals("artifact-1.0.0.jar", node?.file?.name)
    }

    @Test
    fun `resolve handles transitive dependencies`() {
        val childArtifact =
            object : Artifact by DefaultArtifact("group:child:1.0.0") {
                override fun getFile(): File = File("child-1.0.0.jar")
            }
        val childNode = ArtifactDependencyNode(childArtifact, emptyList<ArtifactDependencyNode>(), false)

        val parentArtifact =
            object : Artifact by DefaultArtifact("group:parent:1.0.0") {
                override fun getFile(): File = File("parent-1.0.0.jar")
            }
        val parentNode = ArtifactDependencyNode(parentArtifact, listOf(childNode), false)

        val resolver = MavenDependencyResolver(
            project,
            object : MavenTreeResolver {
                override fun resolveDependenciesTree(
                    groupId: String,
                    artifactId: String,
                    version: String,
                    project: Project,
                    shouldDownload: Boolean,
                    exclusions: List<String>,
                    indicator: ProgressIndicator?,
                ): Pair<ArtifactDependencyNode?, Map<String, File>> = parentNode to emptyMap()
            },
        )

        val node = resolver.resolve("group:parent:1.0.0")

        assertNotNull(node)
        assertEquals("group:parent:1.0.0", node?.coordinate)
        assertEquals(1, node?.children?.size)
        assertEquals("group:child:1.0.0", node?.children?.get(0)?.coordinate)
    }

    @Test
    fun `resolve handles excluded dependencies`() {
        val excludedArtifact =
            object : Artifact by DefaultArtifact("org.jetbrains.kotlin:kotlin-stdlib:1.9.0") {
                override fun getFile(): File = File("irrelevant.jar")
            }
        val excludedNode = ArtifactDependencyNode(excludedArtifact, emptyList<ArtifactDependencyNode>(), false)

        val resolver = MavenDependencyResolver(
            project,
            object : MavenTreeResolver {
                override fun resolveDependenciesTree(
                    groupId: String,
                    artifactId: String,
                    version: String,
                    project: Project,
                    shouldDownload: Boolean,
                    exclusions: List<String>,
                    indicator: ProgressIndicator?,
                ): Pair<ArtifactDependencyNode?, Map<String, File>> = excludedNode to emptyMap()
            },
        )

        val node = resolver.resolve("org.jetbrains.kotlin:kotlin-stdlib:1.9.0")

        assertNotNull(node)
        assertEquals(true, node?.isProvided)
    }

    @Test
    fun `resolve handles rejected dependencies`() {
        val rejectedArtifact =
            object : Artifact by DefaultArtifact("group:rejected:1.0.0") {
                override fun getFile(): File = File("rejected-1.0.0.jar")
            }
        val rejectedNode = ArtifactDependencyNode(rejectedArtifact, emptyList<ArtifactDependencyNode>(), true)

        val resolver = MavenDependencyResolver(
            project,
            object : MavenTreeResolver {
                override fun resolveDependenciesTree(
                    groupId: String,
                    artifactId: String,
                    version: String,
                    project: Project,
                    shouldDownload: Boolean,
                    exclusions: List<String>,
                    indicator: ProgressIndicator?,
                ): Pair<ArtifactDependencyNode?, Map<String, File>> = rejectedNode to emptyMap()
            },
        )

        val node = resolver.resolve("group:rejected:1.0.0")

        assertNotNull(node)
        assertEquals(true, node?.isRejected)
    }

    @Test
    fun `resolve handles detekt force exclusions`() {
        val detektArtifact =
            object : Artifact by DefaultArtifact("io.gitlab.arturbosch.detekt:detekt-api:1.23.0") {
                override fun getFile(): File = File("irrelevant.jar")
            }
        val detektNode = ArtifactDependencyNode(detektArtifact, emptyList(), false)

        val resolver = MavenDependencyResolver(
            project,
            object : MavenTreeResolver {
                override fun resolveDependenciesTree(
                    groupId: String,
                    artifactId: String,
                    version: String,
                    project: Project,
                    shouldDownload: Boolean,
                    exclusions: List<String>,
                    indicator: ProgressIndicator?,
                ): Pair<ArtifactDependencyNode?, Map<String, File>> = detektNode to emptyMap()
            },
        )

        val node = resolver.resolve("io.gitlab.arturbosch.detekt:detekt-api:1.23.0")

        assertNotNull(node)
        assertEquals(true, node?.isProvided)
    }

    @Test
    fun `resolve returns null for invalid coordinate`() {
        val resolver = MavenDependencyResolver(project)
        val node = resolver.resolve("invalid-coordinate")
        assertNull(node)
    }

    @Test
    fun `resolve returns null when groupId is blank`() {
        val resolver = MavenDependencyResolver(project)
        assertNull(resolver.resolve(":artifact:1.0.0"))
    }

    @Test
    fun `resolve returns null when artifactId is blank`() {
        val resolver = MavenDependencyResolver(project)
        assertNull(resolver.resolve("group::1.0.0"))
    }

    @Test
    fun `resolve returns null when version is blank`() {
        val resolver = MavenDependencyResolver(project)
        assertNull(resolver.resolve("group:artifact:"))
    }

    @Test
    fun `resolve returns null when groupId and artifactId are blank`() {
        val resolver = MavenDependencyResolver(project)
        assertNull(resolver.resolve("::1.0.0"))
    }

    @Test
    fun `resolve returns null when any GAV part is whitespace only`() {
        val resolver = MavenDependencyResolver(project)
        assertNull(resolver.resolve("   :artifact:1.0.0"))
        assertNull(resolver.resolve("group:   :1.0.0"))
        assertNull(resolver.resolve("group:artifact:   "))
    }

    @Test
    fun `resolve returns null when tree resolver returns null`() {
        val resolver = MavenDependencyResolver(
            project,
            object : MavenTreeResolver {
                override fun resolveDependenciesTree(
                    groupId: String,
                    artifactId: String,
                    version: String,
                    project: Project,
                    shouldDownload: Boolean,
                    exclusions: List<String>,
                    indicator: ProgressIndicator?,
                ): Pair<ArtifactDependencyNode?, Map<String, File>> = null to emptyMap()
            },
        )

        val node = resolver.resolve("group:artifact:1.0.0")
        assertEquals(null, node)
    }

    @Test
    fun `resolve uses artifact file when resolved files map is empty`() {
        val mockArtifact =
            object : Artifact by DefaultArtifact("group:artifact:1.0.0") {
                override fun getFile(): File = File("artifact-1.0.0.jar")
            }
        val mockNode = ArtifactDependencyNode(mockArtifact, emptyList<ArtifactDependencyNode>(), false)

        val resolver = MavenDependencyResolver(
            project,
            object : MavenTreeResolver {
                override fun resolveDependenciesTree(
                    groupId: String,
                    artifactId: String,
                    version: String,
                    project: Project,
                    shouldDownload: Boolean,
                    exclusions: List<String>,
                    indicator: ProgressIndicator?,
                ): Pair<ArtifactDependencyNode?, Map<String, File>> = mockNode to emptyMap()
            },
        )

        val node = resolver.resolve("group:artifact:1.0.0")

        assertNotNull(node)
        // Should use the artifact's file directly when not in resolved files map
        assertEquals("artifact-1.0.0.jar", node?.file?.name)
    }

    @Test
    fun `resolve prefers resolved files map over artifact file`() {
        val mockArtifact =
            object : Artifact by DefaultArtifact("group:artifact:1.0.0") {
                override fun getFile(): File? = null // No file on artifact
            }
        val mockNode = ArtifactDependencyNode(mockArtifact, emptyList<ArtifactDependencyNode>(), false)
        val resolvedFiles = mapOf("group:artifact:1.0.0" to File("resolved-artifact.jar"))

        val resolver = MavenDependencyResolver(
            project,
            object : MavenTreeResolver {
                override fun resolveDependenciesTree(
                    groupId: String,
                    artifactId: String,
                    version: String,
                    project: Project,
                    shouldDownload: Boolean,
                    exclusions: List<String>,
                    indicator: ProgressIndicator?,
                ): Pair<ArtifactDependencyNode?, Map<String, File>> = mockNode to resolvedFiles
            },
        )

        val node = resolver.resolve("group:artifact:1.0.0")

        assertNotNull(node)
        assertEquals("resolved-artifact.jar", node?.file?.name)
    }

    @Test
    fun `flattening and uniqueness logic works correctly`() {
        val sharedDep =
            MavenDependencyResolver.ResolvedNode(
                "group",
                "shared",
                "1.0.0",
                "group:shared:1.0.0",
                isProvided = false,
                isRejected = false,
                children = emptyList(),
            )
        val child1 =
            MavenDependencyResolver.ResolvedNode(
                "group",
                "child1",
                "1.0.0",
                "group:child1:1.0.0",
                isProvided = false,
                isRejected = false,
                children = listOf(sharedDep),
            )
        val child2 =
            MavenDependencyResolver.ResolvedNode(
                "group",
                "child2",
                "1.0.0",
                "group:child2:1.0.0",
                isProvided = false,
                isRejected = false,
                children = listOf(sharedDep),
            )
        val root =
            MavenDependencyResolver.ResolvedNode(
                "group",
                "root",
                "1.0.0",
                "group:root:1.0.0",
                isProvided = false,
                isRejected = false,
                children = listOf(child1, child2),
            )

        val resolver = MavenDependencyResolver(project)
        val flattened = resolver.flatten(root)

        // Total nodes in tree (root + child1 + shared + child2 + shared) = 5
        assertEquals(5, flattened.size)

        // Distinct coordinates (root, child1, child2, shared) = 4
        val distinctCount = flattened.distinctBy { it.coordinate }.size
        assertEquals(4, distinctCount)

        // Transitive dependencies (excluding root)
        val transitiveDistinctCount =
            flattened.filter { it.coordinate != root.coordinate }.distinctBy { it.coordinate }.size
        assertEquals(3, transitiveDistinctCount)
    }

    @Test
    fun `resolve passes indicator to tree resolver`() {
        var passedIndicator: ProgressIndicator? = null
        val mockIndicator =
            java.lang.reflect.Proxy.newProxyInstance(
                ProgressIndicator::class.java.classLoader,
                arrayOf(ProgressIndicator::class.java),
            ) { _, _, _ ->
                null
            } as ProgressIndicator

        val resolver = MavenDependencyResolver(
            project,
            object : MavenTreeResolver {
                override fun resolveDependenciesTree(
                    groupId: String,
                    artifactId: String,
                    version: String,
                    project: Project,
                    shouldDownload: Boolean,
                    exclusions: List<String>,
                    indicator: ProgressIndicator?,
                ): Pair<ArtifactDependencyNode?, Map<String, File>> {
                    passedIndicator = indicator
                    return null to emptyMap()
                }
            },
        )

        resolver.resolve("group:artifact:1.0.0", indicator = mockIndicator)
        assertTrue(mockIndicator === passedIndicator)
    }
}
