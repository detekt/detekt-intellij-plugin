package io.gitlab.arturbosch.detekt.idea.intention

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.util.parentOfType
import io.gitlab.arturbosch.detekt.api.Finding
import io.gitlab.arturbosch.detekt.idea.DETEKT
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtModifierListOwner
import org.jetbrains.kotlin.psi.KtPsiFactory

class SuppressFindingIntention(private val finding: Finding) : IntentionAction {

    override fun startInWriteAction(): Boolean = true

    override fun getText(): String = "Suppress $DETEKT finding"

    override fun getFamilyName(): String = DETEKT

    override fun isAvailable(project: Project, editor: Editor, file: PsiFile): Boolean = true

    override fun invoke(project: Project, editor: Editor, file: PsiFile) {
        val factory = KtPsiFactory(project)
        println("I'm called")
        val textLocation = finding.entity.location.text
        val elem = file.findElementAt(textLocation.start)
        val modifierOwner = elem?.parentOfType<KtModifierListOwner>(withSelf = true) ?: return
        val existingSuppression = findSuppressAnnotation(modifierOwner)
        if (existingSuppression != null) {
            TODO("impl")
        } else {
            val suppress = factory.createAnnotationEntry("Suppress")
            suppress.valueArgumentList?.addArgument(factory.createArgument("detekt.${finding.id}"))
            modifierOwner.addAnnotationEntry(suppress)
        }
//        val annotation =
//            AnnotationUtil.findAnnotation(modifierOwner, JavaSuppressionUtil.SUPPRESS_INSPECTIONS_ANNOTATION_NAME)

//        val annotatable = file.elementsInRange(TextRange(textLocation.start, textLocation.end))
//            .asSequence()
//            .map { it.parentOfType<KtExpression>(withSelf = true)  }
//            .first()
//        if (annotatable != null) {
//            KotlinSuppressIntentionAction(
//                annotatable,
//                "$DETEKT.${finding.id}",
//                AnnotationHostKind("kind?", "name?", true)
//            ).invoke(project, editor, file)
//        }
    }

    private fun findSuppressAnnotation(modifierOwner: KtModifierListOwner?): KtAnnotationEntry? =
        modifierOwner?.annotationEntries?.find { it.shortName?.identifier == "Suppress" }
            ?: modifierOwner?.annotationEntries?.find { it.shortName?.identifier == "SuppressWarnings" }
}
