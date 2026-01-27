package io.gitlab.arturbosch.detekt.idea

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManagerListener
import io.gitlab.arturbosch.detekt.idea.config.DetektPluginSettings
import io.gitlab.arturbosch.detekt.idea.config.DetektPluginSettings.EnableForProjectPromptResult

class DetektProjectManagerListener : ProjectManagerListener {

    @Deprecated("")
    override fun projectOpened(project: Project) {
        val settings = project.service<DetektPluginSettings>()

        // Only show the prompt if detekt isn't already enabled, and the user hasn't
        // opted out explicitly.
        if (settings.enableDetekt || !settings.shouldShowPromptToEnable) return

        val notification = Notification(
            NOTIFICATION_GROUP_ID,
            DetektBundle.message("detekt.notification.enableDetekt.title"),
            DetektBundle.message("detekt.notification.enableDetekt.description"),
            NotificationType.INFORMATION
        )

        notification.addAction(
            object : DumbAwareAction(DetektBundle.message("detekt.notification.enableDetekt.enable")) {
                override fun actionPerformed(e: AnActionEvent) {
                    settings.enableForProjectResult = EnableForProjectPromptResult.Accepted
                    settings.enableDetekt = true
                    notification.expire()
                }
            }
        )

        notification.addAction(
            object : DumbAwareAction(DetektBundle.message("detekt.notification.enableDetekt.optOut")) {
                override fun actionPerformed(e: AnActionEvent) {
                    settings.enableForProjectResult = EnableForProjectPromptResult.Declined
                    notification.expire()
                }
            }
        )

        notification.notify(project)
    }
}
