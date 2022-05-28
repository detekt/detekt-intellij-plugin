package io.gitlab.arturbosch.detekt.idea.action

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ToggleAction
import com.intellij.openapi.project.DumbAware
import io.gitlab.arturbosch.detekt.idea.config.DetektConfigStorage

class EnableAction : ToggleAction(), DumbAware {

    override fun isSelected(e: AnActionEvent): Boolean {
        val project = e.project ?: return false
        return DetektConfigStorage.instance(project).enableDetekt
    }

    override fun setSelected(e: AnActionEvent, state: Boolean) {
        val project = e.project ?: return
        DetektConfigStorage.instance(project).enableDetekt = state
    }
}
