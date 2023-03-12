package io.gitlab.arturbosch.detekt.idea.problems

import com.intellij.analysis.problemsView.toolWindow.ProblemsViewPanelProvider
import com.intellij.analysis.problemsView.toolWindow.ProblemsViewState
import com.intellij.analysis.problemsView.toolWindow.ProblemsViewTab
import com.intellij.openapi.project.Project

class FindingsView(private val project: Project) : ProblemsViewPanelProvider {

    override fun create(): ProblemsViewTab = FindingsViewTab(project, ProblemsViewState.getInstance(project))
}
