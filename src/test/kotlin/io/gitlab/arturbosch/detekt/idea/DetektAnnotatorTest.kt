package io.gitlab.arturbosch.detekt.idea

import com.intellij.openapi.application.WriteAction
import com.intellij.psi.PsiFile
import io.gitlab.arturbosch.detekt.idea.config.DetektConfigStorage
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEmpty
import strikt.assertions.isNotEmpty

class DetektAnnotatorTest : DetektPluginTestCase() {

    @Test
    fun `returns empty list when detekt is disabled`() {
        val file = myFixture.copyFileToProject("Poko.kt")
        val psi = WriteAction.computeAndWait<PsiFile, Throwable> { psiManager.findFile(file)!! }

        val findings = DetektAnnotator().doAnnotate(psi)

        expectThat(findings).isEmpty()
    }

    @Test
    @Disabled("Dependency conflicts between embeddable and normal kotlin compiler.")
    fun `report issues when detekt is enabled`() {
        DetektConfigStorage.instance(project).enableDetekt = true
        val file = myFixture.copyFileToProject("Poko.kt")
        val psi = WriteAction.computeAndWait<PsiFile, Throwable> { psiManager.findFile(file)!! }

        val findings = DetektAnnotator().doAnnotate(psi)

        expectThat(findings).isNotEmpty()
    }
}
