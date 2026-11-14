package com.sidedoor.guard.window

import com.sidedoor.guard.model.Bounds

/** The window kinds the guard cares about, mirroring AccessibilityWindowInfo types. */
enum class WindowKind { APPLICATION, INPUT_METHOD, SYSTEM, ACCESSIBILITY_OVERLAY, OTHER }

data class WindowInfo(
    val kind: WindowKind,
    val packageName: String?,
    val bounds: Bounds,
    /** Z-order: higher is closer to the user. */
    val layer: Int,
)

/**
 * Where the watched app stands in the window stack.
 *
 * [appInFront] gates every overlay: the shield and the dead zones may only
 * exist while the app is the frontmost app window with nothing large drawn
 * over it. Get this wrong and a touch-eating rectangle is left sitting on the
 * home screen's dock or the notification shade.
 */
data class Presence(
    val appInFront: Boolean,
    val imeVisible: Boolean,
    /**
     * A large system window (the notification shade, a system dialog, the
     * lock screen) is on top. Android then lists only that window, so the app
     * underneath looks gone even though it will be right back.
     */
    val coveredBySystem: Boolean = false,
    /** The frontmost app window's package, when an app (rather than a system window) is in front. */
    val frontPackage: String? = null,
) {
    /** Dead zones sit where the keyboard draws, so they must yield to it. */
    val canShowDeadZones: Boolean get() = appInFront && !imeVisible

    /**
     * What took the watched app's place, for deciding whether a visit is over:
     * the front app's package; [WindowStack.UNREADABLE_APP] when no app window
     * is visible at all (recorded on the test phone: an app outside the service's
     * package filter is left out of the window list entirely, so "nothing
     * there" means "an app we may not see"); or null when a large system
     * window covers everything, which is not leaving.
     */
    val replacedBy: String? get() = if (coveredBySystem) null else frontPackage ?: WindowStack.UNREADABLE_APP

    companion object {
        val ABSENT = Presence(appInFront = false, imeVisible = false)
    }
}

object WindowStack {

    /**
     * Package reported for an app window the service is not allowed to read.
     * An accessibility service limited to certain packages still sees every
     * window, but not whose it is - so "some other app" must stay distinct
     * from "no app at all" (a system window covering everything).
     */
    const val UNREADABLE_APP = "?"

    /** A system window this much of the screen is the shade, a dialog or a lock screen - not a status bar. */
    private const val LARGE_SYSTEM_WINDOW_FRACTION = 0.4

    fun assess(windows: List<WindowInfo>, watchedPackage: String, screen: Bounds): Presence {
        val screenArea = screen.width.toLong() * screen.height
        var imeVisible = false
        for (w in windows.sortedByDescending { it.layer }) {
            when (w.kind) {
                WindowKind.ACCESSIBILITY_OVERLAY, WindowKind.OTHER -> Unit
                WindowKind.INPUT_METHOD -> imeVisible = true
                WindowKind.SYSTEM -> if (area(w.bounds) > screenArea * LARGE_SYSTEM_WINDOW_FRACTION) return COVERED
                WindowKind.APPLICATION -> return Presence(
                    appInFront = w.packageName == watchedPackage,
                    imeVisible = imeVisible,
                    frontPackage = w.packageName,
                )
            }
        }
        return Presence.ABSENT
    }

    private val COVERED = Presence(appInFront = false, imeVisible = false, coveredBySystem = true)

    private fun area(b: Bounds): Long = if (b.isEmpty) 0 else b.width.toLong() * b.height
}
