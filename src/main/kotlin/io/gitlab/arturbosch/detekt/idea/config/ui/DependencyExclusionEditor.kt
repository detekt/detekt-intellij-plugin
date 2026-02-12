package io.gitlab.arturbosch.detekt.idea.config.ui

import com.intellij.ide.JavaUiBundle
import com.intellij.openapi.ui.DialogBuilder
import com.intellij.ui.CheckboxTree
import com.intellij.ui.CheckboxTreeBase
import com.intellij.ui.CheckboxTreeListener
import com.intellij.ui.CheckedTreeNode
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.TreeUIHelper
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.NamedColorUtil
import com.intellij.util.ui.tree.TreeUtil
import io.gitlab.arturbosch.detekt.idea.DetektBundle
import io.gitlab.arturbosch.detekt.idea.config.logic.DependencySelectionHandler
import io.gitlab.arturbosch.detekt.idea.config.logic.SyncableNode
import io.gitlab.arturbosch.detekt.idea.util.MavenDependencyResolver
import org.jetbrains.annotations.VisibleForTesting
import java.awt.Component
import javax.swing.JTree

/**
 * A dialog for excluding dependencies from download in detekt plugins.
 *
 * @param root The root node of the dependency tree.
 * @param parentComponent The Swing component that will be the parent of this dialog.
 */
internal class DependencyExclusionEditor(
    root: MavenDependencyResolver.ResolvedNode,
    private val parentComponent: Component,
) {
    private val dependenciesTree = DependenciesTree(root) { node -> syncArtifacts(node) }

    private val allNodesCache: List<CheckedTreeNodeAdapter> by lazy {
        TreeUtil.treeNodeTraverser(dependenciesTree.rootNode)
            .filter(CheckedTreeNode::class.java)
            .map(::CheckedTreeNodeAdapter)
            .toList()
    }

    /**
     * Opens a dialog to allow the user to select which transitive dependencies to exclude.
     *
     * @param initialExclusions The set of initially excluded dependency coordinates.
     * @return The updated set of excluded dependency coordinates, or null if the user cancelled.
     */
    fun selectExcludedDependencies(initialExclusions: Set<String>): Set<String>? {
        uncheckExcludedNodes(dependenciesTree.rootNode, initialExclusions, false)
        TreeUtil.expandAll(dependenciesTree)

        val dialogBuilder =
            DialogBuilder(parentComponent)
                .title(JavaUiBundle.message("dialog.title.include.transitive.dependencies"))
                .centerPanel(JBScrollPane(dependenciesTree))

        dialogBuilder.setPreferredFocusComponent(dependenciesTree)

        return if (dialogBuilder.showAndGet()) {
            val result = mutableSetOf<String>()
            collectUncheckedNodes(dependenciesTree.rootNode, result)
            result
        } else {
            null
        }
    }

    private fun uncheckExcludedNodes(node: CheckedTreeNode, excluded: Set<String>, parentIsExcluded: Boolean) {
        val isExcluded = parentIsExcluded || excluded.contains(node.mavenCoordinate)
        node.isChecked = !isExcluded || node.isProvided
        node.children().asSequence().filterIsInstance<CheckedTreeNode>().forEach {
            uncheckExcludedNodes(it, excluded, isExcluded)
        }
    }

    private fun collectUncheckedNodes(node: CheckedTreeNode, result: MutableSet<String>) {
        if (node.isChecked) {
            node.children().asSequence()
                .filterIsInstance<CheckedTreeNode>()
                .forEach { collectUncheckedNodes(it, result) }
        } else {
            result.add(node.mavenCoordinate)
        }
    }

    private fun syncArtifacts(node: CheckedTreeNode) {
        DependencySelectionHandler.syncArtifacts(CheckedTreeNodeAdapter(node), allNodesCache) { syncable, state ->
            syncable.updateChecked(state)
        }
    }

    /**
     * An adapter that allows a [CheckedTreeNode] to be used with the [DependencySelectionHandler].
     */
    private inner class CheckedTreeNodeAdapter(val node: CheckedTreeNode) : SyncableNode {

        override val groupId: String
            get() = node.resolvedNode.groupId

        override val artifactId: String
            get() = node.resolvedNode.artifactId

        override val isProvided: Boolean
            get() = node.resolvedNode.isProvided

        override var isChecked: Boolean
            get() = node.isChecked
            set(value) {
                node.isChecked = value
            }

        override val parent: SyncableNode?
            get() = (node.parent as? CheckedTreeNode)?.let { CheckedTreeNodeAdapter(it) }

        override val children: Iterable<SyncableNode>
            get() =
                node
                    .children()
                    .asSequence()
                    .filterIsInstance<CheckedTreeNode>()
                    .map { CheckedTreeNodeAdapter(it) }
                    .asIterable()

        override fun updateChecked(checked: Boolean) {
            dependenciesTree.setNodeState(node, checked)
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is CheckedTreeNodeAdapter) return false
            return node == other.node
        }

        override fun hashCode(): Int = node.hashCode()
    }

    companion object {
        private val CheckedTreeNode.mavenCoordinate: String
            get() = "${resolvedNode.groupId}:${resolvedNode.artifactId}"

        private val CheckedTreeNode.isProvided: Boolean
            get() = resolvedNode.isProvided

        private val CheckedTreeNode.resolvedNode: MavenDependencyResolver.ResolvedNode
            get() = userObject as MavenDependencyResolver.ResolvedNode
    }
}

