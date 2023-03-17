package io.gitlab.arturbosch.detekt.idea.webviews

import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBPanelWithEmptyText
import com.intellij.ui.jcef.JBCefApp

class DocumentationToolWindow : ToolWindowFactory, DumbAware {

    companion object {
        const val ID = "detekt doc"
    }

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val content = if (JBCefApp.isSupported()) {
            val browser = RulesBrowserService.getInstance()
            toolWindow.contentManager.factory
                .createContent(browser.getComponent(), null, false)
                .apply { setDisposer(browser) }
        } else {
            val component = JBPanelWithEmptyText().apply {
                emptyText.text = "JCEF is not supported. Please make sure to use Jetbrains JDK."
            }
            toolWindow.contentManager.factory
                .createContent(component, null, false)
        }

        toolWindow.contentManager.addContent(content)
    }
}
