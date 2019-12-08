package io.gitlab.arturbosch.detekt.config

import com.intellij.openapi.project.Project
import io.gitlab.arturbosch.detekt.util.absolutePath
import io.gitlab.arturbosch.detekt.util.showNotification
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

fun DetektConfigStorage.plugins(project: Project): List<Path>? {
    val pluginPaths = pluginPaths
        .split(File.pathSeparator)
        .map { absolutePath(project, it) }
        .map { Paths.get(it) }

    if (pluginPaths.isNotEmpty()) {
        if (pluginPaths.any { Files.notExists(it) }) {
            showNotification(
                "Plugin jar file not found",
                "One or more provided detekt plugin jars do not exist. Skipping detekt run.",
                project
            )

            return null
        }
    }

    return pluginPaths
}
