package io.gitlab.arturbosch.detekt.idea.config

import com.intellij.CommonBundle
import com.intellij.codeInspection.javaDoc.JavadocUIUtil.bindCheckbox
import com.intellij.openapi.components.service
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.BoundSearchableConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.JBList
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import io.gitlab.arturbosch.detekt.idea.DetektBundle
import io.gitlab.arturbosch.detekt.idea.util.PluginUtils
import javax.swing.DefaultListModel
import javax.swing.ListSelectionModel

class DetektConfig(private val project: Project) : BoundSearchableConfigurable(
    displayName = DetektBundle.message("detekt.configuration.title"),
    helpTopic = DetektBundle.message("detekt.configuration.title"),
    _id = "io.github.detekt.config",
) {

    private val settings = project.service<DetektPluginSettings>()

    // TODO Switch to new group() overload once the minimum IJ version is >= 221.3427.89
    @Suppress("UnstableApiUsage", "LongMethod", "DialogTitleCapitalization")
    override fun createPanel(): DialogPanel = panel {
        row {
            checkBox(DetektBundle.message("detekt.configuration.enableDetekt"))
                .bindCheckbox(settings::enableDetekt)
        }
        row {
            checkBox(DetektBundle.message("detekt.configuration.treatAsErrors"))
                .bindCheckbox(settings::treatAsErrors)
        }

        group(title = DetektBundle.message("detekt.configuration.rulesGroup.title"), indent = false, topGroupGap = false) {
            row {
                checkBox(DetektBundle.message("detekt.configuration.buildUponDefaultConfig"))
                    .bindCheckbox(settings::buildUponDefaultConfig)

            }
            row {
                checkBox(DetektBundle.message("detekt.configuration.enableFormattingRules"))
                    .bindCheckbox(settings::enableFormatting)
            }
            row {
                checkBox(DetektBundle.message("detekt.configuration.enableAllRules"))
                    .bindCheckbox(settings::enableAllRules)
            }
        }

        val supportsComments = PluginUtils.isAtLeastIJBuild("213.6461.79")
        group(title = DetektBundle.message("detekt.configuration.filesGroup.title"), indent = false, topGroupGap = false) {
            row(DetektBundle.message("detekt.configuration.configurationFiles.title")) {
                val filesList = FilesList()
                val listModel = DefaultListModel<String>()
                filesList.model = listModel

                val decoratedListPanel = ToolbarDecorator.createDecorator(filesList)
                    .setAddAction {
                        val descriptor = FileChooserDescriptorFactory.createMultipleFilesNoJarsDescriptor()
                        descriptor.title = DetektBundle.message("detekt.configuration.configurationFiles.dialog.title")
                        descriptor.description = DetektBundle.message("detekt.configuration.configurationFiles.dialog.description")

                        val files = FileChooser.chooseFiles(descriptor, filesList, project, null)
                        var changes = false
                        for (file in files) {
                            if (file != null) {
                                listModel.add(listModel.size, file.path)
                                changes = true
                            }
                        }
                        if (changes) onDataChanged()
                    }
                    .setRemoveAction {
                        val changed = filesList.selectedIndices.isNotEmpty()
                        filesList.selectedIndices.copyOf().reversed()
                            .forEach { listModel.remove(it) }
                        if (changed) onDataChanged()
                    }
                    .setButtonComparator(CommonBundle.message("button.add"), CommonBundle.message("button.remove"))
                    .createPanel()

                cell(decoratedListPanel, decoratedListPanel)

//                textFieldWithBrowseButton(
//                    DetektBundle.message("detekt.configuration.configurationFiles.dialog.title"),
//                    project,
//                    FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor(),
//                )
//                    .bindItem(settings::configPath)
//                    .apply {
//                        if (supportsComments) {
//                            @Suppress("MissingRecentApi")
//                            comment(DetektBundle.message("detekt.configuration.configurationFiles.comment"))
//                        }
//                    }
            }

            row(DetektBundle.message("detekt.configuration.baselineFile.title")) {
                textFieldWithBrowseButton(
                    DetektBundle.message("detekt.configuration.baselineFile.dialog.title"),
                    project,
                    FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor(),
                ).bindText(settings::baselinePath)
            }

            row(DetektBundle.message("detekt.configuration.pluginJars.title")) {
                textFieldWithBrowseButton(
                    DetektBundle.message("detekt.configuration.pluginJars.dialog.title"),
                    project,
                    FileChooserDescriptorFactory.createAllButJarContentsDescriptor(),
                )
                    //.bindText(settings::pluginPaths)
                    .apply {
                        if (supportsComments) {
                            @Suppress("MissingRecentApi")
                            comment(DetektBundle.message("detekt.configuration.pluginJars.comment"))
                        }
                    }
            }
        }
    }

    private fun onDataChanged() {
        // TODO()
    }

    override fun isModified(): Boolean = false
}

private class FilesList : JBList<String>() {
    init {
        dragEnabled = false
        selectionModel.selectionMode = ListSelectionModel.MULTIPLE_INTERVAL_SELECTION
    }
}
