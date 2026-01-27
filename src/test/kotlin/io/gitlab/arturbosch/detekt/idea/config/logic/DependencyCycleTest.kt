package io.gitlab.arturbosch.detekt.idea.config.logic

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DependencyCycleTest {

    @Test
    fun `direct cycle does not cause stack overflow during propagation`() {
        val node = TestNode("g", "a")
        node.addChild(node) // A -> A cycle

        node.isChecked = false
        // Should not throw StackOverflowError
        sync(node, listOf(node))
        assertFalse(node.isChecked)
    }

    @Test
    fun `indirect cycle does not cause stack overflow during propagation`() {
        val nodeA = TestNode("g", "a")
        val nodeB = TestNode("g", "b")
        nodeA.addChild(nodeB)
        nodeB.addChild(nodeA) // A -> B -> A cycle

        nodeA.isChecked = false
        // Should not throw StackOverflowError
        sync(nodeA, listOf(nodeA, nodeB))

        assertFalse(nodeA.isChecked)
        assertFalse(nodeB.isChecked)
    }

    @Test
    fun `multiple paths to same node do not cause issues`() {
        // This is a diamond, not a cycle, but tests recursion depth/visit tracking
        val root = TestNode("root", "r")
        val left = TestNode("left", "l")
        val right = TestNode("right", "r")
        val bottom = TestNode("bottom", "b")

        root.addChild(left)
        root.addChild(right)
        left.addChild(bottom)
        right.addChild(bottom)

        root.isChecked = false
        // Should not throw StackOverflowError
        sync(root, listOf(root, left, right, bottom))

        assertFalse(root.isChecked)
        assertFalse(left.isChecked)
        assertFalse(right.isChecked)
        assertFalse(bottom.isChecked)
    }

    @Test
    fun `checking child in cyclic dependency propagates to ancestors`() {
        val nodeA = TestNode("g", "a", initialChecked = false)
        val nodeB = TestNode("g", "b", initialChecked = false, parent = nodeA)
        nodeA.addChild(nodeB)
        nodeB.addChild(nodeA) // Cycle

        nodeB.isChecked = true
        sync(nodeB, listOf(nodeA, nodeB))

        assertTrue(nodeB.isChecked)
        assertTrue(nodeA.isChecked, "Parent nodeA should have been checked by propagation from child nodeB")
    }
}
