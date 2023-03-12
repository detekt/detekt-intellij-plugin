package io.gitlab.arturbosch.detekt.idea.config.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.ui.dsl.builder.BottomGap
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.MutableProperty
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.RowLayout
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import com.intellij.ui.dsl.gridLayout.VerticalAlign
import com.intellij.ui.layout.selected
import io.gitlab.arturbosch.detekt.idea.DetektBundle
import io.gitlab.arturbosch.detekt.idea.config.DetektPluginSettings
import io.gitlab.arturbosch.detekt.idea.util.toPathsList
import io.gitlab.arturbosch.detekt.idea.util.toVirtualFilesList
import io.gitlab.arturbosch.detekt.idea.util.validateAsFilePath
import java.io.File
import javax.swing.JCheckBox
import javax.swing.JPanel
import kotlin.reflect.KMutableProperty0

internal class DetektConfigUi(
    private val settings: DetektPluginSettings,
    private val project: Project,
) {

    fun createPanel(): DialogPanel = panel {
        backgroundAnalysisGroup()
        rulesGroup()
        filesGroup()
    }

    private fun Panel.backgroundAnalysisGroup() {
        lateinit var detektEnabledCheckbox: JCheckBox

        group(
            title = DetektBundle.message("detekt.configuration.backgroundAnalysisGroup.title"),
            indent = false,
        ) {
            row {
                detektEnabledCheckbox = checkBox(DetektBundle.message("detekt.configuration.enableBackgroundAnalysis"))
                    .applyToComponent { toolTipText = DetektBundle.message("detekt.configuration.enableBackgroundAnalysis.tooltip") }
                    .bindSelected(settings::enableDetekt)
                    .component
            }
            row {
                checkBox(DetektBundle.message("detekt.configuration.treatAsErrors"))
                    .bindSelected(settings::treatAsErrors)
                    .enabledIf(detektEnabledCheckbox.selected)
            }
        }
    }

    private fun Panel.rulesGroup() =
        group(
            title = DetektBundle.message("detekt.configuration.rulesGroup.title"),
            indent = false,
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

    private fun Panel.filesGroup() =
        group(
            title = DetektBundle.message("detekt.configuration.filesGroup.title"),
            indent = false,
        ) {
            configurationFilesRow()

            baselineFileRow()

            pluginJarsRow()
        }

    private fun Panel.configurationFilesRow() {
        row {
            val label = label(DetektBundle.message("detekt.configuration.configurationFiles.title"))
                .verticalAlign(VerticalAlign.TOP)
                .component

            val listModel = FilesListPanel.ListModel()
            val filesListPanel = FilesListPanel(
                listModel = listModel,
                project = project,
                fileChooserTitle = DetektBundle.message("detekt.configuration.configurationFiles.dialog.title"),
                fileChooserDescription =
                DetektBundle.message("detekt.configuration.configurationFiles.dialog.description"),
                descriptorProvider = { FileChooserDescriptorUtil.createYamlChooserDescriptor() }
            ).decorated()

            cell(filesListPanel)
                .horizontalAlign(HorizontalAlign.FILL)
                .resizableColumn()
                .bindItems(settings::configurationFilePaths, listModel)

            label.labelFor = filesListPanel
        }.layout(RowLayout.LABEL_ALIGNED)

        row("") {
            comment(DetektBundle.message("detekt.configuration.configurationFiles.comment"))
        }.bottomGap(BottomGap.MEDIUM)
    }

    private fun Panel.baselineFileRow() {
        row(DetektBundle.message("detekt.configuration.baselineFile.title")) {
            textFieldWithBrowseButton(
                DetektBundle.message("detekt.configuration.baselineFile.dialog.title"),
                project,
                FileChooserDescriptorUtil.createSingleXmlChooserDescriptor()
            )
                .bindText(
                    getter = { LocalFileSystem.getInstance().extractPresentableUrl(settings.baselinePath) },
                    setter = {
                        if (File(it).isFile) {
                            settings.baselinePath = LocalFileSystem.getInstance().findFileByPath(it)?.path.orEmpty()
                        } else {
                            settings.baselinePath = ""
                        }
                    }
                )
                .horizontalAlign(HorizontalAlign.FILL)
                .resizableColumn()
                .validationOnInput { validateAsFilePath(it.text, isWarning = true) }
                .validationOnApply { validateAsFilePath(it.text) }
        }

        row("") {
            comment(DetektBundle.message("detekt.configuration.baselineFile.comment"))
        }.bottomGap(BottomGap.MEDIUM)
    }

    private fun Panel.pluginJarsRow() {
        row {
            val label = label(DetektBundle.message("detekt.configuration.pluginJarFiles.title"))
                .verticalAlign(VerticalAlign.TOP)
                .component

            val listModel = FilesListPanel.ListModel(settings.pluginJarPaths.toVirtualFilesList())
            val filesListPanel = FilesListPanel(
                listModel = listModel,
                project = project,
                fileChooserTitle = DetektBundle.message("detekt.configuration.pluginJarFiles.dialog.title"),
                fileChooserDescription =
                DetektBundle.message("detekt.configuration.pluginJarFiles.dialog.description"),
                descriptorProvider = { FileChooserDescriptorUtil.createJarsChooserDescriptor() }
            ).decorated()

            cell(filesListPanel)
                .horizontalAlign(HorizontalAlign.FILL)
                .resizableColumn()
                .bindItems(settings::pluginJarPaths, listModel)

            label.labelFor = filesListPanel
        }.layout(RowLayout.LABEL_ALIGNED)

        row("") {
            comment(DetektBundle.message("detekt.configuration.pluginJarFiles.comment"))
        }
    }

    private fun Cell<JPanel>.bindItems(
        fileListProperty: KMutableProperty0<List<String>>,
        listModel: FilesListPanel.ListModel,
    ) {
        bind(
            { listModel.items },
            { _, virtualFiles -> listModel.clear(); listModel += virtualFiles },
            MutableProperty(
                { fileListProperty.get().toVirtualFilesList() },
                { fileListProperty.set(it.toPathsList()) }
            )
        )
    }
}
