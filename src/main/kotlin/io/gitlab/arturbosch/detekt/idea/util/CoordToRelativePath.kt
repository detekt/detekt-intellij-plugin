package io.gitlab.arturbosch.detekt.idea.util

import org.jetbrains.annotations.VisibleForTesting

internal const val MIN_COORDINATES_PARTS = 3

/**
 * Converts Maven coordinates and a filename into a relative path structure.
 *
 * For example, given coordinates `io.gitlab.arturbosch.detekt:detekt-cli:1.23.0` and filename `detekt-cli-1.23.0.jar`,
 * it will return `io/gitlab/arturbosch/detekt/detekt-cli/1.23.0/detekt-cli-1.23.0.jar`.
 *
 * @param coord Maven coordinates in the format `groupId:artifactId:version`.
 * @param fileName The name of the file to append to the path.
 * @return A relative path string, or "unknown/$fileName" if the coordinates are invalid.
 */
@VisibleForTesting
internal fun coordToRelativePath(coord: String, fileName: String): String {
    val parts = coord.split(":")
    if (parts.size >= MIN_COORDINATES_PARTS) {
        // group/artifact/version/fileName
        val groupPath = parts[0].replace('.', '/')
        return "$groupPath/${parts[1]}/${parts[2]}/$fileName"
    }
    return "unknown/$fileName"
}
