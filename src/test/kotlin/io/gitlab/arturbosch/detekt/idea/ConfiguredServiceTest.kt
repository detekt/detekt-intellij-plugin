package io.gitlab.arturbosch.detekt.idea

import io.github.detekt.test.utils.readResourceContent
import io.github.detekt.test.utils.resourceAsPath
import io.gitlab.arturbosch.detekt.idea.config.DetektConfigStorage
import org.junit.jupiter.api.Test
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.hasSize
import strikt.assertions.isA
import strikt.assertions.isEmpty
import strikt.assertions.isFailure
import strikt.assertions.isSuccess

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
    fun `expect detekt runs successfully`() {
        val service = ConfiguredService(project)

        // If a ClassCastException is thrown, it means detekt-core was called and creating a KotlinEnvironment
        // failed due to conflicted compiler vs embedded-compiler dependency.
        // IntelliJ isolates plugins in an own classloader so detekt runs fine.
        // In the testcase this is not possible but it is enough to prove detekt runs and does not crash due to
        // regressions in this plugin.
        expectCatching {
            service.execute(
                readResourceContent("testData/Poko.kt"),
                resourceAsPath("testData/Poko.kt").toString(),
                autoCorrect = false
            )
        }
            .isFailure()
            .isA<ClassCastException>()
    }

    @Test
    fun `debugging fragments are excluded from analysis`() {
        val service = ConfiguredService(project)

        expectCatching { service.execute("", SPECIAL_FILENAME_FOR_DEBUGGING, false) }
            .isSuccess()
            .isEmpty()
    }
}
