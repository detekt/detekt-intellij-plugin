package io.gitlab.arturbosch.detekt.idea.util

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.options.newEditor.SettingsDialog
import com.intellij.openapi.project.Project
import io.gitlab.arturbosch.detekt.idea.DETEKT
import io.gitlab.arturbosch.detekt.idea.config.DetektConfig
import io.gitlab.arturbosch.detekt.idea.config.DetektPluginSettings
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.absolute

fun Project.isDetektEnabled(): Boolean =
    service<DetektPluginSettings>().enableDetekt

fun absolutePath(project: Project, path: String): String {
    if (path.isBlank() || File(path).isAbsolute) return path

    return project.basePath?.let { Path.of(it, path).absolute().toString() }
        ?: path
}

fun extractPaths(path: String, project: Project): List<Path> =
    path.trim()
        .split(File.pathSeparator)
        .filter { it.isNotEmpty() }
        .map { absolutePath(project, it) }
        .map { Paths.get(it) }

fun showNotification(problems: List<String>, project: Project) {
    showNotification(
        "detekt plugin noticed some problems",
        problems.joinToString(System.lineSeparator()) + "Skipping detekt run.",
        project
    )
}

fun showNotification(title: String, content: String, project: Project) {
    val notification = Notification(
        DETEKT,
        title,
        content,
        NotificationType.WARNING
    )

    @Suppress("DialogTitleCapitalization")
    notification.addAction(object : AnAction("Open Detekt projects settings") {
        override fun actionPerformed(e: AnActionEvent) {
            val dialog = SettingsDialog(
                project,
                "Detekt project settings",
                DetektConfig(project),
                true,
                true
            )
            ApplicationManager.getApplication().invokeLater(dialog::show)
        }
    })
    notification.notify(project)
}
