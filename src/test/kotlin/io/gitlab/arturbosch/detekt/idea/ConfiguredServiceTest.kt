package io.gitlab.arturbosch.detekt.idea

import com.intellij.openapi.components.service
import com.intellij.util.io.readText
import io.github.detekt.test.utils.resourceAsPath
import io.gitlab.arturbosch.detekt.idea.config.DetektPluginSettings
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ConfiguredServiceTest : MockProjectTestCase() {

    @Test
    fun `non configured plugin has no problems`() {
        val service = ConfiguredService(project)

        val problems = service.validate()

        assertThat(problems).isEmpty()
    }

    @Test
    fun `invalid config found`() {
        val settings = project.service<DetektPluginSettings>()
        settings.configurationFilePaths = mutableListOf("example")

        val problems = ConfiguredService(project).validate()

        assertThat(problems).hasSize(1)
        assertThat(problems.first()).contains("Configuration file")
        assertThat(problems.first()).contains("does not exist")
    }

    @Test
    fun `invalid baseline file found`() {
        val settings = project.service<DetektPluginSettings>()
        settings.baselinePath = "example"

        val problems = ConfiguredService(project).validate()

        assertThat(problems).hasSize(1)
        assertThat(problems.first()).contains("baseline file")
        assertThat(problems.first()).contains("does not exist")
    }

    @Test
    fun `invalid plugin found`() {
        val settings = project.service<DetektPluginSettings>()
        settings.pluginJarPaths = mutableListOf("example")

        val problems = ConfiguredService(project).validate()

        assertThat(problems).hasSize(1)
        assertThat(problems.first()).contains("Plugin jar")
        assertThat(problems.first()).contains("does not exist")
    }

    @Test
    fun `expect detekt runs successfully`() {
        val service = ConfiguredService(project)
        val testPath = resourceAsPath("testData/Poko.kt")

        assertThat(
            service.execute(
                testPath.readText(),
                testPath.toString(),
                autoCorrect = false
            )
        ).isNotEmpty
    }

    @Test
    fun `debugging fragments are excluded from analysis`() {
        val service = ConfiguredService(project)

        val findings = service.execute("", SPECIAL_FILENAME_FOR_DEBUGGING, false)

        assertThat(findings).isEmpty()
    }
}
