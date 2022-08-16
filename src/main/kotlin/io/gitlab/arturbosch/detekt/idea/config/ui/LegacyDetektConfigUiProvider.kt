package io.gitlab.arturbosch.detekt.idea.config.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.ui.layout.CellBuilder
import com.intellij.ui.layout.ComponentPredicate
import com.intellij.ui.layout.LayoutBuilder
import com.intellij.ui.layout.PropertyBinding
import com.intellij.ui.layout.Row
import com.intellij.ui.layout.panel
import com.intellij.ui.layout.selected
import io.gitlab.arturbosch.detekt.idea.DetektBundle
import io.gitlab.arturbosch.detekt.idea.config.DetektPluginSettings
import io.gitlab.arturbosch.detekt.idea.util.toPathsSet
import io.gitlab.arturbosch.detekt.idea.util.toVirtualFilesList
import io.gitlab.arturbosch.detekt.idea.util.validateAsFilePath
import java.io.File
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JLabel
import kotlin.reflect.KMutableProperty0

@Suppress("DialogTitleCapitalization") // It gets tripped up by the capitalization of Detekt's name
class LegacyDetektConfigUiProvider(
    private val settings: DetektPluginSettings,
    private val project: Project
) : DetektConfigUiProvider {

    override fun createPanel(): DialogPanel = panel {
        lateinit var detektEnabledCheckbox: JCheckBox
        row {
            detektEnabledCheckbox =
                checkBox(DetektBundle.message("detekt.configuration.enableDetekt"), settings::enableDetekt)
                    .component
        }
        row {
            checkBox(DetektBundle.message("detekt.configuration.treatAsErrors"), settings::treatAsErrors)
                .enableIf(detektEnabledCheckbox.selected)
        }

        rulesGroup(detektEnabledCheckbox.selected)
        filesGroup(detektEnabledCheckbox.selected)
    }

    private fun LayoutBuilder.rulesGroup(enabled: ComponentPredicate) =
        titledRow(title = DetektBundle.message("detekt.configuration.rulesGroup.title")) {
            row {
                checkBox(
                    DetektBundle.message("detekt.configuration.buildUponDefaultConfig"),
                    settings::buildUponDefaultConfig
                ).enableIf(enabled)
            }
            row {
                checkBox(DetektBundle.message("detekt.configuration.enableFormattingRules"), settings::enableFormatting)
                    .enableIf(enabled)
            }
            row {
                checkBox(DetektBundle.message("detekt.configuration.enableAllRules"), settings::enableAllRules)
                    .enableIf(enabled)
            }
        }

    private fun LayoutBuilder.filesGroup(enabled: ComponentPredicate) =
        titledRow(title = DetektBundle.message("detekt.configuration.filesGroup.title")) {
            configurationFilesRow(enabled)

            baselineFileRow(enabled)

            pluginJarFilesRow(enabled)
        }

    private fun Row.configurationFilesRow(enabled: ComponentPredicate) {
        row {
            label(DetektBundle.message("detekt.configuration.configurationFiles.title"))
                .constraints(growY)
                .textAlignment(JLabel.TOP)

            val listModel = FilesListPanel.ListModel(settings.configurationFilePaths.toVirtualFilesList())
            val filesListPanel = FilesListPanel(
                listModel = listModel,
                project = project,
                fileChooserTitle = DetektBundle.message("detekt.configuration.configurationFiles.dialog.title"),
                fileChooserDescription =
                DetektBundle.message("detekt.configuration.configurationFiles.dialog.description"),
                descriptorProvider = { FileChooserDescriptorUtil.createYamlChooserDescriptor() }
            )
                .decorated()
                .hackEnabledIf(enabled)

            filesListPanel()
                .constraints(growX)
                .enableIf(enabled)
                .bindItems(settings::configurationFilePaths, listModel)
        }

        row("") {
            comment(DetektBundle.message("detekt.configuration.configurationFiles.comment"))
        }.largeGapAfter()
    }

    private fun Row.baselineFileRow(enabled: ComponentPredicate) {
        row(DetektBundle.message("detekt.configuration.baselineFile.title")) {
            textFieldWithBrowseButton(
                getter = { LocalFileSystem.getInstance().extractPresentableUrl(settings.baselinePath) },
                setter = {
                    if (File(it).isFile) {
                        settings.baselinePath = LocalFileSystem.getInstance().findFileByPath(it)?.path.orEmpty()
                    } else {
                        settings.baselinePath = ""
                    }
                },
                DetektBundle.message("detekt.configuration.baselineFile.dialog.title"),
                project,
                FileChooserDescriptorUtil.createSingleXmlChooserDescriptor(),
            )
                .enableIf(enabled)
                .withValidationOnInput { validateAsFilePath(it.text, isWarning = true) }
                .withValidationOnApply { validateAsFilePath(it.text) }
        }

        row("") {
            comment(DetektBundle.message("detekt.configuration.baselineFile.comment"))
        }.largeGapAfter()
    }

    private fun Row.pluginJarFilesRow(enabled: ComponentPredicate) {
        row {
            label(DetektBundle.message("detekt.configuration.pluginJarFiles.title"))
                .constraints(growY)
                .textAlignment(JLabel.TOP)

            val listModel = FilesListPanel.ListModel(settings.pluginJarPaths.toVirtualFilesList())
            val filesListPanel = FilesListPanel(
                listModel = listModel,
                project = project,
                fileChooserTitle = DetektBundle.message("detekt.configuration.pluginJarFiles.dialog.title"),
                fileChooserDescription =
                DetektBundle.message("detekt.configuration.pluginJarFiles.dialog.description"),
                descriptorProvider = { FileChooserDescriptorUtil.createJarsChooserDescriptor() }
            )
                .decorated()
                .hackEnabledIf(enabled)

            filesListPanel()
                .constraints(growX)
                .enableIf(enabled)
                .bindItems(settings::pluginJarPaths, listModel)
        }

        row("") {
            comment(DetektBundle.message("detekt.configuration.pluginJarFiles.comment"))
        }
    }

    private fun CellBuilder<JLabel>.textAlignment(alignment: Int) = run {
        component.apply {
            verticalTextPosition = alignment
            verticalAlignment = alignment
        }
    }

    // This is a hack to compensate for the fact that in older IJ versions, the enabled flag
    // wasn't correctly carried over to child components (at least when testing on 2020.3)
    private fun JComponent.hackEnabledIf(enabled: ComponentPredicate) = apply {
        fun JComponent.setChildrenEnabled(newEnabled: Boolean) {
            components.forEach { child ->
                child.isEnabled = newEnabled
                (child as? JComponent)?.setChildrenEnabled(newEnabled)
            }
        }

        val isEnabledNow = enabled()
        setChildrenEnabled(isEnabledNow)

        enabled.addListener { newEnabled -> setChildrenEnabled(newEnabled) }
    }

    private fun CellBuilder<JComponent>.bindItems(
        fileSetProperty: KMutableProperty0<MutableSet<String>>,
        listModel: FilesListPanel.ListModel
    ) {
        withBinding(
            { listModel.items },
            { _, virtualFiles -> listModel.clear(); listModel += virtualFiles },
            PropertyBinding(
                { fileSetProperty.get().toVirtualFilesList() },
                { fileSetProperty.set(it.toPathsSet().toMutableSet()) }
            )
        )
    }
}
