package io.gitlab.arturbosch.detekt.idea.util

/**
 * Represents a Detekt plugin and its configuration.
 *
 * @property coordinate The Maven coordinate (groupId:artifactId:version) of the plugin.
 * @property exclusions A set of coordinates or groupId:artifactId strings to exclude from the dependency tree.
 */
data class DetektPlugin(var coordinate: String = "", var exclusions: MutableSet<String> = mutableSetOf())
