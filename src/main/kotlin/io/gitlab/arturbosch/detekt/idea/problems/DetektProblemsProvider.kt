package io.gitlab.arturbosch.detekt.idea.problems

import com.intellij.analysis.problemsView.ProblemsProvider
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

@Service
class DetektProblemsProvider(override val project: Project) : ProblemsProvider {

    companion object {
        fun getInstance(project: Project): DetektProblemsProvider = project.service()
    }
}
