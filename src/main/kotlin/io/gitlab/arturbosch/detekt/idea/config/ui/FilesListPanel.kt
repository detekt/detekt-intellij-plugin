package io.gitlab.arturbosch.detekt.idea.config.ui

import com.intellij.CommonBundle
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsContexts.DialogTitle
import com.intellij.openapi.util.NlsContexts.Label
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.JBList
import javax.swing.AbstractListModel
import javax.swing.JPanel
import javax.swing.ListSelectionModel

internal class FilesListPanel(
    private val listModel: ListModel,
    private val project: Project,
    @DialogTitle private val fileChooserTitle: String,
    @Label private val fileChooserDescription: String,
    private val onDataChanged: (newData: List<String>) -> Unit
) {

    private val list = JBList(listModel).apply {
        dragEnabled = false
        selectionModel.selectionMode = ListSelectionModel.MULTIPLE_INTERVAL_SELECTION
    }

    fun decorated(): JPanel =
        ToolbarDecorator.createDecorator(list)
            .setAddAction {
                onAddFileClick(listModel) { onDataChanged(it) }
            }
            .setRemoveAction {
                onRemoveFileClick(listModel) { onDataChanged(it) }
            }
            .setButtonComparator(CommonBundle.message("button.add"), CommonBundle.message("button.remove"))
            .createPanel()

    private fun onRemoveFileClick(
        listModel: ListModel,
        onDataChanged: (List<String>) -> Unit
    ) {
        val changed = listModel.removeAt(list.selectedIndices.toList()).isNotEmpty()
        if (changed) onDataChanged(listModel.items)
    }

    private fun onAddFileClick(
        listModel: ListModel,
        onDataChanged: (List<String>) -> Unit
    ) {
        val descriptor = FileChooserDescriptorFactory.createMultipleFilesNoJarsDescriptor()
        descriptor.title = fileChooserTitle
        descriptor.description = fileChooserDescription

        val files = FileChooser.chooseFiles(descriptor, list, project, null)
        var changed = false
        for (file in files) {
            if (file != null) {
                listModel += file.path
                changed = true
            }
        }
        if (changed) onDataChanged(listModel.items)
    }

    @Suppress("TooManyFunctions") // Required functionality
    class ListModel(initialItems: List<String> = emptyList()) : AbstractListModel<String>() {

        private val _items: MutableList<String> = initialItems.toMutableList()

        val items: List<String> = _items

        override fun getSize() = items.size

        override fun getElementAt(index: Int) = _items[index]

        operator fun get(index: Int) = _items[index]

        operator fun set(index: Int, value: String) {
            _items[index] = value
            fireContentsChanged(this, index, index)
        }

        operator fun plusAssign(newItem: String) {
            add(newItem = newItem)
        }

        operator fun plusAssign(newItems: Collection<String>) {
            addAll(newItems = newItems)
        }

        fun add(index: Int = _items.size - 1, newItem: String) {
            _items.add(index, newItem)
            fireIntervalAdded(this, index, index)
        }

        fun addAll(index: Int = _items.lastIndex + 1, newItems: Collection<String>) {
            if (newItems.isEmpty()) return
            _items.addAll(index, newItems)
            fireIntervalAdded(this, index, index + newItems.size - 1)
        }

        fun removeRange(range: IntRange): List<String> {
            if (range.isEmpty()) return emptyList()
            val removed = range.reversed()
                .map { indexToRemove -> _items.removeAt(indexToRemove) }
            fireIntervalRemoved(this, range.first, range.last)
            return removed
        }

        fun removeAt(indices: Collection<Int>): List<String> {
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
            val formerLastIndex = _items.lastIndex
            _items.clear()
            fireIntervalRemoved(this, 0, formerLastIndex)
        }
    }
}
