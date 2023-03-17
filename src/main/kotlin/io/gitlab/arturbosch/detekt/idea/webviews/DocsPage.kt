package io.gitlab.arturbosch.detekt.idea.webviews

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager

enum class DocsPage(val url: String) {
    Rules("https://detekt.dev/docs/rules/style"),
    Changelog("https://detekt.dev/changelog"),
}

fun showPage(page: DocsPage, project: Project) {
    ToolWindowManager.getInstance(project)
        .getToolWindow(DocumentationToolWindow.ID)
        ?.show { RulesBrowserService.getInstance().loadPage(page) }
}
