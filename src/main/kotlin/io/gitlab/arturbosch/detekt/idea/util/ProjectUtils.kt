package io.gitlab.arturbosch.detekt.idea.util

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.options.newEditor.SettingsDialog
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.layout.ValidationInfoBuilder
import io.gitlab.arturbosch.detekt.idea.DETEKT
import io.gitlab.arturbosch.detekt.idea.DetektBundle
import io.gitlab.arturbosch.detekt.idea.config.DetektConfig
import io.gitlab.arturbosch.detekt.idea.config.DetektPluginSettings
import java.io.File
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.absolute

fun Project.isDetektEnabled(): Boolean =
    service<DetektPluginSettings>().enableDetekt

fun absolutePath(project: Project, path: String): String {
    if (path.isBlank() || File(path).isAbsolute) return path

    return project.basePath?.let { Path(it, path).absolute().toString() }
        ?: path
}

fun absoluteBaselinePath(project: Project, settings: DetektPluginSettings): Path? =
    settings.baselinePath.trim()
        .takeIf { it.isNotEmpty() }
        ?.let { Path(absolutePath(project, settings.baselinePath)) }

fun List<String>.toVirtualFilesList(): List<VirtualFile> {
    val fs = LocalFileSystem.getInstance()
    return filter { it.isNotBlank() }
        .mapNotNull { fs.findFileByPath(it) }
}

fun List<VirtualFile>.toPathsList(): List<String> =
    filter { it.exists() }
        .map { it.path }

fun showNotification(problems: List<String>, project: Project) {
    showNotification(
        title = DetektBundle.message("detekt.notifications.message.problemsFound"),
        content = problems.joinToString(System.lineSeparator()) +
                DetektBundle.message("detekt.notifications.content.skippingRun"),
        project = project
    )
}

fun showNotification(title: String, content: String, project: Project) {
    val notification = Notification(
        DETEKT,
        title,
        content,
        NotificationType.WARNING
    )

    notification.addAction(object : AnAction(DetektBundle.message("detekt.notifications.actions.openSettings")) {
        override fun actionPerformed(e: AnActionEvent) {
            val dialog = SettingsDialog(
                project,
                "detekt-settings",
                DetektConfig(project),
                true,
                true
            )
            ApplicationManager.getApplication().invokeLater(dialog::show)
        }
    })
    notification.notify(project)
}

internal fun ValidationInfoBuilder.validateAsFilePath(text: String, isWarning: Boolean = false) =
    if (text.isNotEmpty() && !File(text).isFile) {
        if (isWarning) {
            warning(DetektBundle.message("detekt.configuration.validationError.filePath"))
        } else {
            error(DetektBundle.message("detekt.configuration.validationError.filePath"))
        }
    } else {
        null
    }
