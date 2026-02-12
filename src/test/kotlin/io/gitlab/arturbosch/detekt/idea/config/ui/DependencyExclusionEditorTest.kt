package io.gitlab.arturbosch.detekt.idea.config.ui

import io.gitlab.arturbosch.detekt.idea.util.MavenDependencyResolver
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import javax.swing.tree.TreeNode

class DependencyExclusionEditorTest {

    @Test
    fun `buildDependencyTreeNode keeps children even when root is provided`() {
        val child =
            node(
                groupId = "com.example",
                artifactId = "child",
                isProvided = false,
                children = emptyList(),
            )
        val root =
            node(
                groupId = "io.gitlab.arturbosch.detekt",
                artifactId = "detekt-formatting",
                isProvided = true,
                children = listOf(child),
            )

        val treeNode = buildDependencyTreeNode(root)

        assertThat(treeNode.childCount).isEqualTo(1)
        assertThat(treeNode.isEnabled).isFalse()
        val onlyChild = treeNode.getChildAt(0) as TreeNode
        assertThat((onlyChild as com.intellij.ui.CheckedTreeNode).isEnabled).isTrue()
    }

    private fun node(
        groupId: String,
        artifactId: String,
        isProvided: Boolean,
        children: List<MavenDependencyResolver.ResolvedNode>,
    ): MavenDependencyResolver.ResolvedNode {
        val version = "1.0.0"
        val coordinate = "$groupId:$artifactId:$version"
        return MavenDependencyResolver.ResolvedNode(
            groupId = groupId,
            artifactId = artifactId,
            version = version,
            coordinate = coordinate,
            isProvided = isProvided,
            isRejected = false,
            children = children,
            file = null,
        )
    }
}
