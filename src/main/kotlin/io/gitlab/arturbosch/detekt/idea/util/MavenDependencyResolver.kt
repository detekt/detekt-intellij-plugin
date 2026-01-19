package io.gitlab.arturbosch.detekt.idea.util

import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import org.jetbrains.idea.maven.aether.ArtifactDependencyNode
import java.io.File

private const val GAV_COORDINATES_PARTS_COUNT = 3

class MavenDependencyResolver(
    private val project: Project,
    private val treeResolver: MavenTreeResolver = MavenTreeResolver.Default,
) {
    private val logger = thisLogger()

    /**
     * Represents a resolved dependency node in the Maven dependency tree.
     *
     * @property groupId The group ID of the dependency.
     * @property artifactId The artifact ID of the dependency.
     * @property version The version of the dependency.
     * @property coordinate The full GAV coordinate string (groupId:artifactId:version).
     * @property isProvided Whether this dependency is force-excluded by the platform (e.g., standard libraries).
     * @property isRejected Whether this dependency was rejected by the resolver (e.g., due to conflicts).
     * @property children The list of transitive dependencies.
     * @property file The resolved artifact file on disk, if available.
     */
    data class ResolvedNode(
        val groupId: String,
        val artifactId: String,
        val version: String,
        val coordinate: String,
        val isProvided: Boolean,
        val isRejected: Boolean,
        val children: List<ResolvedNode>,
        val file: File? = null,
    )

    /**
     * Resolves the dependency tree for the given [coordinate].
     *
     * This method fetches the full transitive dependency tree and maps it to a tree of [ResolvedNode]s, identifying
     * force-excluded and rejected dependencies along the way.
     *
     * @param coordinate The GAV coordinate to resolve (groupId:artifactId:version).
     * @return The root [ResolvedNode] of the resolved tree, or null if resolution fails.
     */
    @Suppress("ReturnCount") // No idea how to reduce them without making the code worse
    fun resolve(
        coordinate: String,
        shouldDownload: Boolean = false,
        exclusions: List<String> = emptyList(),
        indicator: ProgressIndicator? = null,
    ): ResolvedNode? {
        logger.debug("Resolving coordinate: $coordinate (download: $shouldDownload)")
        val parts = coordinate.split(":")
        if (parts.size < GAV_COORDINATES_PARTS_COUNT) {
            logger.warn("Invalid coordinate format: $coordinate")
            return null
        }

        val groupId = parts[0]
        val artifactId = parts[1]
        val version = parts[2]

        if (groupId.isBlank() || artifactId.isBlank() || version.isBlank()) {
            logger.warn("Invalid coordinate: groupId, artifactId and version must be non-empty: $coordinate")
            return null
        }

        val (rootNode, resolvedFiles) =
            treeResolver.resolveDependenciesTree(
                groupId,
                artifactId,
                version,
                project,
                shouldDownload,
                exclusions,
                indicator,
            )
        if (rootNode == null) {
            logger.warn("Could not resolve tree for $coordinate")
            return null
        }

        val resolved = mapToResolvedNode(rootNode, resolvedFiles)
        logger.info("Resolved $coordinate with ${flatten(resolved).size} total nodes")
        return resolved
    }

    /**
     * Maps an [ArtifactDependencyNode] and its resolved files to a [ResolvedNode] tree.
     *
     * Includes cycle detection to prevent [StackOverflowError] in case of cyclic dependencies.
     */
    private fun mapToResolvedNode(
        node: ArtifactDependencyNode,
        resolvedFiles: Map<String, File>,
        visited: Set<String> = emptySet(),
    ): ResolvedNode {
        val artifact = node.artifact
        val coord = "${artifact.groupId}:${artifact.artifactId}:${artifact.version}"

        if (visited.contains(coord)) {
            logger.warn("Cycle detected during tree mapping at $coord. Skipping children.")
            return createLeafNode(
                artifact.groupId,
                artifact.artifactId,
                artifact.version,
                coord,
                artifact.file ?: resolvedFiles[coord],
                isRejected = true,
            )
        }

        val file = artifact.file ?: resolvedFiles[coord]
        val isProvided = ProvidedDependencies.isProvided(artifact.groupId, artifact.artifactId)

        logger.debug("Mapping node: $coord (file: ${file?.name}, provided: $isProvided)")

        val newVisited = visited + coord
        return ResolvedNode(
            groupId = artifact.groupId,
            artifactId = artifact.artifactId,
            version = artifact.version,
            coordinate = coord,
            isProvided = isProvided,
            isRejected = node.isRejected,
            children = node.dependencies.map { mapToResolvedNode(it, resolvedFiles, newVisited) },
            file = file,
        )
    }

    @Suppress("LongParameterList")
    private fun createLeafNode(
        groupId: String,
        artifactId: String,
        version: String,
        coord: String,
        file: File?,
        isRejected: Boolean,
    ): ResolvedNode {
        val isProvided = ProvidedDependencies.isProvided(groupId, artifactId)
        return ResolvedNode(
            groupId = groupId,
            artifactId = artifactId,
            version = version,
            coordinate = coord,
            isProvided = isProvided,
            isRejected = isRejected,
            children = emptyList(),
            file = file,
        )
    }

    /**
     * Flattens the [ResolvedNode] tree into a list of all nodes.
     *
     * Includes cycle detection to prevent [StackOverflowError] in case of cyclic dependencies.
     */
    internal fun flatten(node: ResolvedNode, visited: Set<String> = emptySet()): List<ResolvedNode> {
        if (visited.contains(node.coordinate)) return emptyList()
        val newVisited = visited + node.coordinate
        return listOf(node) + node.children.flatMap { flatten(it, newVisited) }
    }
}
