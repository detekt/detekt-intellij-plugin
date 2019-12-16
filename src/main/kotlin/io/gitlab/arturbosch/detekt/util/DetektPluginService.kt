package io.gitlab.arturbosch.detekt.util

import com.intellij.openapi.vfs.VirtualFile
import io.gitlab.arturbosch.detekt.cli.CliArgs
import io.gitlab.arturbosch.detekt.cli.loadConfiguration
import io.gitlab.arturbosch.detekt.config.DetektConfigStorage
import io.gitlab.arturbosch.detekt.core.DetektFacade
import io.gitlab.arturbosch.detekt.core.FileProcessorLocator
import io.gitlab.arturbosch.detekt.core.ProcessingSettings
import io.gitlab.arturbosch.detekt.core.RuleSetLocator
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.ForkJoinPool

class DetektPluginService(
    private val configStorage: DetektConfigStorage
) {

    fun createFacade(settings: ProcessingSettings, withoutFormatting: Boolean = false): DetektFacade {
        var providers = RuleSetLocator(settings).load()
        if (withoutFormatting) {
            providers = providers.filterNot { it.ruleSetId == "formatting" }
        }
        val processors = FileProcessorLocator(settings).load()
        return DetektFacade.create(settings, providers, processors)
    }

    fun getProcessSettings(
        virtualFile: VirtualFile,
        rulesPath: String,
        configStorage: DetektConfigStorage,
        autoCorrect: Boolean,
        pluginPaths: List<Path>
    ): ProcessingSettings {
        return ProcessingSettings(
            inputPath = Paths.get(virtualFile.path),
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
}
