package io.gitlab.arturbosch.detekt.idea.problems

import com.intellij.analysis.problemsView.toolWindow.ProblemsViewPanel
import com.intellij.analysis.problemsView.toolWindow.ProblemsViewState
import com.intellij.analysis.problemsView.toolWindow.ProblemsViewToolWindowUtils
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.project.Project
import io.gitlab.arturbosch.detekt.idea.DetektBundle

private const val FINDINGS_TAB_ID = "io.github.arturbosch.detekt.idea.findings.tab"

class FindingsViewTab(
    project: Project,
    state: ProblemsViewState,
) : ProblemsViewPanel(
    project,
    FINDINGS_TAB_ID,
    state,
    DetektBundle.lazy("detekt.configuration.title"),
) {

    init {
        treeModel.root = FindingsRootNode(project, this)
        FindingsManager.getInstance(project).register {
            invokeLater {
                treeModel.structureChanged(null)
                ProblemsViewToolWindowUtils.selectTab(project, FINDINGS_TAB_ID)
            }
        }
    }
}
