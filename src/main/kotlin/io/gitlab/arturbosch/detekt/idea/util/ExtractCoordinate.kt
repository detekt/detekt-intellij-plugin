package io.gitlab.arturbosch.detekt.idea.util

import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.idea.maven.utils.library.RepositoryLibraryProperties

private const val MIN_PATH_PARTS = 4

/**
 * Extracts Maven coordinates from a [com.intellij.openapi.vfs.VirtualFile].
 *
 * It attempts to extract coordinates from the file path if it's located within a Maven repository structure
 * (e.g., `.../repository/group/id/artifactId/version/artifact-version.jar`).
 *
 * @param file The [VirtualFile] representing the artifact.
 * @param mainProps Default properties to use if `isMain` is true.
 * @param isMain If true, the coordinates are directly taken from [mainProps].
 * @return Maven coordinates in the format `groupId:artifactId:version`, or a best-effort "unknown" string if extraction
 *   fails.
 */
@Suppress("ReturnCount") // No idea how to trim it further
internal fun extractCoordinate(file: VirtualFile, mainProps: RepositoryLibraryProperties, isMain: Boolean): String {
    if (isMain) return "${mainProps.groupId}:${mainProps.artifactId}:${mainProps.version}"

    val path = file.path
    val repoMarker = "/repository/"
    val index = path.lastIndexOf(repoMarker)
    if (index != -1) {
        val relativePath = path.substring(index + repoMarker.length)
        val parts = relativePath.split('/')
        if (parts.size >= MIN_PATH_PARTS) {
            // .../group/part/artifactId/version/artifact-version.jar
            val versionIndex = parts.size - 2
            val version = parts[versionIndex]
            val artifactId = parts[versionIndex - 1]
            val groupId = parts.subList(0, versionIndex - 1).joinToString(".")
            return "$groupId:$artifactId:$version"
        }
    }
    return "unknown:${file.nameWithoutExtension}:unknown"
}
