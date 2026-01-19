package io.gitlab.arturbosch.detekt.idea.util

/**
 * Utility for determining which Maven dependencies are provided by the environment.
 *
 * These are typically dependencies provided by the detekt-intellij-plugin itself or the underlying IntelliJ platform
 * (detekt-api, kotlin-stdlib, etc.).
 */
object ProvidedDependencies {

    private val PROVIDED_GROUP_IDS = setOf("io.gitlab.arturbosch.detekt", "dev.detekt", "org.jetbrains.kotlin")

    /** Returns true if the given dependency is provided by the environment. */
    fun isProvided(groupId: String, artifactId: String): Boolean {
        if (groupId == "org.jetbrains.kotlinx") {
            return artifactId.startsWith("kotlinx-coroutines-")
        }
        return groupId in PROVIDED_GROUP_IDS
    }

    /** Returns a list of Maven exclusion strings for all provided dependencies. */
    fun getExclusions(): List<String> =
        PROVIDED_GROUP_IDS.map { "$it:*" } + "org.jetbrains.kotlinx:kotlinx-coroutines-*"
}
