package com.sidedoor.app.guard

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import com.sidedoor.guard.model.Bounds
import com.sidedoor.guard.model.ScreenSnapshot
import com.sidedoor.guard.model.UiNode

/**
 * Turns Instagram's live accessibility tree into a [ScreenSnapshot].
 *
 * Mirrors `uiautomator dump`, which the test fixtures were recorded with: a
 * node that is not visible to the user is skipped along with its subtree.
 * Keeping the two identical is what lets the JVM tests stand in for the phone.
 */
object SnapshotReader {

    /** Instagram's deepest real screen measured ~30 levels; this is a runaway guard, not a limit. */
    private const val MAX_DEPTH = 80

    fun read(root: AccessibilityNodeInfo): ScreenSnapshot {
        val out = ArrayList<UiNode>(256)
        visit(root, depth = 0, out = out, rect = Rect())
        return ScreenSnapshot(out)
    }

    private fun visit(node: AccessibilityNodeInfo, depth: Int, out: MutableList<UiNode>, rect: Rect) {
        if (depth > MAX_DEPTH || !node.isVisibleToUser) return
        node.getBoundsInScreen(rect)
        val bounds = Bounds(rect.left, rect.top, rect.right, rect.bottom)
        if (!bounds.isEmpty) {
            out += UiNode(
                viewId = node.viewIdResourceName?.substringAfter(":id/"),
                text = node.text?.toString(),
                description = node.contentDescription?.toString(),
                bounds = bounds,
                isSelected = node.isSelected,
                isClickable = node.isClickable,
            )
        }
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { visit(it, depth + 1, out, rect) }
        }
    }
}
