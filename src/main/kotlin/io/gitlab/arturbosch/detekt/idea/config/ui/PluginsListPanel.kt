package io.gitlab.arturbosch.detekt.idea.config.ui

import com.intellij.CommonBundle
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.JBList
import icons.MavenIcons
import io.gitlab.arturbosch.detekt.idea.DetektBundle
import io.gitlab.arturbosch.detekt.idea.util.DetektPlugin
import java.awt.GraphicsEnvironment
import javax.swing.DefaultListModel
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.ListSelectionModel

internal sealed interface PluginSpec {
    data class Jar(val file: VirtualFile) : PluginSpec

    data class Maven(val plug: DetektPlugin) : PluginSpec
}

internal class PluginsListPanel(private val listModel: PluginListModel, private val project: Project) {

    private val list =
        JBList(listModel).apply {
            if (!GraphicsEnvironment.isHeadless()) {
                dragEnabled = true
            }
            selectionModel.selectionMode = ListSelectionModel.MULTIPLE_INTERVAL_SELECTION
            cellRenderer = Renderer()
        }

    fun decorated(): JPanel =
        ToolbarDecorator.createDecorator(list)
            .setAddAction { onAddJarClick() }
            .setAddActionName(DetektBundle.message("detekt.configuration.plugins.dialog.title.jar"))
            .addExtraAction(
                object : DumbAwareAction(
                    DetektBundle.message("detekt.configuration.mavenSearch.title"),
                    null,
                    MavenIcons.ToolWindowMaven,
                ) {
                    override fun actionPerformed(e: AnActionEvent) {
                        onAddMavenClick()
                    }
                }
            )
            .setRemoveAction { onRemoveClick() }
            .setButtonComparator(
                DetektBundle.message("detekt.configuration.plugins.dialog.title.jar"),
                DetektBundle.message("detekt.configuration.mavenSearch.title"),
                CommonBundle.message("button.remove"),
            )
            .createPanel()

    private fun onRemoveClick() {
        listModel.removeAt(list.selectedIndices.toList())
    }

    private fun onAddJarClick() {
        val descriptor = FileChooserDescriptorUtil.createJarsChooserDescriptor()
        descriptor.title = DetektBundle.message("detekt.configuration.plugins.dialog.title.jar")
        descriptor.description = DetektBundle.message("detekt.configuration.plugins.dialog.description.jar")

        val files = FileChooser.chooseFiles(descriptor, list, project, null)
        for (file in files) {
            val spec = PluginSpec.Jar(file)
            if (file != null && !listModel.items.contains(spec)) {
                listModel += spec
            }
        }
    }

    private fun onAddMavenClick() {
        val dialog = MavenSearchDialog(project)
        if (dialog.showAndGet()) {
            val result = dialog.getResult()
            if (result != null) {
                val spec = PluginSpec.Maven(result)
                if (!listModel.items.contains(spec)) {
                    listModel += spec
                }
            }
        }
    }

    class PluginListModel(initialItems: List<PluginSpec> = emptyList()) : DefaultListModel<PluginSpec>() {

        val items: List<PluginSpec>
            get() = super.elements().toList()

        init {
            addAll(initialItems)
        }

        operator fun plusAssign(newItem: PluginSpec) {
            addElement(newItem)
        }

        operator fun plusAssign(newItems: Collection<PluginSpec>) {
            addAll(newItems)
        }

        fun removeAt(indices: Collection<Int>) {
            if (indices.isEmpty()) return
            indices.sortedDescending().forEach { indexToRemove -> remove(indexToRemove) }
        }
    }

    private class Renderer : ColoredListCellRenderer<PluginSpec>() {
        override fun customizeCellRenderer(
            list: JList<out PluginSpec>,
            value: PluginSpec,
            index: Int,
            selected: Boolean,
            hasFocus: Boolean,
        ) {
            when (value) {
                is PluginSpec.Jar -> {
                    icon = value.file.fileType.icon
                    append(value.file.name, SimpleTextAttributes.REGULAR_ATTRIBUTES)
                    append(" (${value.file.parent.path})", SimpleTextAttributes.GRAYED_ATTRIBUTES)
                }

                is PluginSpec.Maven -> {
                    icon = AllIcons.Nodes.PpLib
                    append(value.plug.coordinate, SimpleTextAttributes.REGULAR_ATTRIBUTES)
                }
            }
        }
    }
}
