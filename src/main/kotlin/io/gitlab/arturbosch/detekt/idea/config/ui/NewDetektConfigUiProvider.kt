package io.gitlab.arturbosch.detekt.idea.config.ui

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.BottomGap
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import com.intellij.ui.layout.selected
import io.gitlab.arturbosch.detekt.idea.DetektBundle
import io.gitlab.arturbosch.detekt.idea.config.DetektPluginSettings
import javax.swing.JCheckBox

@Suppress("DialogTitleCapitalization") // It gets tripped up by the capitalization of Detekt's name
internal class NewDetektConfigUiProvider(
    private val settings: DetektPluginSettings,
    private val project: Project
) : DetektConfigUiProvider {

    override fun createPanel(): DialogPanel = panel {
        lateinit var detektEnabledCheckbox: JCheckBox
        row {
            detektEnabledCheckbox = checkBox(DetektBundle.message("detekt.configuration.enableDetekt"))
                .bindSelected(settings::enableDetekt)
                .component
        }
        row {
            checkBox(DetektBundle.message("detekt.configuration.treatAsErrors"))
                .bindSelected(settings::treatAsErrors)
                .enabledIf(detektEnabledCheckbox.selected)
        }

        rulesGroup()
            .enabledIf(detektEnabledCheckbox.selected)

        filesGroup()
            .enabledIf(detektEnabledCheckbox.selected)
    }

    // TODO replace with newer overload once the min IJ version is >= 22.1
    // MissingRecentApi: this is only meant to be used on IJ 21.3 and later
    @Suppress("UnstableApiUsage", "MissingRecentApi", "Deprecation")
    private fun Panel.rulesGroup() =
        group(
            title = DetektBundle.message("detekt.configuration.rulesGroup.title"),
            indent = false,
            topGroupGap = false
        ) {
            row {
                checkBox(DetektBundle.message("detekt.configuration.buildUponDefaultConfig"))
                    .bindSelected(settings::buildUponDefaultConfig)
            }
            row {
                checkBox(DetektBundle.message("detekt.configuration.enableFormattingRules"))
                    .bindSelected(settings::enableFormatting)
            }
            row {
                checkBox(DetektBundle.message("detekt.configuration.enableAllRules"))
                    .bindSelected(settings::enableAllRules)
            }
        }

    // TODO replace with newer overload once the min IJ version is >= 22.1
    // MissingRecentApi: this is only meant to be used on IJ 21.3 and later
    @Suppress("UnstableApiUsage", "MissingRecentApi", "Deprecation")
    private fun Panel.filesGroup() =
        group(
            title = DetektBundle.message("detekt.configuration.filesGroup.title"),
            indent = false,
            topGroupGap = false
        ) {
            row(DetektBundle.message("detekt.configuration.configurationFiles.title")) {
                val listModel = FilesListPanel.ListModel(settings.configurationFilePaths.toList().sorted())
                val filesListPanel = FilesListPanel(
                    listModel = listModel,
                    project = project,
                    fileChooserTitle = DetektBundle.message("detekt.configuration.configurationFiles.dialog.title"),
                    fileChooserDescription =
                    DetektBundle.message("detekt.configuration.configurationFiles.dialog.description")
                ) {
                    settings.configurationFilePaths = it.toMutableSet()
                }.decorated()

                @Suppress("DEPRECATION") // TODO replace with newer overload once the min IJ version is >= 22.1
                cell(filesListPanel, filesListPanel)
                    .horizontalAlign(HorizontalAlign.FILL)
                    .resizableColumn()
                    .comment(DetektBundle.message("detekt.configuration.configurationFiles.comment"))
            }.bottomGap(BottomGap.MEDIUM)

            row(DetektBundle.message("detekt.configuration.baselineFile.title")) {
                textFieldWithBrowseButton(
                    DetektBundle.message("detekt.configuration.baselineFile.dialog.title"),
                    project,
                    FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor(),
                ).bindText(settings::baselinePath)
                    .horizontalAlign(HorizontalAlign.FILL)
                    .resizableColumn()
            }.bottomGap(BottomGap.MEDIUM)

            row(DetektBundle.message("detekt.configuration.pluginJars.title")) {
                val listModel = FilesListPanel.ListModel(settings.pluginJarPaths.toList().sorted())
                val filesListPanel = FilesListPanel(
                    listModel = listModel,
                    project = project,
                    fileChooserTitle = DetektBundle.message("detekt.configuration.pluginJarFiles.dialog.title"),
                    fileChooserDescription =
                    DetektBundle.message("detekt.configuration.pluginJarFiles.dialog.description")
                ) {
                    settings.pluginJarPaths = it.toMutableSet()
                }.decorated()

                @Suppress("DEPRECATION") // TODO replace with newer overload once the min IJ version is >= 22.1
                cell(filesListPanel, filesListPanel)
                    .horizontalAlign(HorizontalAlign.FILL)
                    .resizableColumn()
                    .comment(DetektBundle.message("detekt.configuration.pluginJars.comment"))
            }
        }
}
