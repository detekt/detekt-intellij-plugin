package io.gitlab.arturbosch.detekt.util

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.options.newEditor.SettingsDialog
import com.intellij.openapi.project.Project
import io.gitlab.arturbosch.detekt.config.DetektConfig
import io.gitlab.arturbosch.detekt.config.DetektConfigStorage
import io.gitlab.arturbosch.detekt.core.DetektFacade
import io.gitlab.arturbosch.detekt.core.FileProcessorLocator
import io.gitlab.arturbosch.detekt.core.ProcessingSettings
import io.gitlab.arturbosch.detekt.core.RuleSetLocator
import java.io.File

fun absolutePath(project: Project, path: String): String =
    if (path.isBlank() || File(path).isAbsolute) {
        path
    } else {
        project.basePath + "/" + path
    }

fun ensureFileExists(path: String, project: Project, title: String, content: String): Boolean {
    if (!File(path).exists()) {
        val n = Notification(
            "Detekt",
            title,
            content,
            NotificationType.WARNING
        )
        n.addAction(object : AnAction("Open Detekt projects settings") {
            override fun actionPerformed(e: AnActionEvent) {
                val dialog = SettingsDialog(project, "Detekt project settings", DetektConfig(project), true, true)
                ApplicationManager.getApplication().invokeLater(dialog::show)
            }
        })
        n.notify(project)
        return false
    }
    return true
}

fun createFacade(settings: ProcessingSettings, configuration: DetektConfigStorage): DetektFacade {
    var providers = RuleSetLocator(settings).load()
    if (!configuration.enableFormatting) {
        providers = providers.filterNot { it.ruleSetId == "formatting" }
    }
    val processors = FileProcessorLocator(settings).load()
    return DetektFacade.create(settings, providers, processors)
}