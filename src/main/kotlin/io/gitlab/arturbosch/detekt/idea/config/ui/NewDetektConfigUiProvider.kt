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
import com.intellij.ui.layout.ComponentPredicate
import com.intellij.ui.layout.selected
import io.gitlab.arturbosch.detekt.idea.DetektBundle
import io.gitlab.arturbosch.detekt.idea.config.DetektPluginSettings
import io.gitlab.arturbosch.detekt.idea.util.toPathsSet
import io.gitlab.arturbosch.detekt.idea.util.toVirtualFilesList
import javax.swing.JCheckBox
import javax.swing.JPanel
import kotlin.reflect.KMutableProperty0

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

        rulesGroup(detektEnabledCheckbox.selected)

        filesGroup(detektEnabledCheckbox.selected)
    }

    // TODO replace with newer overload once the min IJ version is >= 22.1
    // MissingRecentApi: this is only meant to be used on IJ 21.3 and later
    @Suppress("UnstableApiUsage", "MissingRecentApi", "Deprecation")
    private fun Panel.rulesGroup(enabled: ComponentPredicate) =
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
        }.enabledIf(enabled)

    // TODO replace with newer overload once the min IJ version is >= 22.1
    // MissingRecentApi: this is only meant to be used on IJ 21.3 and later
    // UnstableApiUsage: some calls have a newer overload in IJ 22.1+
    @Suppress("UnstableApiUsage", "MissingRecentApi", "Deprecation")
    private fun Panel.filesGroup(enabled: ComponentPredicate) =
        group(
            title = DetektBundle.message("detekt.configuration.filesGroup.title"),
            indent = false,
            topGroupGap = false
        ) {
            configurationFilesRow(enabled)

            baselineFileRow(enabled)

            pluginJarsRow(enabled)
        }

    // MissingRecentApi: this is only meant to be used on IJ 21.3 and later
    // UnstableApiUsage: some calls have a newer overload in IJ 22.1 and later
    @Suppress("MissingRecentApi", "UnstableApiUsage")
    private fun Panel.configurationFilesRow(enabled: ComponentPredicate) {
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

            @Suppress("DEPRECATION") // TODO replace with newer overload once the min IJ version is >= 22.1
            cell(filesListPanel, filesListPanel)
                .horizontalAlign(HorizontalAlign.FILL)
                .resizableColumn()
                .enabledIf(enabled)
                .bindItems(settings::configurationFilePaths, listModel)

            label.labelFor = filesListPanel
        }.layout(RowLayout.LABEL_ALIGNED)

        row("") {
            comment(DetektBundle.message("detekt.configuration.configurationFiles.comment"))
        }.bottomGap(BottomGap.MEDIUM)
    }

    // MissingRecentApi: this is only meant to be used on IJ 21.3 and later
    // UnstableApiUsage: some calls have a newer overload in IJ 22.1 and later
    @Suppress("MissingRecentApi", "UnstableApiUsage")
    private fun Panel.baselineFileRow(enabled: ComponentPredicate) {
        row(DetektBundle.message("detekt.configuration.baselineFile.title")) {
            textFieldWithBrowseButton(
                DetektBundle.message("detekt.configuration.baselineFile.dialog.title"),
                project,
                FileChooserDescriptorUtil.createSingleXmlChooserDescriptor()
            )
                .bindText(
                    getter = { LocalFileSystem.getInstance().extractPresentableUrl(settings.baselinePath) },
                    setter = {
                        settings.baselinePath = LocalFileSystem.getInstance().findFileByPath(it)?.path.orEmpty()
                    }
                )
                .horizontalAlign(HorizontalAlign.FILL)
                .resizableColumn()
                .enabledIf(enabled)
        }

        row("") {
            comment(DetektBundle.message("detekt.configuration.baselineFile.comment"))
        }.bottomGap(BottomGap.MEDIUM)
    }

    // MissingRecentApi: this is only meant to be used on IJ 21.3 and later
    // UnstableApiUsage: some calls have a newer overload in IJ 22.1 and later
    @Suppress("MissingRecentApi", "UnstableApiUsage")
    private fun Panel.pluginJarsRow(enabled: ComponentPredicate) {
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

            @Suppress("DEPRECATION") // TODO replace with newer overload once the min IJ version is >= 22.1
            cell(filesListPanel, filesListPanel)
                .horizontalAlign(HorizontalAlign.FILL)
                .resizableColumn()
                .enabledIf(enabled)
                .bindItems(settings::pluginJarPaths, listModel)

            label.labelFor = filesListPanel
        }.layout(RowLayout.LABEL_ALIGNED)

        row("") {
            comment(DetektBundle.message("detekt.configuration.pluginJarFiles.comment"))
        }
    }

    // MissingRecentApi: this is only meant to be used on IJ 21.3 and later
    @Suppress("MissingRecentApi")
    private fun Cell<JPanel>.bindItems(
        fileSetProperty: KMutableProperty0<MutableSet<String>>,
        listModel: FilesListPanel.ListModel
    ) {
        bind(
            { listModel.items },
            { _, virtualFiles -> listModel.clear(); listModel += virtualFiles },
            MutableProperty(
                { fileSetProperty.get().toVirtualFilesList() },
                { fileSetProperty.set(it.toPathsSet().toMutableSet()) }
            )
        )
    }
}
