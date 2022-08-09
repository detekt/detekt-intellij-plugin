package io.gitlab.arturbosch.detekt.idea.config

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.components.service
import com.intellij.openapi.options.BoundSearchableConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import io.gitlab.arturbosch.detekt.idea.DetektBundle
import io.gitlab.arturbosch.detekt.idea.config.ui.LegacyDetektConfigUiProvider
import io.gitlab.arturbosch.detekt.idea.config.ui.NewDetektConfigUiProvider
import io.gitlab.arturbosch.detekt.idea.util.PluginUtils

class DetektConfig(private val project: Project) : BoundSearchableConfigurable(
    displayName = DetektBundle.message("detekt.configuration.title"),
    helpTopic = DetektBundle.message("detekt.configuration.title"),
    _id = "io.github.detekt.config",
) {

    private val settings = project.service<DetektPluginSettings>()

    override fun createPanel(): DialogPanel {
        // The Kotlin V2 UI DSL is only available on platform versions 2021.3 and onwards
        val ui = if (PluginUtils.isAtLeastIJBuild(MIN_KOTLIN_DSL_V2_IJ_VERSION)) {
            NewDetektConfigUiProvider(settings, project)
        } else {
            LegacyDetektConfigUiProvider(settings, project)
        }
        return ui.createPanel()
    }

    override fun apply() {
        super.apply()
        DaemonCodeAnalyzer.getInstance(project).restart()
    }

    companion object {

        private const val MIN_KOTLIN_DSL_V2_IJ_VERSION = "213.5744.223"
    }
}
