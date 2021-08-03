package io.gitlab.arturbosch.detekt.idea

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.ExternalAnnotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile
import io.gitlab.arturbosch.detekt.api.Finding
import io.gitlab.arturbosch.detekt.api.TextLocation
import io.gitlab.arturbosch.detekt.idea.config.DetektConfigStorage
import io.gitlab.arturbosch.detekt.idea.util.isDetektEnabled
import io.gitlab.arturbosch.detekt.idea.util.showNotification

class DetektAnnotator : ExternalAnnotator<PsiFile, List<Finding>>() {

    override fun collectInformation(file: PsiFile): PsiFile = file

    override fun doAnnotate(collectedInfo: PsiFile): List<Finding> {
        if (!collectedInfo.project.isDetektEnabled()) {
            return emptyList()
        }
        val service = ConfiguredService(collectedInfo.project)

        val problems = service.validate()
        if (problems.isNotEmpty()) {
            showNotification(problems, collectedInfo.project)
            return emptyList()
        }

        return service.execute(collectedInfo, autoCorrect = false)
    }

    override fun apply(
        file: PsiFile,
        annotationResult: List<Finding>,
        holder: AnnotationHolder
    ) {
        val configuration = DetektConfigStorage.instance(file.project)
        for (finding in annotationResult) {
            val textRange = finding.charPosition.toTextRange()
            val message = buildString {
                append("Detekt - ")
                append(finding.id)
                append(": ")
                append(finding.messageOrDescription())
            }
            val severity = if (configuration.treatAsError) HighlightSeverity.ERROR else HighlightSeverity.WARNING
            holder.newAnnotation(severity, message)
                .range(textRange)
                .create()
        }
    }

    private fun TextLocation.toTextRange(): TextRange = TextRange.create(start, end)
}