// We need to handle this ourselves so we can deal with provided nodes properly.
// If we let the component deal with any of these, we end up in broken states.
private val checkboxCheckPolicy =
    CheckboxTreeBase.CheckPolicy(
        false, // checkChildrenWithCheckedParent
        false, // uncheckChildrenWithUncheckedParent
        false, // checkParentWithCheckedChild
        false, // uncheckParentWithUncheckedChild
    )

/**
 * A specialized [CheckboxTree] for displaying and managing Maven dependencies.
 */
private class DependenciesTree(
    root: MavenDependencyResolver.ResolvedNode,
    onNodeStateChanged: (CheckedTreeNode) -> Unit,
) : CheckboxTree(Renderer(), buildDependencyTreeNode(root), checkboxCheckPolicy) {
    val rootNode: CheckedTreeNode
        get() = model.root as CheckedTreeNode

    init {
        isRootVisible = false
        addCheckboxTreeListener(
            object : CheckboxTreeListener {
                private var processingNodes = false

                override fun nodeStateChanged(node: CheckedTreeNode) {
                    if (processingNodes) return
                    processingNodes = true
                    try {
                        onNodeStateChanged(node)
                    } finally {
                        processingNodes = false
                    }
                }
            }
        )
    }

    override fun installSpeedSearch() {
        TreeUIHelper.getInstance()
            .installTreeSpeedSearch(
                this,
                { treePath ->
                    val node = treePath.lastPathComponent as? CheckedTreeNode
                    val data = node?.userObject as? MavenDependencyResolver.ResolvedNode
                    data?.coordinate ?: ""
                },
                true,
            )
    }

    /**
     * A renderer for [DependenciesTree] that handles coordinate formatting and status (rejected, provided).
     *
     * We pass `false` for usePartialStatusForParentNodes because we want parent nodes to show their own checked state,
     * not inherit the state from their children. This is important when a parent is unchecked but all its children
     * (provided deps) are forced to stay checked.
     */
    private class Renderer :
        CheckboxTreeCellRenderer(
            true, // opaque
            false, // usePartialStatusForParentNodes
        ) {
        override fun customizeRenderer(
            tree: JTree,
            value: Any,
            selected: Boolean,
            expanded: Boolean,
            leaf: Boolean,
            row: Int,
            hasFocus: Boolean,
        ) {
            val node = (value as? CheckedTreeNode)?.userObject as? MavenDependencyResolver.ResolvedNode ?: return

            val (attributes, grayedAttributes) =
                if (node.isRejected) {
                    STRIKEOUT_ATTRIBUTES to STRIKEOUT_GRAYED_ATTRIBUTES
                } else {
                    SimpleTextAttributes.REGULAR_ATTRIBUTES to SimpleTextAttributes.GRAYED_ATTRIBUTES
                }

            if (node.isProvided) {
                textRenderer.append("(provided) ", SimpleTextAttributes.GRAYED_ATTRIBUTES)
            }
            textRenderer.append("${node.groupId}:${node.artifactId}", attributes, true)
            textRenderer.append(":${node.version}", grayedAttributes, true)

            toolTipText =
                when {
                    node.isProvided -> DetektBundle.message("tooltip.text.dependency.is.provided")
                    node.isRejected -> JavaUiBundle.message("tooltip.text.dependency.was.rejected")
                    else -> null
                }
        }
    }

    companion object {
        private val STRIKEOUT_ATTRIBUTES = SimpleTextAttributes(SimpleTextAttributes.STYLE_STRIKEOUT, null)
        private val STRIKEOUT_GRAYED_ATTRIBUTES =
            SimpleTextAttributes(SimpleTextAttributes.STYLE_STRIKEOUT, NamedColorUtil.getInactiveTextColor())

        // Node building is handled by buildDependencyTreeNode to keep it testable.
    }
}

@VisibleForTesting
internal fun buildDependencyTreeNode(
    node: MavenDependencyResolver.ResolvedNode,
    visited: Set<String> = emptySet(),
): CheckedTreeNode {
    val treeNode = CheckedTreeNode(node)
    treeNode.isEnabled = !node.isProvided
    treeNode.isChecked = true

    if (visited.contains(node.coordinate)) {
        treeNode.isEnabled = false
        return treeNode
    }

    val nextVisited = visited + node.coordinate
    for (dependency in node.children) {
        treeNode.add(buildDependencyTreeNode(dependency, nextVisited))
    }
    return treeNode
}
