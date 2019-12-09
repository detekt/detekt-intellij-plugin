package io.gitlab.arturbosch.detekt.util

import com.intellij.openapi.vfs.VirtualFile
import io.gitlab.arturbosch.detekt.cli.CliArgs
import io.gitlab.arturbosch.detekt.cli.loadConfiguration
import io.gitlab.arturbosch.detekt.config.DetektConfigStorage
import io.gitlab.arturbosch.detekt.core.DetektFacade
import io.gitlab.arturbosch.detekt.core.FileProcessorLocator
import io.gitlab.arturbosch.detekt.core.ProcessingSettings
import io.gitlab.arturbosch.detekt.core.RuleSetLocator
import java.nio.file.Paths
import java.util.concurrent.ForkJoinPool

fun createFacade(settings: ProcessingSettings, configuration: DetektConfigStorage): DetektFacade {
    var providers = RuleSetLocator(settings).load()
    if (!configuration.enableFormatting) {
        providers = providers.filterNot { it.ruleSetId == "formatting" }
    }
    val processors = FileProcessorLocator(settings).load()
    return DetektFacade.create(settings, providers, processors)
}

fun getProcessSettings(virtualFile: VirtualFile, rulesPath: String,
                                configStorage: DetektConfigStorage): ProcessingSettings {
    return ProcessingSettings(
        inputPath = Paths.get(virtualFile.path),
        autoCorrect = false,
        config = CliArgs().apply {
            config = rulesPath
            failFast = configStorage.failFast
            buildUponDefaultConfig = configStorage.buildUponDefaultConfig
        }.loadConfiguration(),
        executorService = ForkJoinPool.commonPool()
    )
}
