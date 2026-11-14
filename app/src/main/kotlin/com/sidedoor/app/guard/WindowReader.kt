package com.sidedoor.app.guard

import android.accessibilityservice.AccessibilityService
import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import com.sidedoor.guard.classify.IgIds
import com.sidedoor.guard.model.Bounds
import com.sidedoor.guard.window.Presence
import com.sidedoor.guard.window.WindowInfo
import com.sidedoor.guard.window.WindowKind
import com.sidedoor.guard.window.WindowStack

/**
 * Where Instagram is in the window stack, plus the root of its frontmost window.
 * [instagramOnScreen] stays true while Instagram is merely covered (the shade,
 * a dialog) - it is still drawn, just not in front.
 */
data class WindowScan(
    val presence: Presence,
    val instagramRoot: AccessibilityNodeInfo?,
    val instagramOnScreen: Boolean,
    /** Every window as the guard saw it, top first - for debug logs. */
    val windows: List<WindowInfo> = emptyList(),
) {
    fun describe(): String = windows.sortedByDescending { it.layer }
        .joinToString { "${it.kind}:${it.packageName ?: "-"}@${it.bounds.width}x${it.bounds.height}" }
}

object WindowReader {

    fun scan(service: AccessibilityService): WindowScan {
        val windows = service.windows.orEmpty()
        val rect = Rect()
        var igRoot: AccessibilityNodeInfo? = null
        var igLayer = Int.MIN_VALUE

        val infos = windows.map { w ->
            val isApp = w.type == AccessibilityWindowInfo.TYPE_APPLICATION
            val root = if (isApp) w.root else null
            val pkg = root?.packageName?.toString() ?: if (isApp) WindowStack.UNREADABLE_APP else null
            if (pkg == IgIds.PACKAGE && w.layer > igLayer) {
                igRoot = root
                igLayer = w.layer
            }
            w.getBoundsInScreen(rect)
            WindowInfo(kindOf(w.type), pkg, Bounds(rect.left, rect.top, rect.right, rect.bottom), w.layer)
        }
        val presence = WindowStack.assess(infos, IgIds.PACKAGE, screenBounds(service))
        return WindowScan(presence, igRoot.takeIf { presence.appInFront }, instagramOnScreen = igRoot != null, windows = infos)
    }

    private fun kindOf(type: Int): WindowKind = when (type) {
        AccessibilityWindowInfo.TYPE_APPLICATION -> WindowKind.APPLICATION
        AccessibilityWindowInfo.TYPE_INPUT_METHOD -> WindowKind.INPUT_METHOD
        AccessibilityWindowInfo.TYPE_SYSTEM -> WindowKind.SYSTEM
        AccessibilityWindowInfo.TYPE_ACCESSIBILITY_OVERLAY -> WindowKind.ACCESSIBILITY_OVERLAY
        else -> WindowKind.OTHER
    }

    private fun screenBounds(service: AccessibilityService): Bounds {
        val metrics = service.resources.displayMetrics
        return Bounds(0, 0, metrics.widthPixels, metrics.heightPixels)
    }
}
