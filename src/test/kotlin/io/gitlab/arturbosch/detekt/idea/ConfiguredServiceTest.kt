package io.gitlab.arturbosch.detekt.idea

import io.gitlab.arturbosch.detekt.api.internal.DisabledAutoCorrectConfig
import io.gitlab.arturbosch.detekt.api.internal.YamlConfig
import io.gitlab.arturbosch.detekt.idea.config.DetektConfigStorage
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.hasSize
import strikt.assertions.isA
import strikt.assertions.isEmpty
import strikt.assertions.isTrue

class ConfiguredServiceTest : DetektPluginTestCase() {

    @Test
    fun `non configured plugin has no problems`() {
        val service = ConfiguredService(project)

        val problems = service.validate()

        expectThat(problems).isEmpty()
    }

    @Test
    fun `invalid config found`() {
        val config = DetektConfigStorage.instance(project)
        config.rulesPath = "asadadasdas"

        val problems = ConfiguredService(project).validate()

        expectThat(problems).hasSize(1)
        expectThat(problems.first()).contains("Configuration file")
        expectThat(problems.first()).contains("does not exist")
    }

    @Test
    fun `invalid baseline file found`() {
        val config = DetektConfigStorage.instance(project)
        config.baselinePath = "asadadasdas"

        val problems = ConfiguredService(project).validate()

        expectThat(problems).hasSize(1)
        expectThat(problems.first()).contains("baseline file")
        expectThat(problems.first()).contains("does not exist")
    }

    @Test
    fun `invalid plugin found`() {
        val config = DetektConfigStorage.instance(project)
        config.pluginPaths = "asadadasdas"

        val problems = ConfiguredService(project).validate()

        expectThat(problems).hasSize(1)
        expectThat(problems.first()).contains("Plugin jar")
        expectThat(problems.first()).contains("does not exist")
    }

    @Test
    fun `auto correct is disabled by default`() {
        val config = ConfiguredService(project).config()

        expectThat(config).isA<DisabledAutoCorrectConfig>()
    }

    @Test
    fun `auto correct can be enabled`() {
        val config = ConfiguredService(project).config(autoCorrect = true)

        expectThat(config).isA<YamlConfig>()
    }

    @Test
    fun `all experimental rules are active`() {
        val config = DetektConfigStorage.instance(project)
        config.enableAllRules = true

        val service = ConfiguredService(project)

        val detektConfig = service.config()

        expectThat(detektConfig.valueOrDefault("active", false)).isTrue()
    }
}
