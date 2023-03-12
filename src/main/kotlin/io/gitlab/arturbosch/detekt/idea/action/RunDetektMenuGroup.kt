package io.gitlab.arturbosch.detekt.idea.action

import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.fileEditor.FileEditorManager
import io.gitlab.arturbosch.detekt.idea.util.PluginUtils

class RunDetektMenuGroup : DefaultActionGroup(null, null, PluginUtils.pluginIcon()) {

    override fun update(e: AnActionEvent) {
        val project = e.project
        val presentation = e.presentation
        if (project == null) {
            // If no project defined, disable the menu item
            presentation.isEnabled = false
            presentation.isVisible = false
            return
        } else {
            presentation.isVisible = true
        }
        if (e.place == ActionPlaces.MAIN_MENU) {
            // Enabled only if some files are selected.
            val selectedFiles = FileEditorManager.getInstance(project).selectedFiles
            presentation.isEnabled = selectedFiles.isNotEmpty()
        }
    }
}
