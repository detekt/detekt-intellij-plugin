package io.gitlab.arturbosch.detekt.idea.problems

import com.intellij.analysis.problemsView.toolWindow.ProblemsViewPanel
import com.intellij.analysis.problemsView.toolWindow.ProblemsViewState
import com.intellij.openapi.project.Project
import io.gitlab.arturbosch.detekt.idea.DetektBundle

class FindingsViewTab(
    project: Project,
    state: ProblemsViewState,
) : ProblemsViewPanel(
    project,
    "io.github.arturbosch.detekt.idea.findings.tab",
    state,
    DetektBundle.lazy("detekt.configuration.title"),
) {

    init {
        treeModel.root = FindingsRootNode(project, this)
        FindingsManager.getInstance().register { treeModel.structureChanged(null) }
    }
}
