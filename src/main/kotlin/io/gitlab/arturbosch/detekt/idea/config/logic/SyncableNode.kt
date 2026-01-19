package io.gitlab.arturbosch.detekt.idea.config.logic

/**
 * Interface for a node that can be synchronized and propagated in a dependency tree.
 */
interface SyncableNode {
    /**
     * The Maven group ID of the dependency.
     */
    val groupId: String

    /**
     * The Maven artifact ID of the dependency.
     */
    val artifactId: String

    /**
     * Whether this dependency is provided by the platform and cannot be unchecked.
     */
    val isProvided: Boolean

    /**
     * Whether this dependency is currently checked for download.
     */
    var isChecked: Boolean

    /**
     * The parent node in the dependency tree, or null if this is a root node.
     */
    val parent: SyncableNode?

    /**
     * The list of children (transitive dependencies) of this node.
     */
    val children: Iterable<SyncableNode>

    /**
     * Updates the checked state of this node. In a UI implementation, this should update the underlying checkbox.
     */
    fun updateChecked(checked: Boolean)

    /**
     * The Maven coordinates ([groupId]:[artifactId]) of the dependency.
     */
    val mavenCoordinate: String
        get() = "$groupId:$artifactId"
}
