package io.gitlab.arturbosch.detekt.idea.config

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.components.service
import com.intellij.openapi.options.BoundSearchableConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import io.gitlab.arturbosch.detekt.idea.DetektBundle
import io.gitlab.arturbosch.detekt.idea.config.ui.DetektConfigUi

class DetektConfig(private val project: Project) : BoundSearchableConfigurable(
    displayName = DetektBundle.message("detekt.configuration.title"),
    helpTopic = DetektBundle.message("detekt.configuration.title"),
    _id = "io.github.detekt.config",
) {

    private val settings = project.service<DetektPluginSettings>()

    override fun createPanel(): DialogPanel {
        return DetektConfigUi(settings, project).createPanel()
    }

    override fun apply() {
        super.apply()
        DaemonCodeAnalyzer.getInstance(project).restart()
    }
}
