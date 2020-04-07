package io.gitlab.arturbosch.detekt.idea.config

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.project.Project
import io.gitlab.arturbosch.detekt.idea.DETEKT
import javax.swing.JComponent

class DetektConfig(private val project: Project) : SearchableConfigurable {

    private val detektConfigStorage: DetektConfigStorage = DetektConfigStorage.instance(project)
    private val detektConfigurationForm: DetektConfigurationForm = DetektConfigurationForm(project)

    override fun isModified(): Boolean = detektConfigurationForm.isModified

    override fun getId(): String = "io.github.detekt.config"

    override fun getDisplayName(): String = DETEKT

    override fun apply() {
        detektConfigurationForm.apply()
        DaemonCodeAnalyzer.getInstance(project).restart()
    }

    override fun reset() = detektConfigurationForm.reset()

    override fun createComponent(): JComponent? = detektConfigurationForm.createPanel(detektConfigStorage)
}
