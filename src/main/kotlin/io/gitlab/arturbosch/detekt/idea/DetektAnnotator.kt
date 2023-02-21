package io.gitlab.arturbosch.detekt.idea

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.ExternalAnnotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.components.service
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile
import io.gitlab.arturbosch.detekt.api.CorrectableCodeSmell
import io.gitlab.arturbosch.detekt.api.Finding
import io.gitlab.arturbosch.detekt.api.TextLocation
import io.gitlab.arturbosch.detekt.idea.action.AddToBaselineAction
import io.gitlab.arturbosch.detekt.idea.config.DetektPluginSettings
import io.gitlab.arturbosch.detekt.idea.intention.AutoCorrectIntention
import io.gitlab.arturbosch.detekt.idea.util.isDetektEnabled
import io.gitlab.arturbosch.detekt.idea.util.showNotification
import org.jetbrains.kotlin.idea.KotlinLanguage

class DetektAnnotator : ExternalAnnotator<PsiFile, List<Finding>>() {

    override fun collectInformation(file: PsiFile): PsiFile = file

    override fun doAnnotate(collectedInfo: PsiFile): List<Finding> {
        if (
            !collectedInfo.project.isDetektEnabled() ||
            !isKotlinFile(collectedInfo)
        ) {
            return emptyList()
        }

        val service = ConfiguredService(collectedInfo.project)
        val problems = service.validate()

        return if (problems.isNotEmpty()) {
            showNotification(problems, collectedInfo.project)
            emptyList()
        } else {
            service.execute(collectedInfo, autoCorrect = false)
        }
    }

    private fun isKotlinFile(psiFile: PsiFile): Boolean =
        psiFile.language.id == KotlinLanguage.INSTANCE.id ||
            psiFile.getUserData(TEST_KOTLIN_LANGUAGE_ID_KEY) == KotlinLanguage.INSTANCE.id

    override fun apply(
        file: PsiFile,
        annotationResult: List<Finding>,
        holder: AnnotationHolder,
    ) {
        val settings = file.project.service<DetektPluginSettings>()
        for (finding in annotationResult) {
            val textRange = finding.charPosition.toTextRange()
            val message = buildString {
                append("Detekt - ")
                append(finding.id)
                append(": ")
                append(finding.messageOrDescription())
            }
            val severity = if (settings.treatAsErrors) HighlightSeverity.ERROR else HighlightSeverity.WARNING
            val annotationBuilder = holder.newAnnotation(severity, message)
                .range(textRange)

            if (textRange == file.textRange) {
                annotationBuilder.fileLevel()
            }

            if (finding is CorrectableCodeSmell) {
                annotationBuilder.withFix(AutoCorrectIntention())
            } else {
                annotationBuilder.withFix(AddToBaselineAction(finding))
            }

            annotationBuilder.create()
        }
    }

    private fun TextLocation.toTextRange(): TextRange = TextRange.create(start, end)
}
