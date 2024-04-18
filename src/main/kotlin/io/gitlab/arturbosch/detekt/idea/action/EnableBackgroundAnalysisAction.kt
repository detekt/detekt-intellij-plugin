package io.gitlab.arturbosch.detekt.idea.action

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ToggleAction
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAware
import io.gitlab.arturbosch.detekt.idea.config.DetektPluginSettings
import io.gitlab.arturbosch.detekt.idea.util.PluginUtils

class EnableBackgroundAnalysisAction : ToggleAction(null, null, PluginUtils.pluginIcon()), DumbAware {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

    override fun isSelected(e: AnActionEvent): Boolean {
        val project = e.project ?: return false
        return project.service<DetektPluginSettings>().enableDetekt
    }

    override fun setSelected(e: AnActionEvent, state: Boolean) {
        val project = e.project ?: return
        project.service<DetektPluginSettings>().enableDetekt = state
    }
}
