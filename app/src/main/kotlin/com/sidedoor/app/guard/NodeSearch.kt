package com.sidedoor.app.guard

import android.view.accessibility.AccessibilityNodeInfo

/**
 * Breadth-first search of the live accessibility tree.
 *
 * The platform's findAccessibilityNodeInfosByText only searches classic Views;
 * it never looks inside Compose's virtual nodes, which is how Instagram builds
 * its Settings screens. Walking the tree ourselves sees what the snapshot sees.
 */
object NodeSearch {

    private const val MAX_NODES = 1_500

    fun first(root: AccessibilityNodeInfo, predicate: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo? {
        val queue = ArrayDeque<AccessibilityNodeInfo>().apply { add(root) }
        var visited = 0
        while (queue.isNotEmpty() && visited++ < MAX_NODES) {
            val node = queue.removeFirst()
            if (!node.isVisibleToUser) continue
            if (predicate(node)) return node
            for (i in 0 until node.childCount) node.getChild(i)?.let(queue::add)
        }
        return null
    }

    /** Every visible match, in breadth-first order, up to [limit]. */
    fun all(
        root: AccessibilityNodeInfo,
        limit: Int = 8,
        predicate: (AccessibilityNodeInfo) -> Boolean,
    ): List<AccessibilityNodeInfo> {
        val found = ArrayList<AccessibilityNodeInfo>(limit)
        val queue = ArrayDeque<AccessibilityNodeInfo>().apply { add(root) }
        var visited = 0
        while (queue.isNotEmpty() && visited++ < MAX_NODES && found.size < limit) {
            val node = queue.removeFirst()
            if (!node.isVisibleToUser) continue
            if (predicate(node)) found += node
            for (i in 0 until node.childCount) node.getChild(i)?.let(queue::add)
        }
        return found
    }

    fun hasLabel(node: AccessibilityNodeInfo, label: String): Boolean =
        node.text?.toString() == label || node.contentDescription?.toString() == label
}
