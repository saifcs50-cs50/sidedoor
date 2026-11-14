package com.sidedoor.guard.window

import com.google.common.truth.Truth.assertThat
import com.sidedoor.guard.model.Bounds
import org.junit.Test

class WindowStackTest {

    private val screen = Bounds(0, 0, 720, 1600)
    private val ig = "com.instagram.android"

    private fun app(pkg: String, layer: Int) = WindowInfo(WindowKind.APPLICATION, pkg, screen, layer)
    private val statusBar = WindowInfo(WindowKind.SYSTEM, "com.android.systemui", Bounds(0, 0, 720, 64), 30)
    private val navBar = WindowInfo(WindowKind.SYSTEM, "com.android.systemui", Bounds(0, 1572, 720, 1600), 29)
    private val ourOverlay = WindowInfo(WindowKind.ACCESSIBILITY_OVERLAY, "com.sidedoor.app", screen, 40)

    @Test
    fun `instagram under the status and nav bars is in front`() {
        val p = WindowStack.assess(listOf(statusBar, navBar, ourOverlay, app(ig, 10)), ig, screen)
        assertThat(p).isEqualTo(Presence(appInFront = true, imeVisible = false, frontPackage = ig))
    }

    @Test
    fun `the launcher on top means instagram is not in front`() {
        val p = WindowStack.assess(listOf(statusBar, app("com.sec.android.app.launcher", 12), app(ig, 10)), ig, screen)
        assertThat(p.appInFront).isFalse()
    }

    @Test
    fun `a pulled-down notification shade hides the overlays`() {
        val shade = WindowInfo(WindowKind.SYSTEM, "com.android.systemui", screen, 35)
        assertThat(WindowStack.assess(listOf(shade, app(ig, 10)), ig, screen).appInFront).isFalse()
    }

    @Test
    fun `a full screen shade is reported as covering, even when it is the only window listed`() {
        // Recorded on the test phone: with the shade open, Android lists nothing but the shade.
        val shade = WindowInfo(WindowKind.SYSTEM, "com.android.systemui", screen, 0)
        assertThat(WindowStack.assess(listOf(shade), ig, screen).coveredBySystem).isTrue()
    }

    @Test
    fun `the app in front is reported, so going home can be told from a detour`() {
        val p = WindowStack.assess(listOf(statusBar, app("com.sec.android.app.launcher", 12), app(ig, 10)), ig, screen)
        assertThat(p.frontPackage).isEqualTo("com.sec.android.app.launcher")
    }

    @Test
    fun `an app missing from the window list is an app the guard cannot see, not a cover`() {
        // Recorded on the test phone with Chrome in front: only the status bar and Sidedoor's own overlays are listed.
        val p = WindowStack.assess(listOf(statusBar, ourOverlay), ig, screen)
        assertThat(p.appInFront).isFalse()
        assertThat(p.replacedBy).isEqualTo(WindowStack.UNREADABLE_APP)
    }

    @Test
    fun `under the shade nothing has replaced instagram`() {
        val shade = WindowInfo(WindowKind.SYSTEM, "com.android.systemui", screen, 0)
        assertThat(WindowStack.assess(listOf(shade), ig, screen).replacedBy).isNull()
    }

    @Test
    fun `a readable app in front is named`() {
        val p = WindowStack.assess(listOf(statusBar, app("com.sec.android.app.launcher", 12)), ig, screen)
        assertThat(p.replacedBy).isEqualTo("com.sec.android.app.launcher")
    }

    @Test
    fun `another app in front is not covering`() {
        val p = WindowStack.assess(listOf(statusBar, app("com.sec.android.app.launcher", 12)), ig, screen)
        assertThat(p.coveredBySystem).isFalse()
    }

    @Test
    fun `the keyboard keeps the shield but drops the dead zones`() {
        val ime = WindowInfo(WindowKind.INPUT_METHOD, "com.samsung.android.honeyboard", Bounds(0, 1000, 720, 1600), 20)
        val p = WindowStack.assess(listOf(statusBar, ime, app(ig, 10)), ig, screen)
        assertThat(p.appInFront).isTrue()
        assertThat(p.canShowDeadZones).isFalse()
    }

    @Test
    fun `layer order wins over list order`() {
        val p = WindowStack.assess(listOf(app(ig, 10), app("com.whatsapp", 11)), ig, screen)
        assertThat(p.appInFront).isFalse()
    }

    @Test
    fun `no windows at all is absent`() {
        assertThat(WindowStack.assess(emptyList(), ig, screen)).isEqualTo(Presence.ABSENT)
    }
}
