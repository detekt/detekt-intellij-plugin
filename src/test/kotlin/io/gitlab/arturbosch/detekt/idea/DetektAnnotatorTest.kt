package io.gitlab.arturbosch.detekt.idea

import com.intellij.openapi.components.service
import io.github.detekt.test.utils.resourceAsPath
import io.gitlab.arturbosch.detekt.api.Finding
import io.gitlab.arturbosch.detekt.idea.config.DetektPluginSettings
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class DetektAnnotatorTest : KotlinParsingTestCase() {

    @Test
    fun `test returns empty list when detekt is disabled`() {
        assertThat(runAnnotator(enabled = false)).isEmpty()
    }

    @Test
    fun `test report issues when detekt is enabled`() {
        assertThat(runAnnotator(enabled = true)).isNotEmpty
    }

    private fun runAnnotator(enabled: Boolean): List<Finding> {
        project.service<DetektPluginSettings>().enableDetekt = enabled
        val psi = parseFile(resourceAsPath("testData/Poko.kt"))
        return DetektAnnotator().doAnnotate(psi)
    }
}
