package com.sidedoor.app.guard

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import com.sidedoor.guard.classify.IgIds
import com.sidedoor.guard.trip.Target

/** Taps things inside Instagram on the guard's behalf. */
object NodeActions {

    private const val TAG = "Sidedoor"
    private const val MAX_ANCESTOR_HOPS = 6
    private const val TAP_DURATION_MS = 40L

    /**
     * Clicks [target] through the accessibility API, walking up to the
     * nearest clickable ancestor. Instagram's Bloks screens (Settings) often
     * expose no clickable node at all, so those fall back to a real tap.
     */
    fun tap(service: AccessibilityService, root: AccessibilityNodeInfo, target: Target): Boolean {
        val node = find(root, target)
        if (node == null) {
            Log.w(TAG, "tap $target: not on screen")
            return false
        }
        if (clickSelfOrAncestor(node)) {
            Log.i(TAG, "tap $target: clicked")
            return true
        }
        val rect = Rect().also(node::getBoundsInScreen)
        val sent = gestureTap(service, rect.exactCenterX(), rect.exactCenterY())
        Log.i(TAG, "tap $target: gesture at ${rect.centerX()},${rect.centerY()} sent=$sent")
        return sent
    }

    private fun find(root: AccessibilityNodeInfo, target: Target): AccessibilityNodeInfo? = when (target) {
        is Target.ById -> {
            val fullId = "${IgIds.PACKAGE}:id/${target.viewId}"
            NodeSearch.first(root) { it.viewIdResourceName == fullId }
        }
        // Exact match: "Saved" must not hit "Saved audio". Instagram often carries the
        // same label on a decorative node and on the real button; take the button.
        is Target.ByLabel -> {
            val matches = NodeSearch.all(root) { NodeSearch.hasLabel(it, target.label) }
            matches.firstOrNull(::clickableWithinReach) ?: matches.firstOrNull()
        }
        // An inbox row: the clickable node covering exactly those bounds.
        is Target.ByBounds -> {
            val rect = Rect()
            val matches = NodeSearch.all(root) {
                it.getBoundsInScreen(rect)
                rect.left == target.bounds.left && rect.top == target.bounds.top &&
                    rect.right == target.bounds.right && rect.bottom == target.bounds.bottom
            }
            matches.firstOrNull(::clickableWithinReach) ?: matches.firstOrNull()
        }
    }

    private fun clickableWithinReach(start: AccessibilityNodeInfo): Boolean {
        var node: AccessibilityNodeInfo? = start
        repeat(MAX_ANCESTOR_HOPS) {
            val current = node ?: return false
            if (current.isClickable) return true
            node = current.parent
        }
        return false
    }

    private fun clickSelfOrAncestor(start: AccessibilityNodeInfo): Boolean {
        var node: AccessibilityNodeInfo? = start
        repeat(MAX_ANCESTOR_HOPS) {
            val current = node ?: return false
            if (current.isClickable) return current.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            node = current.parent
        }
        return false
    }

    private fun gestureTap(service: AccessibilityService, x: Float, y: Float): Boolean {
        val path = Path().apply { moveTo(x, y) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, TAP_DURATION_MS))
            .build()
        return service.dispatchGesture(gesture, null, null)
    }
}
