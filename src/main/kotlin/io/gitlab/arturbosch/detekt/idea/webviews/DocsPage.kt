package io.gitlab.arturbosch.detekt.idea.webviews

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager

enum class DocsPage {
    Rules,
    Changelog,
}

fun showPage(page: DocsPage, project: Project) {
    ToolWindowManager.getInstance(project)
        .getToolWindow(DocumentationToolWindow.ID)
        ?.show {
            val browser = RulesBrowserService.getInstance()
            when (page) {
                DocsPage.Rules -> browser.loadRulesPage()
                DocsPage.Changelog -> browser.loadChangelog()
            }
        }
}
