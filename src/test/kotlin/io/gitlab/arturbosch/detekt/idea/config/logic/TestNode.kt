package io.gitlab.arturbosch.detekt.idea.config.logic

internal class TestNode(
    override val groupId: String,
    override val artifactId: String,
    override val isProvided: Boolean = false,
    initialChecked: Boolean = true,
    override val parent: TestNode? = null,
) : SyncableNode {
    override var isChecked: Boolean = initialChecked
    override val children = mutableListOf<TestNode>()

    override fun updateChecked(checked: Boolean) {
        isChecked = checked
    }

    fun addChild(child: TestNode) {
        children.add(child)
    }
}

internal fun sync(node: SyncableNode, allNodes: Iterable<SyncableNode>) {
    DependencySelectionHandler.syncArtifacts(node, allNodes) { n, state ->
        (n as TestNode).updateChecked(state)
    }
}
