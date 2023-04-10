package io.gitlab.arturbosch.detekt.idea.problems

import com.intellij.analysis.problemsView.ProblemsProvider
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project

@Service
class DetektProblemsProvider(override val project: Project) : ProblemsProvider {

    override fun dispose() {
        // empty override to fix:
        // "references an unresolved class com.intellij.analysis.problemsView.ProblemsProvider.DefaultImpls"
    }
}
