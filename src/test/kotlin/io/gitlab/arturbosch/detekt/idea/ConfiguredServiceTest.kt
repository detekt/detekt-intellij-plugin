package io.gitlab.arturbosch.detekt.idea

import io.github.detekt.test.utils.readResourceContent
import io.github.detekt.test.utils.resourceAsPath
import io.gitlab.arturbosch.detekt.idea.config.DetektConfigStorage
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.junit.jupiter.api.Test

class ConfiguredServiceTest : DetektPluginTestCase() {

    @Test
    fun `non configured plugin has no problems`() {
        val service = ConfiguredService(project)

        val problems = service.validate()

        assertThat(problems).isEmpty()
    }

    @Test
    fun `invalid config found`() {
        val config = DetektConfigStorage.instance(project)
        config.configPaths = "asadadasdas"

        val problems = ConfiguredService(project).validate()

        assertThat(problems).hasSize(1)
        assertThat(problems.first()).contains("Configuration file")
        assertThat(problems.first()).contains("does not exist")
    }

    @Test
    fun `invalid baseline file found`() {
        val config = DetektConfigStorage.instance(project)
        config.baselinePath = "asadadasdas"

        val problems = ConfiguredService(project).validate()

        assertThat(problems).hasSize(1)
        assertThat(problems.first()).contains("baseline file")
        assertThat(problems.first()).contains("does not exist")
    }

    @Test
    fun `invalid plugin found`() {
        val config = DetektConfigStorage.instance(project)
        config.pluginPaths = "asadadasdas"

        val problems = ConfiguredService(project).validate()

        assertThat(problems).hasSize(1)
        assertThat(problems.first()).contains("Plugin jar")
        assertThat(problems.first()).contains("does not exist")
    }

    @Test
    fun `expect detekt runs successfully`() {
        val service = ConfiguredService(project)

        // If a NoSuchMethodError is thrown, it means detekt-core was called and creating a KotlinEnvironment
        // failed due to conflicted compiler vs embedded-compiler dependency.
        // IntelliJ isolates plugins in an own classloader so detekt runs fine.
        // In the testcase this is not possible but it is enough to prove detekt runs and does not crash due to
        // regressions in this plugin.
        assertThatCode {
            service.execute(
                readResourceContent("testData/Poko.kt"),
                resourceAsPath("testData/Poko.kt").toString(),
                autoCorrect = false
            )
        }
            .isInstanceOf(NoSuchMethodError::class.java)
            .hasMessageContaining(
                    "org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment${"$"}Companion.createForProduction"
            )
    }

    @Test
    fun `debugging fragments are excluded from analysis`() {
        val service = ConfiguredService(project)

        val findings = service.execute("", SPECIAL_FILENAME_FOR_DEBUGGING, false)

        assertThat(findings).isEmpty()
    }
}
