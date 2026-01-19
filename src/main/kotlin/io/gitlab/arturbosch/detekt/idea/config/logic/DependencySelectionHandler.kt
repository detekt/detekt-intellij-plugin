package io.gitlab.arturbosch.detekt.idea.config.logic

/**
 * Handles the synchronization and propagation logic for dependency selection.
 */
object DependencySelectionHandler {

    /**
     * Synchronizes selection state for the given [node] across [allNodes] and propagates changes to relatives.
     * Propagation rules are based on JPS' UI's rules, plus adjustments to deal with always-selected provided nodes.
     *
     * Rules implemented:
     * 1. **Provided Protection**: provided nodes cannot be unchecked.
     * 2. **Global Sync**: all nodes with the same G:A as [node] update to the new state.
     * 3. **Children Propagation**: if [node] is toggled, all children update to match (respecting provided protection).
     * 4. **Parent Propagation**: if [node] is checked, all ancestors are checked (unless [node] is provided).
     */
    fun syncArtifacts(
        node: SyncableNode,
        allNodes: Iterable<SyncableNode>,
        setNodeState: (SyncableNode, Boolean) -> Unit,
    ) {
        // 1. Provided Node Protection: force provided nodes to stay checked
        if (node.isProvided) {
            if (!node.isChecked) {
                setNodeState(node, true)
            }
            // Provided nodes are static and should not be sources of propagation
            return
        }

        val mavenCoordinate = node.mavenCoordinate
        val newState = node.isChecked

        // 2. Global Synchronization: sync all occurrences of the same artifact
        synchronize(allNodes, mavenCoordinate, newState, setNodeState)

        // 3. Children Propagation: recursively update the state of all descendants
        propagateToChildren(node, newState, setNodeState)

        // 4. Parent Propagation: if checked, recursively ensure all ancestors are checked
        propagateToParent(newState, node, setNodeState)
    }

    private fun synchronize(
        allNodes: Iterable<SyncableNode>,
        mavenCoordinate: String,
        newState: Boolean,
        setNodeState: (SyncableNode, Boolean) -> Unit,
    ) {
        for (treeNode in allNodes) {
            val matchingCoordinates = treeNode.mavenCoordinate == mavenCoordinate
            val isNowCheckedOrNotProvided = newState || !treeNode.isProvided
            val checkedChanged = treeNode.isChecked != newState
            if (matchingCoordinates && isNowCheckedOrNotProvided && checkedChanged) {
                setNodeState(treeNode, newState)
            }
        }
    }

    private fun propagateToChildren(
        node: SyncableNode,
        newState: Boolean,
        setNodeState: (SyncableNode, Boolean) -> Unit,
        visited: Set<SyncableNode> = emptySet(),
    ) {
        if (visited.contains(node)) return // Prevent loops
        val newVisited = visited + node

        for (child in node.children) {
            if (newState) {
                setNodeState(child, true)
            } else if (!child.isProvided) {
                setNodeState(child, false)
            }

            // Provided children stay checked when unchecking parent
            propagateToChildren(child, newState, setNodeState, newVisited)
        }
    }

    private fun propagateToParent(
        newState: Boolean,
        node: SyncableNode,
        setNodeState: (SyncableNode, Boolean) -> Unit,
    ) {
        if (newState) {
            var parent = node.parent
            while (parent != null) {
                if (!parent.isChecked) {
                    setNodeState(parent, true)
                }
                parent = parent.parent
            }
        }
    }
}
