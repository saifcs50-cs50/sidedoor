package com.sidedoor.app.overlay

import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import com.sidedoor.app.R

/**
 * An opaque cover over Instagram while the guard corrects a blocked screen or
 * walks to Saved, so the feed is never actually visible - only this, for a
 * fraction of a second. It eats touches unless [show] is told otherwise.
 */
class Shield(private val context: Context) {

    private val windows = OverlayWindows(context.getSystemService(WindowManager::class.java))
    private val handler = Handler(Looper.getMainLooper())
    private val pendingHide = Runnable { hide() }

    private var view: View? = null
    private var message: TextView? = null
    private var touchable = true

    val isShowing: Boolean get() = view != null

    fun show(text: String, touchable: Boolean = true) {
        handler.removeCallbacks(pendingHide)
        if (view != null && this.touchable == touchable) {
            message?.text = text
            return
        }
        hide()
        val built = build(text)
        if (windows.add(built.first, OverlayWindows.fullScreen(touchable))) {
            view = built.first
            message = built.second
            this.touchable = touchable
        }
    }

    /** Hide after [delayMs] unless shown again first - rides out half-drawn transition frames. */
    fun hideAfter(delayMs: Long) {
        if (view == null) return
        handler.removeCallbacks(pendingHide)
        handler.postDelayed(pendingHide, delayMs)
    }

    fun hide() {
        handler.removeCallbacks(pendingHide)
        view?.let(windows::remove)
        view = null
        message = null
    }

    private fun build(text: String): Pair<View, TextView> {
        val label = TextView(context).apply {
            this.text = text
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            gravity = Gravity.CENTER
        }
        val brand = TextView(context).apply {
            this.text = context.getString(R.string.app_name)
            setTextColor(Color.argb(140, 255, 255, 255))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, 0)
        }
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(context.getColor(R.color.shield))
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
            addView(label)
            addView(brand)
        }
        return root to label
    }

    private fun dp(v: Int) = (v * context.resources.displayMetrics.density).toInt()
}
