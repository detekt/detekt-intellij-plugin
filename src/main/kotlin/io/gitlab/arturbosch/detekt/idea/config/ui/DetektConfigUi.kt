package io.gitlab.arturbosch.detekt.idea.config.ui

import com.intellij.openapi.observable.properties.AtomicBooleanProperty
import com.intellij.openapi.observable.properties.ObservableProperty
import com.intellij.openapi.observable.util.bindEnabled
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.AlignY
import com.intellij.ui.dsl.builder.BottomGap
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.MutableProperty
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.RowLayout
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.layout.selected
import io.gitlab.arturbosch.detekt.idea.DetektBundle
import io.gitlab.arturbosch.detekt.idea.config.DetektPluginSettings
import io.gitlab.arturbosch.detekt.idea.util.DetektPlugin
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
        lateinit var detektEnabledCheckbox: JCheckBox
        row {
            detektEnabledCheckbox = checkBox(DetektBundle.message("detekt.configuration.enableBackgroundAnalysis"))
                .bindSelected(settings::enableDetekt)
                .comment(DetektBundle.message("detekt.configuration.enableBackgroundAnalysis.tooltip"))
                .component
        }

        pluginOptionsGroup().enabledIf(detektEnabledCheckbox.selected)

        rulesGroup().enabledIf(detektEnabledCheckbox.selected)

        // Passing the "selected" state down shouldn't be necessary, but it looks like
        // the DSL doesn't pass down the enabled state to custom components (at least
        // as of today's lowest supported IDE version, 221.6008.13)
        filesGroup(detektEnabledCheckbox.selectedProperty)
            .enabledIf(detektEnabledCheckbox.selected)
    }

    private fun Panel.pluginOptionsGroup() =
        group(
            title = DetektBundle.message("detekt.configuration.pluginGroup.title"),
            indent = false,
        ) {
            row {
                checkBox(DetektBundle.message("detekt.configuration.treatAsErrors"))
                    .bindSelected(settings::treatAsErrors)
            }

            lateinit var redirectCheckbox: JCheckBox
            row {
                redirectCheckbox = checkBox(DetektBundle.message("detekt.configuration.redirect"))
                    .bindSelected(settings::redirectChannels)
                    .component
            }
            indent {
                row {
                    checkBox(DetektBundle.message("detekt.configuration.debug"))
                        .bindSelected(settings::debug)
                        .enabledIf(redirectCheckbox.selected)
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

    private fun Panel.filesGroup(isEnabled: ObservableProperty<Boolean>) =
        group(
            title = DetektBundle.message("detekt.configuration.filesGroup.title"),
            indent = false,
        ) {
            configurationFilesRow(isEnabled)

            baselineFileRow()

            pluginsRow(isEnabled)
        }

    private fun Panel.configurationFilesRow(isEnabled: ObservableProperty<Boolean>) {
        row {
            val label = label(DetektBundle.message("detekt.configuration.configurationFiles.title"))
                .align(AlignY.TOP)
                .component

            val listModel = FilesListPanel.ListModel()
            val filesListPanel = FilesListPanel(
                listModel = listModel,
                project = project,
                fileChooserTitle = DetektBundle.message("detekt.configuration.configurationFiles.dialog.title"),
                fileChooserDescription =
                    DetektBundle.message("detekt.configuration.configurationFiles.dialog.description"),
                descriptorProvider = { FileChooserDescriptorUtil.createYamlChooserDescriptor() },
            ).decorated()
                .bindEnabled(isEnabled)

            cell(filesListPanel)
                .align(AlignX.FILL)
                .resizableColumn()
                .bindConfigFiles(settings::configurationFilePaths, listModel)

            label.labelFor = filesListPanel
        }.layout(RowLayout.LABEL_ALIGNED)

        row("") {
            comment(DetektBundle.message("detekt.configuration.configurationFiles.comment"))
        }.bottomGap(BottomGap.MEDIUM)
    }

    private fun Cell<JPanel>.bindConfigFiles(
        fileListProperty: KMutableProperty0<List<String>>,
        listModel: FilesListPanel.ListModel,
    ) {
        bind(
            { listModel.items },
            { _, virtualFiles ->
                listModel.clear()
                listModel += virtualFiles
            },
            MutableProperty(
                getter = { fileListProperty.get().toVirtualFilesList() },
                setter = { fileListProperty.set(it.toPathsList()) },
            ),
        )
    }

    private fun Panel.baselineFileRow() {
        row(DetektBundle.message("detekt.configuration.baselineFile.title")) {
            textFieldWithBrowseButton(
                DetektBundle.message("detekt.configuration.baselineFile.dialog.title"),
                project,
                FileChooserDescriptorUtil.createSingleXmlChooserDescriptor(),
            )
                .bindText(
                    getter = { LocalFileSystem.getInstance().extractPresentableUrl(settings.baselinePath) },
                    setter = {
                        if (File(it).isFile) {
                            settings.baselinePath = LocalFileSystem.getInstance().findFileByPath(it)?.path.orEmpty()
                        } else {
                            settings.baselinePath = ""
                        }
                    },
                )
                .align(AlignX.FILL)
                .resizableColumn()
                .validationOnInput { validateAsFilePath(it.text, isWarning = true) }
                .validationOnApply { validateAsFilePath(it.text) }
        }

        row("") {
            comment(DetektBundle.message("detekt.configuration.baselineFile.comment"))
        }.bottomGap(BottomGap.MEDIUM)
    }

    private fun Panel.pluginsRow(isEnabled: ObservableProperty<Boolean>) {
        row {
            val label = label(DetektBundle.message("detekt.configuration.plugins.title"))
                .align(AlignY.TOP)
                .component

            val pluginJars = settings.pluginJarPaths.toVirtualFilesList()
                .map { PluginSpec.Jar(it) }
            val mavenPlugins = settings.plugins.map { PluginSpec.Maven(it) }
            val initialItems = pluginJars + mavenPlugins

            val listModel = PluginsListPanel.PluginListModel(initialItems)
            val pluginsListPanel = PluginsListPanel(listModel = listModel, project = project)
                .decorated()
                .bindEnabled(isEnabled)

            cell(pluginsListPanel)
                .align(AlignX.FILL)
                .resizableColumn()
                .bindPlugins(settings::pluginJarPaths, settings::plugins, listModel)

            label.labelFor = pluginsListPanel
        }.layout(RowLayout.LABEL_ALIGNED)

        row("") {
            comment(DetektBundle.message("detekt.configuration.plugins.comment"))
        }
    }

    private fun Cell<JPanel>.bindPlugins(
        jarPathsProperty: KMutableProperty0<List<String>>,
        pluginsProperty: KMutableProperty0<List<DetektPlugin>>,
        listModel: PluginsListPanel.PluginListModel,
    ) {
        bind(
            componentGet = { listModel.items },
            componentSet = { _, items ->
                listModel.clear()
                listModel += items
            },
            prop = MutableProperty(
                getter = {
                    val jars = jarPathsProperty.get()
                        .toVirtualFilesList()
                        .map { PluginSpec.Jar(it) }
                    val mavenPlugins = pluginsProperty.get()
                        .map { PluginSpec.Maven(it) }

                    jars + mavenPlugins
                },
                setter = { items ->
                    val jars = items.filterIsInstance<PluginSpec.Jar>()
                        .map { it.file }
                        .toPathsList()
                    val mavenPlugin = items.filterIsInstance<PluginSpec.Maven>()
                        .map { it.plug }

                    jarPathsProperty.set(jars)
                    pluginsProperty.set(mavenPlugin)
                },
            ),
        )
    }

    private val JCheckBox.selectedProperty: ObservableProperty<Boolean>
        get() {
            val prop = AtomicBooleanProperty(isSelected)
            addChangeListener { prop.set(isSelected) }
            return prop
        }
}
