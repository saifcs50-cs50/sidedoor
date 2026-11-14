package com.sidedoor.app.overlay

import android.graphics.PixelFormat
import android.os.Build
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import com.sidedoor.guard.model.Bounds

/**
 * Accessibility-overlay windows. An accessibility service can draw these with
 * no SYSTEM_ALERT_WINDOW grant, they sit above the app, and Android trusts
 * them for touches - so a covered tab really does eat the tap.
 */
internal class OverlayWindows(private val wm: WindowManager) {

    fun add(view: View, params: WindowManager.LayoutParams): Boolean = try {
        wm.addView(view, params)
        true
    } catch (e: WindowManager.BadTokenException) {
        Log.e(TAG, "Overlay rejected", e)
        false
    } catch (e: IllegalStateException) {
        Log.e(TAG, "Overlay already attached", e)
        false
    }

    fun remove(view: View) {
        runCatching { wm.removeViewImmediate(view) }.onFailure { Log.w(TAG, "Overlay already gone", it) }
    }

    companion object {
        private const val TAG = "SidedoorOverlay"

        fun fullScreen(touchable: Boolean): WindowManager.LayoutParams =
            params(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT, touchable)

        /** A window exactly covering [b], in screen coordinates. */
        fun at(b: Bounds): WindowManager.LayoutParams =
            params(b.width, b.height, touchable = true).apply {
                x = b.left
                y = b.top
            }

        private fun params(width: Int, height: Int, touchable: Boolean) = WindowManager.LayoutParams(
            width,
            height,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                (if (touchable) 0 else WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE),
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // Accessibility bounds are absolute screen pixels; do not let
                // the system shift the window below the status bar.
                fitInsetsTypes = 0
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
    }
}
