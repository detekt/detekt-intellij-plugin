package io.gitlab.arturbosch.detekt.idea.action

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.util.io.exists
import io.github.detekt.tooling.api.BaselineProvider
import io.gitlab.arturbosch.detekt.api.Finding
import io.gitlab.arturbosch.detekt.idea.DETEKT
import io.gitlab.arturbosch.detekt.idea.DetektBundle
import io.gitlab.arturbosch.detekt.idea.config.DetektPluginSettings
import io.gitlab.arturbosch.detekt.idea.util.absoluteBaselinePath

class AddToBaselineAction(private val finding: Finding) : IntentionAction {

    override fun startInWriteAction(): Boolean = true

    override fun getText(): String = DetektBundle.message("detekt.actions.addToBaseline", finding.id)

    override fun getFamilyName(): String = DETEKT

    override fun isAvailable(project: Project, editor: Editor, file: PsiFile): Boolean {
        val id = finding.compactWithSignature()
        return id.trim().isNotEmpty() && project.isBaselineDefinedAndValid()
    }

    private fun Project.isBaselineDefinedAndValid(): Boolean {
        val settings = this.service<DetektPluginSettings>()
        val baseline = absoluteBaselinePath(this, settings)
        return baseline != null && baseline.exists()
    }

    override fun invoke(project: Project, editor: Editor, file: PsiFile) {
        val settings = project.service<DetektPluginSettings>()
        val baselinePath = requireNotNull(absoluteBaselinePath(project, settings))
        val provider = BaselineProvider.load()
        val baseline = provider.read(baselinePath)
        val newBaseline = provider.of(
            baseline.manuallySuppressedIssues + provider.id(finding),
            baseline.currentIssues,
        )
        provider.write(baselinePath, newBaseline)
        DaemonCodeAnalyzer.getInstance(project).restart()
    }
}
