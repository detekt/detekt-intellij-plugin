package io.gitlab.arturbosch.detekt.idea.webviews

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import io.gitlab.arturbosch.detekt.idea.util.PluginUtils

class ShowRulesAction : AnAction(PluginUtils.pluginIcon()) {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        showPage(DocsPage.Rules, project)
    }
}
