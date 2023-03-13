package io.gitlab.arturbosch.detekt.idea.webviews

import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory

class DocumentationToolWindow : ToolWindowFactory, DumbAware {

    companion object {
        const val ID = "detekt doc"
    }

    override fun init(toolWindow: ToolWindow) {
        RulesBrowserService.getInstance().loadRulesPage()
    }

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val browser = RulesBrowserService.getInstance()
        val content = toolWindow.contentManager.factory
            .createContent(browser.getComponent(), null, false)
        content.setDisposer(browser)
        toolWindow.contentManager.addContent(content)
    }
}
