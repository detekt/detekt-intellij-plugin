package io.gitlab.arturbosch.detekt.idea.intention

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import io.gitlab.arturbosch.detekt.idea.DETEKT
import io.gitlab.arturbosch.detekt.idea.action.AutoCorrectAction

class AutoCorrectIntention : IntentionAction {

    override fun startInWriteAction(): Boolean = true

    override fun getText(): String = "Auto-correct with $DETEKT"

    override fun getFamilyName(): String = DETEKT

    override fun isAvailable(project: Project, editor: Editor, file: PsiFile): Boolean = true

    override fun invoke(project: Project, editor: Editor, file: PsiFile) {
        AutoCorrectAction().runAction(project, file)
    }
}
