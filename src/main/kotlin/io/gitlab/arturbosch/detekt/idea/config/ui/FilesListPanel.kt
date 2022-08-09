package io.gitlab.arturbosch.detekt.idea.config.ui

import com.intellij.CommonBundle
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsContexts.DialogTitle
import com.intellij.openapi.util.NlsContexts.Label
import com.intellij.openapi.vcs.changes.ui.VirtualFileListCellRenderer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.JBList
import javax.swing.AbstractListModel
import javax.swing.JPanel
import javax.swing.ListSelectionModel

@Suppress("UnstableApiUsage") // For NlsContexts annotations
internal class FilesListPanel(
    private val listModel: ListModel,
    private val project: Project,
    @DialogTitle private val fileChooserTitle: String,
    @Label private val fileChooserDescription: String,
    private val descriptorProvider: () -> FileChooserDescriptor
) {

    private val list = JBList(listModel).apply {
        dragEnabled = false
        selectionModel.selectionMode = ListSelectionModel.MULTIPLE_INTERVAL_SELECTION
        cellRenderer = VirtualFileListCellRenderer(project)
    }

    fun decorated(): JPanel =
        ToolbarDecorator.createDecorator(list)
            .setAddAction {
                onAddFileClick(listModel) { TODO() }
            }
            .setRemoveAction {
                onRemoveFileClick(listModel) { TODO() }
            }
            .setButtonComparator(CommonBundle.message("button.add"), CommonBundle.message("button.remove"))
            .disableUpDownActions()
            .createPanel()

    private fun onRemoveFileClick(
        listModel: ListModel,
        onDataChanged: (List<VirtualFile>) -> Unit
    ) {
        val changed = listModel.removeAt(list.selectedIndices.toList()).isNotEmpty()
        if (changed) onDataChanged(listModel.items)
    }

    private fun onAddFileClick(
        listModel: ListModel,
        onDataChanged: (List<VirtualFile>) -> Unit
    ) {
        val descriptor = descriptorProvider()
        descriptor.title = fileChooserTitle
        descriptor.description = fileChooserDescription

        val files = FileChooser.chooseFiles(descriptor, list, project, null)
        var changed = false
        for (file in files) {
            if (file != null && !listModel.items.contains(file)) {
                listModel += file
                changed = true
            }
        }
        if (changed) onDataChanged(listModel.items)
    }

    @Suppress("TooManyFunctions") // Required functionality
    class ListModel(initialItems: List<VirtualFile> = emptyList()) : AbstractListModel<VirtualFile>() {

        private val _items: MutableList<VirtualFile> = initialItems.toMutableList()

        val items: List<VirtualFile> = _items

        override fun getSize() = items.size

        override fun getElementAt(index: Int) = _items[index]

        operator fun get(index: Int) = _items[index]

        operator fun set(index: Int, value: VirtualFile) {
            _items[index] = value
            fireContentsChanged(this, index, index)
        }

        operator fun plusAssign(newItem: VirtualFile) {
            add(newItem)
        }

        operator fun plusAssign(newItems: Collection<VirtualFile>) {
            addAll(newItems)
        }

        fun add(newItem: VirtualFile, index: Int = _items.size) {
            _items.add(index, newItem)
            fireIntervalAdded(this, index, index)
        }

        fun addAll(newItems: Collection<VirtualFile>, index: Int = _items.size) {
            if (newItems.isEmpty()) return
            _items.addAll(index, newItems)
            fireIntervalAdded(this, index, index + newItems.size - 1)
        }

        fun removeAt(indices: Collection<Int>): List<VirtualFile> {
            if (indices.isEmpty()) return emptyList()
            val removed = indices.reversed()
                .map { indexToRemove ->
                    val removed = _items.removeAt(indexToRemove)
                    fireIntervalRemoved(this, indexToRemove, indexToRemove)
                    removed
                }
            return removed
        }

        fun clear() {
            val formerSize = items.size
            _items.clear()
            fireIntervalRemoved(this, 0, formerSize)
        }
    }
}
