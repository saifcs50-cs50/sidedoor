package com.sidedoor.guard.session

import com.google.common.truth.Truth.assertThat
import com.sidedoor.guard.window.WindowStack
import org.junit.Test

class GuardSessionTest {

    private val enders = setOf("com.sec.android.app.launcher", "com.sidedoor.app")
    private val started = GuardSession.start(now = 0)

    @Test
    fun `going to the home screen ends the session`() {
        assertThat(started.leftFor("com.sec.android.app.launcher", enders, now = 5_000)).isNull()
    }

    @Test
    fun `going back to sidedoor itself ends the session`() {
        assertThat(started.leftFor("com.sidedoor.app", enders, now = 5_000)).isNull()
    }

    @Test
    fun `a short detour into another app keeps the session`() {
        val away = checkNotNull(started.leftFor("com.google.android.photopicker", enders, now = 1_000))
        val back = away.returned(now = 1_000 + GuardSession.DETOUR_MS - 1)
        assertThat(back).isEqualTo(started)
    }

    @Test
    fun `an app the guard cannot read into is a detour, not a cover`() {
        val away = checkNotNull(started.leftFor(WindowStack.UNREADABLE_APP, enders, now = 1_000))
        assertThat(away.awaySince).isEqualTo(1_000)
        assertThat(away.returned(now = 1_000 + GuardSession.DETOUR_MS + 1)).isNull()
    }

    @Test
    fun `a long detour ends the session`() {
        val away = checkNotNull(started.leftFor("com.whatsapp", enders, now = 1_000))
        assertThat(away.returned(now = 1_000 + GuardSession.DETOUR_MS + 1)).isNull()
    }

    @Test
    fun `the detour clock starts when you first leave, not on every check`() {
        val first = checkNotNull(started.leftFor("com.whatsapp", enders, now = 1_000))
        val later = checkNotNull(first.leftFor("com.whatsapp", enders, now = 30_000))
        assertThat(later.awaySince).isEqualTo(1_000)
    }

    @Test
    fun `a detour becomes overdue after a minute`() {
        val away = checkNotNull(started.leftFor("com.whatsapp", enders, now = 1_000))
        assertThat(away.isOverdue(now = 1_000 + GuardSession.DETOUR_MS)).isFalse()
        assertThat(away.isOverdue(now = 1_000 + GuardSession.DETOUR_MS + 1)).isTrue()
        assertThat(started.isOverdue(now = Long.MAX_VALUE)).isFalse()
    }

    @Test
    fun `going home from inside a detour still ends the session`() {
        val away = checkNotNull(started.leftFor("com.whatsapp", enders, now = 1_000))
        assertThat(away.leftFor("com.sec.android.app.launcher", enders, now = 5_000)).isNull()
    }

    @Test
    fun `being covered by the shade or the lock screen is not leaving`() {
        // No app in front at all: a system window covers everything.
        val covered = started.leftFor(frontApp = null, enders = enders, now = 10 * 60_000L)
        assertThat(covered).isEqualTo(started)
        assertThat(covered?.returned(now = 20 * 60_000L)).isEqualTo(started)
    }
}
