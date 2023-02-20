package io.gitlab.arturbosch.detekt.idea

import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.components.service
import com.intellij.psi.PsiFile
import io.gitlab.arturbosch.detekt.api.Finding
import io.gitlab.arturbosch.detekt.idea.config.DetektPluginSettings
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

@Disabled("https://github.com/JetBrains/gradle-intellij-plugin/issues/1109")
class DetektAnnotatorTest : DetektPluginTestCase() {

    @Test
    fun `returns empty list when detekt is disabled`() {
        assertThat(runAnnotator(enabled = false)).isEmpty()
    }

    @Test
    fun `report issues when detekt is enabled`() {
        assertThat(runAnnotator(enabled = true)).isNotEmpty
    }

    private fun runAnnotator(enabled: Boolean): List<Finding> {
        project.service<DetektPluginSettings>().enableDetekt = enabled
        val file = myFixture.copyFileToProject("Poko.kt")
        val psi = WriteAction.computeAndWait<PsiFile, Throwable> { psiManager.findFile(file) }
        return DetektAnnotator().doAnnotate(psi)
    }
}
