package io.gitlab.arturbosch.detekt.idea.config.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.ui.layout.*
import io.gitlab.arturbosch.detekt.idea.DetektBundle
import io.gitlab.arturbosch.detekt.idea.config.DetektPluginSettings
import io.gitlab.arturbosch.detekt.idea.util.toVirtualFilesList
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JLabel

@Suppress("DialogTitleCapitalization") // It gets tripped up by the capitalization of Detekt's name
class LegacyDetektConfigUiProvider(
    private val settings: DetektPluginSettings,
    private val project: Project
) : DetektConfigUiProvider {

    @Suppress("DEPRECATION") // This legacy UI is only used on IJ < 21.3 where the v2 DSL is unavailable
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
            ) { virtualFiles ->
                settings.configurationFilePaths = virtualFiles.map { it.path }.toMutableSet()
            }
                .decorated()
                .hackEnabledIf(enabled)

            filesListPanel()
                .constraints(growX)
                .enableIf(enabled)
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
                    settings.baselinePath = LocalFileSystem.getInstance().findFileByPath(it)?.path.orEmpty()
                },
                DetektBundle.message("detekt.configuration.baselineFile.dialog.title"),
                project,
                FileChooserDescriptorUtil.createSingleXmlChooserDescriptor(),
            )
                .enableIf(enabled)
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
            ) { virtualFiles ->
                settings.pluginJarPaths = virtualFiles.map { it.path }.toMutableSet()
            }
                .decorated()
                .hackEnabledIf(enabled)

            filesListPanel()
                .constraints(growX)
                .enableIf(enabled)
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
}
