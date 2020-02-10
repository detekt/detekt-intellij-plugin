package io.gitlab.arturbosch.detekt.util

import io.gitlab.arturbosch.detekt.cli.CliArgs
import io.gitlab.arturbosch.detekt.cli.loadConfiguration
import io.gitlab.arturbosch.detekt.config.DetektConfigStorage
import io.gitlab.arturbosch.detekt.core.DetektFacade
import io.gitlab.arturbosch.detekt.core.FileProcessorLocator
import io.gitlab.arturbosch.detekt.core.ProcessingSettings
import io.gitlab.arturbosch.detekt.core.RuleSetLocator
import java.nio.file.Path
import java.util.concurrent.ForkJoinPool

class DetektPluginService {

    fun createFacade(settings: ProcessingSettings, withoutFormatting: Boolean = false): DetektFacade {
        var providers = RuleSetLocator(settings).load()
        if (withoutFormatting) {
            providers = providers.filterNot { it.ruleSetId == FORMATTING_RULE_SET_ID }
        }
        val processors = FileProcessorLocator(settings).load()
        return DetektFacade.create(settings, providers, processors)
    }

    fun getProcessSettings(
        file: Path,
        rulesPath: String,
        configStorage: DetektConfigStorage,
        autoCorrect: Boolean,
        pluginPaths: List<Path>
    ) = ProcessingSettings(
        inputPath = file,
        autoCorrect = autoCorrect,
        config = CliArgs().apply {
            config = rulesPath
            failFast = configStorage.failFast
            buildUponDefaultConfig = configStorage.buildUponDefaultConfig
        }.loadConfiguration(),
        pluginPaths = pluginPaths,
        executorService = ForkJoinPool.commonPool()
    )
}
