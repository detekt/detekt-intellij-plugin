package io.gitlab.arturbosch.detekt.idea.util

import io.gitlab.arturbosch.detekt.idea.MockProjectTestCase
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PluginDependencyServiceTest : MockProjectTestCase() {

    @Test
    fun `returns correct plugin folder`() {
        val service = PluginDependencyService(project)
        val folder = service.getPluginFolder()
        assertThat(folder.toString()).isEqualTo(tempDir.resolve(".idea/detektPlugins").toString())
    }

    @Test
    fun `collectAllowedNodes filters out provided dependencies`() {
        val service = PluginDependencyService(project)
        val providedNode = MavenDependencyResolver.ResolvedNode(
            "org.jetbrains.kotlin",
            "kotlin-stdlib",
            "1.9.0",
            "org.jetbrains.kotlin:kotlin-stdlib:1.9.0",
            isProvided = true,
            isRejected = false,
            children = emptyList(),
        )
        val normalNode = MavenDependencyResolver.ResolvedNode(
            "com.example",
            "lib",
            "1.0.0",
            "com.example:lib:1.0.0",
            isProvided = false,
            isRejected = false,
            children = emptyList(),
        )
        val rootNode = MavenDependencyResolver.ResolvedNode(
            "com.example",
            "plugin",
            "1.0.0",
            "com.example:plugin:1.0.0",
            isProvided = false,
            isRejected = false,
            children = listOf(providedNode, normalNode),
        )

        val result = service.collectAllowedNodes(rootNode, emptySet())

        assertThat(result.map { it.coordinate })
            .containsExactlyInAnyOrder("com.example:plugin:1.0.0", "com.example:lib:1.0.0")
            .doesNotContain("org.jetbrains.kotlin:kotlin-stdlib:1.9.0")
    }

    @Test
    fun `collectAllowedNodes respects explicit exclusions`() {
        val service = PluginDependencyService(project)
        val excludedNode = MavenDependencyResolver.ResolvedNode(
            "com.example",
            "excluded",
            "1.0.0",
            "com.example:excluded:1.0.0",
            isProvided = false,
            isRejected = false,
            children = emptyList(),
        )
        val rootNode = MavenDependencyResolver.ResolvedNode(
            "com.example",
            "plugin",
            "1.0.0",
            "com.example:plugin:1.0.0",
            isProvided = false,
            isRejected = false,
            children = listOf(excludedNode),
        )

        val result = service.collectAllowedNodes(rootNode, setOf("com.example:excluded"))

        assertThat(result.map { it.coordinate })
            .containsExactly("com.example:plugin:1.0.0")
            .doesNotContain("com.example:excluded:1.0.0")
    }

    @Test
    fun `collectAllowedNodes handles relative exclusions`() {
        val service = PluginDependencyService(project)
        val childNode = MavenDependencyResolver.ResolvedNode(
            "com.example",
            "child",
            "1.0.0",
            "com.example:child:1.0.0",
            isProvided = false,
            isRejected = false,
            children = emptyList(),
        )
        val parentNode = MavenDependencyResolver.ResolvedNode(
            "com.example",
            "parent",
            "1.0.0",
            "com.example:parent:1.0.0",
            isProvided = false,
            isRejected = false,
            children = listOf(childNode),
        )

        // When parent is excluded, children should also be excluded because they are
        // reached
        // through parent
        val result = service.collectAllowedNodes(parentNode, setOf("com.example:parent"))

        assertThat(result).isEmpty()
    }

    @Test
    fun `collectAllowedNodes handles deeply nested trees`() {
        val service = PluginDependencyService(project)
        val level3 = MavenDependencyResolver.ResolvedNode(
            "com.example",
            "level3",
            "1.0.0",
            "com.example:level3:1.0.0",
            isProvided = false,
            isRejected = false,
            children = emptyList(),
        )
        val level2 = MavenDependencyResolver.ResolvedNode(
            "com.example",
            "level2",
            "1.0.0",
            "com.example:level2:1.0.0",
            isProvided = false,
            isRejected = false,
            children = listOf(level3),
        )
        val level1 = MavenDependencyResolver.ResolvedNode(
            "com.example",
            "level1",
            "1.0.0",
            "com.example:level1:1.0.0",
            isProvided = false,
            isRejected = false,
            children = listOf(level2),
        )
        val root = MavenDependencyResolver.ResolvedNode(
            "com.example",
            "root",
            "1.0.0",
            "com.example:root:1.0.0",
            isProvided = false,
            isRejected = false,
            children = listOf(level1),
        )

        val result = service.collectAllowedNodes(root, emptySet())

        assertThat(result.map { it.coordinate })
            .containsExactlyInAnyOrder(
                "com.example:root:1.0.0",
                "com.example:level1:1.0.0",
                "com.example:level2:1.0.0",
                "com.example:level3:1.0.0",
            )
    }

    @Test
    fun `collectAllowedNodes handles node with empty children`() {
        val service = PluginDependencyService(project)
        val rootNode = MavenDependencyResolver.ResolvedNode(
            "com.example",
            "plugin",
            "1.0.0",
            "com.example:plugin:1.0.0",
            isProvided = false,
            isRejected = false,
            children = emptyList(),
        )

        val result = service.collectAllowedNodes(rootNode, emptySet())

        assertThat(result.map { it.coordinate }).containsExactly("com.example:plugin:1.0.0")
    }

    @Test
    fun `collectAllowedNodes deduplicates nodes with same coordinate`() {
        val service = PluginDependencyService(project)
        // Same dependency appearing twice in tree (diamond dependency)
        val sharedDep = MavenDependencyResolver.ResolvedNode(
            "com.example",
            "shared",
            "1.0.0",
            "com.example:shared:1.0.0",
            isProvided = false,
            isRejected = false,
            children = emptyList(),
        )
        val child1 = MavenDependencyResolver.ResolvedNode(
            "com.example",
            "child1",
            "1.0.0",
            "com.example:child1:1.0.0",
            isProvided = false,
            isRejected = false,
            children = listOf(sharedDep),
        )
        val child2 = MavenDependencyResolver.ResolvedNode(
            "com.example",
            "child2",
            "1.0.0",
            "com.example:child2:1.0.0",
            isProvided = false,
            isRejected = false,
            children = listOf(sharedDep),
        )
        val root = MavenDependencyResolver.ResolvedNode(
            "com.example",
            "root",
            "1.0.0",
            "com.example:root:1.0.0",
            isProvided = false,
            isRejected = false,
            children = listOf(child1, child2),
        )

        val result = service.collectAllowedNodes(root, emptySet())

        // shared should only appear once despite being in both child1 and child2
        assertThat(result.count { it.coordinate == "com.example:shared:1.0.0" }).isEqualTo(1)
        assertThat(result.map { it.coordinate })
            .containsExactlyInAnyOrder(
                "com.example:root:1.0.0",
                "com.example:child1:1.0.0",
                "com.example:child2:1.0.0",
                "com.example:shared:1.0.0",
            )
    }

    @Test
    fun `collectAllowedNodes handles direct cycles`() {
        val service = PluginDependencyService(project)
        // Need to use a lazy-initialized list or a mutable list to create a cycle in
        // ResolvedNode
        // which is a data class with an immutable list of children.
        // However, we can use a custom subclass if needed, or just mock it.
        // Let's create a cycle by passing the same node as a child.
        val root = MavenDependencyResolver.ResolvedNode(
            "com.example",
            "root",
            "1.0.0",
            "com.example:root:1.0.0",
            isProvided = false,
            isRejected = false,
            children = emptyList(), // will be swapped
        )

        val cyclicNode = root.copy(children = listOf(root))

        val result = service.collectAllowedNodes(cyclicNode, emptySet())

        assertThat(result.map { it.coordinate }).containsExactly("com.example:root:1.0.0")
    }

    @Test
    fun `collectAllowedNodes handles indirect cycles`() {
        val service = PluginDependencyService(project)
        val nodeB = MavenDependencyResolver.ResolvedNode(
            "com.example",
            "nodeB",
            "1.0.0",
            "com.example:nodeB:1.0.0",
            isProvided = false,
            isRejected = false,
            children = emptyList(), // cycle back to A
        )
        val nodeA = MavenDependencyResolver.ResolvedNode(
            "com.example",
            "nodeA",
            "1.0.0",
            "com.example:nodeA:1.0.0",
            isProvided = false,
            isRejected = false,
            children = listOf(nodeB),
        )

        // Close the cycle: B -> A
        val nodeBWithCycle = nodeB.copy(children = listOf(nodeA))
        val nodeAWithCycle = nodeA.copy(children = listOf(nodeBWithCycle))

        val result = service.collectAllowedNodes(nodeAWithCycle, emptySet())

        assertThat(result.map { it.coordinate })
            .containsExactlyInAnyOrder("com.example:nodeA:1.0.0", "com.example:nodeB:1.0.0")
    }
}
