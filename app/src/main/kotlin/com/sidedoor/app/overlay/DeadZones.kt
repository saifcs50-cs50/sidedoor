package com.sidedoor.app.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.view.WindowManager
import com.sidedoor.app.R
import com.sidedoor.guard.policy.Paint
import com.sidedoor.guard.policy.Zone

/**
 * Touch-eating patches: in Instagram's tab-bar colour over the Home, Reels and
 * Search tabs (they read as empty slots), and clear over a friend's reel (it
 * plays, but there is no swiping on to the next one).
 */
class DeadZones(private val context: Context) {

    private val windows = OverlayWindows(context.getSystemService(WindowManager::class.java))
    private var shown: Map<Zone, View> = emptyMap()

    fun show(zones: List<Zone>) {
        val wanted = zones.toSet()
        if (wanted == shown.keys) return
        val keep = shown.filterKeys { it in wanted }
        shown.filterKeys { it !in wanted }.values.forEach(windows::remove)
        val added = (wanted - keep.keys).mapNotNull { zone ->
            val v = patch(zone.paint)
            if (windows.add(v, OverlayWindows.at(zone.bounds))) zone to v else null
        }
        shown = keep + added
    }

    fun hide() = show(emptyList())

    @SuppressLint("ClickableViewAccessibility")
    private fun patch(paint: Paint) = View(context).apply {
        if (paint == Paint.TAB_BAR) setBackgroundColor(context.getColor(R.color.ig_tab_bar))
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        setOnTouchListener { _, _ -> true }
    }
}
