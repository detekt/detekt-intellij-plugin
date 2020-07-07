package io.gitlab.arturbosch.detekt.idea

import com.intellij.openapi.project.Project
import com.intellij.util.io.exists
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Finding
import io.gitlab.arturbosch.detekt.cli.CliArgs
import io.gitlab.arturbosch.detekt.cli.loadConfiguration
import io.gitlab.arturbosch.detekt.core.DetektFacade
import io.gitlab.arturbosch.detekt.core.FileProcessorLocator
import io.gitlab.arturbosch.detekt.core.ProcessingSettings
import io.gitlab.arturbosch.detekt.core.RuleSetLocator
import io.gitlab.arturbosch.detekt.idea.config.DetektConfigStorage
import io.gitlab.arturbosch.detekt.idea.util.DirectExecuter
import io.gitlab.arturbosch.detekt.idea.util.absolutePath
import io.gitlab.arturbosch.detekt.idea.util.extractPaths
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class ConfiguredService(private val project: Project) {

    private val storage = DetektConfigStorage.instance(project)

    fun validate(): List<String> {
        val messages = mutableListOf<String>()

        plugins()
            .filter { Files.notExists(it) }
            .forEach { messages += "Plugin jar <b>$it</b> does not exist." }

        configPaths()
            .filter { Files.notExists(it) }
            .forEach { messages += "Configuration file <b>$it</b> does not exist." }

        val baseline = baseline()
        if (baseline != null && !baseline.exists()) {
            messages += "The provided baseline file <b>$baseline</b> does not exist."
        }

        return messages
    }

    fun config(autoCorrect: Boolean = false): Config = CliArgs().apply {
        config = configPaths().joinToString(",")
        this.autoCorrect = autoCorrect
        failFast = storage.enableAllRules
        buildUponDefaultConfig = storage.buildUponDefaultConfig
    }.loadConfiguration()

    private fun settings(files: List<Path>, autoCorrect: Boolean): ProcessingSettings =
        ProcessingSettings(
            inputPaths = files,
            autoCorrect = autoCorrect,
            config = config(autoCorrect),
            pluginPaths = plugins(),
            executorService = DirectExecuter(),
            outPrinter = System.out,
            errPrinter = System.err
        )

    private fun configPaths(): List<Path> = extractPaths(storage.rulesPath, project)

    private fun plugins(): List<Path> = extractPaths(storage.pluginPaths, project)

    private fun baseline(): Path? = storage.baselinePath.trim()
        .takeIf { it.isNotEmpty() }
        ?.let { Paths.get(absolutePath(project, storage.baselinePath)) }

    private fun facade(settings: ProcessingSettings): DetektFacade {
        var providers = RuleSetLocator(settings).load()
        if (!storage.enableFormatting) {
            providers = providers.filterNot { it.ruleSetId == FORMATTING_RULE_SET_ID }
        }
        val processors = FileProcessorLocator(settings).load()
        return DetektFacade.create(settings, providers, processors)
    }

    fun execute(path: Path, autoCorrect: Boolean): List<Finding> {
        val settings = settings(listOf(path), autoCorrect)

        fun run(settings: ProcessingSettings): List<Finding> {
            val result = facade(settings).run()
            return result.findings.flatMap { it.value }
        }

        return settings.use(::run)
    }
}
