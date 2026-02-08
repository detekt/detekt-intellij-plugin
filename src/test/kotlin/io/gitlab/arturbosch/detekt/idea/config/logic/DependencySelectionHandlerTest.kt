package io.gitlab.arturbosch.detekt.idea.config.logic

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DependencySelectionHandlerTest {

    @Test
    fun `provided nodes stay checked`() {
        val node = TestNode("g", "a", isProvided = true, initialChecked = true)
        node.isChecked = false // simulate UI uncheck

        sync(node, listOf(node))

        assertTrue(node.isChecked, "Provided node should have been forced back to checked")
    }

    @Test
    fun `global synchronization works`() {
        val node1 = TestNode("g", "a", initialChecked = true)
        val node2 = TestNode("g", "a", initialChecked = true)
        val other = TestNode("g", "b", initialChecked = true)
        val allNodes = listOf(node1, node2, other)

        node1.isChecked = false
        sync(node1, allNodes)

        assertFalse(node2.isChecked, "Second instance of same artifact should be unchecked")
        assertTrue(other.isChecked, "Unrelated artifact should remain checked")
    }

    @Test
    fun `children propagation works`() {
        val parent = TestNode("p", "a")
        val child = TestNode("c", "b", parent = parent)
        parent.addChild(child)

        parent.isChecked = false
        sync(parent, listOf(parent, child))

        assertFalse(child.isChecked, "Child should be unchecked when parent is unchecked")
    }

    @Test
    fun `provided children are not unchecked`() {
        val parent = TestNode("p", "a")
        val child = TestNode("c", "b", isProvided = true, parent = parent)
        parent.addChild(child)

        parent.isChecked = false
        sync(parent, listOf(parent, child))

        assertFalse(parent.isChecked)
        assertTrue(child.isChecked, "Provided child should remain checked even if parent is unchecked")
    }

    @Test
    fun `parent propagation works`() {
        val parent = TestNode("p", "a", initialChecked = false)
        val child = TestNode("c", "b", initialChecked = false, parent = parent)
        parent.addChild(child)

        child.isChecked = true
        sync(child, listOf(parent, child))

        assertTrue(parent.isChecked, "Parent should be checked when child is checked")
    }

    @Test
    fun `provided nodes do not propagate to parents`() {
        val parent = TestNode("p", "a", initialChecked = false)
        val child = TestNode("c", "b", isProvided = true, initialChecked = true, parent = parent)
        parent.addChild(child)

        // Simulate child triggering sync (e.g. during initial protection logic)
        sync(child, listOf(parent, child))

        assertFalse(parent.isChecked, "Provided child should not force parent to be checked")
    }

    @Test
    fun `complex scenario uncheck parent with provided child and non-provided child`() {
        val parent = TestNode("p", "a")
        val providedChild = TestNode("c", "provided", isProvided = true, parent = parent)
        val normalChild = TestNode("c", "normal", parent = parent)
        parent.addChild(providedChild)
        parent.addChild(normalChild)

        val allNodes = listOf(parent, providedChild, normalChild)

        parent.isChecked = false
        sync(parent, allNodes)

        assertFalse(parent.isChecked)
        assertTrue(providedChild.isChecked, "Provided child stays")
        assertFalse(normalChild.isChecked, "Normal child goes")
    }

    @Test
    fun `regression test - nlopez compose rules behavior`() {
        // Root is invisible, but for our test we'll use a dummy root
        val dummyRoot = TestNode("root", "root", isProvided = true)

        // Siblings at top level
        val topDetektCore = TestNode("dev.detekt", "detekt-core", isProvided = true, parent = dummyRoot)
        val ioNlopez = TestNode("io.nlopez.compose.rules", "common-detekt", isProvided = false, parent = dummyRoot)
        val topKotlinStdlib = TestNode("org.jetbrains.kotlin", "kotlin-stdlib", isProvided = true, parent = dummyRoot)

        dummyRoot.addChild(topDetektCore)
        dummyRoot.addChild(ioNlopez)
        dummyRoot.addChild(topKotlinStdlib)

        // Children of ioNlopez
        val childDetektCore = TestNode("dev.detekt", "detekt-core", isProvided = true, parent = ioNlopez)
        val childDetektPsi = TestNode("dev.detekt", "detekt-psi-utils", isProvided = true, parent = ioNlopez)
        val childKotlinStdlib = TestNode("org.jetbrains.kotlin", "kotlin-stdlib", isProvided = true, parent = ioNlopez)

        ioNlopez.addChild(childDetektCore)
        ioNlopez.addChild(childDetektPsi)
        ioNlopez.addChild(childKotlinStdlib)

        val allNodes =
            listOf(
                dummyRoot,
                topDetektCore,
                ioNlopez,
                topKotlinStdlib,
                childDetektCore,
                childDetektPsi,
                childKotlinStdlib,
            )

        // Initial state: all checked
        assertTrue(ioNlopez.isChecked)

        // Action: uncheck ioNlopez
        ioNlopez.isChecked = false
        sync(ioNlopez, allNodes)

        // Verification
        assertFalse(ioNlopez.isChecked, "The clicked node should definitely be unchecked")
        assertTrue(topDetektCore.isChecked, "Provided sibling should stay checked")
        assertTrue(childDetektCore.isChecked, "Provided child should stay checked")
    }

    @Test
    fun `deeply nested tree propagation 4 levels`() {
        val root = TestNode("root", "a", initialChecked = true)
        val level1 = TestNode("level1", "b", initialChecked = true, parent = root)
        val level2 = TestNode("level2", "c", initialChecked = true, parent = level1)
        val level3 = TestNode("level3", "d", initialChecked = true, parent = level2)
        val level4 = TestNode("level4", "e", initialChecked = true, parent = level3)

        root.addChild(level1)
        level1.addChild(level2)
        level2.addChild(level3)
        level3.addChild(level4)

        val allNodes = listOf(root, level1, level2, level3, level4)

        // Uncheck root should uncheck all descendants
        root.isChecked = false
        sync(root, allNodes)

        assertFalse(root.isChecked)
        assertFalse(level1.isChecked, "Level 1 should be unchecked")
        assertFalse(level2.isChecked, "Level 2 should be unchecked")
        assertFalse(level3.isChecked, "Level 3 should be unchecked")
        assertFalse(level4.isChecked, "Level 4 should be unchecked")
    }

    @Test
    fun `root node with no parent checks correctly`() {
        val root = TestNode("root", "a", initialChecked = false, parent = null)

        root.isChecked = true
        sync(root, listOf(root))

        // Should not throw and root should remain checked
        assertTrue(root.isChecked, "Root should stay checked")
    }

    @Test
    fun `unchecking node does not affect siblings`() {
        val parent = TestNode("parent", "a")
        val child1 = TestNode("sibling1", "b", initialChecked = true, parent = parent)
        val child2 = TestNode("sibling2", "c", initialChecked = true, parent = parent)
        parent.addChild(child1)
        parent.addChild(child2)

        val allNodes = listOf(parent, child1, child2)

        child1.isChecked = false
        sync(child1, allNodes)

        assertFalse(child1.isChecked, "Clicked node should be unchecked")
        assertTrue(child2.isChecked, "Sibling should remain checked")
        assertTrue(parent.isChecked, "Parent should remain checked")
    }
}
