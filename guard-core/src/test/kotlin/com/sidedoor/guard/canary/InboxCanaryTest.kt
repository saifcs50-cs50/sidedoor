package com.sidedoor.guard.canary

import com.google.common.truth.Truth.assertThat
import com.sidedoor.guard.classify.Screen
import org.junit.Test

class InboxCanaryTest {

    @Test
    fun `seeing the inbox in time passes`() {
        val armed = InboxCanary.arm(now = 0)
        assertThat(armed.onScreen(Screen.INBOX, now = 1_500)).isEqualTo(CanaryResult.Passed)
    }

    @Test
    fun `other screens on the way do not settle it`() {
        val armed = InboxCanary.arm(now = 0)
        assertThat(armed.onScreen(Screen.UNKNOWN, now = 300)).isEqualTo(CanaryResult.Pending)
        assertThat(armed.onScreen(Screen.HOME_FEED, now = 600)).isEqualTo(CanaryResult.Pending)
    }

    @Test
    fun `no inbox before the deadline fails`() {
        val armed = InboxCanary.arm(now = 0)
        assertThat(armed.onTick(now = InboxCanary.DEADLINE_MS - 1)).isEqualTo(CanaryResult.Pending)
        assertThat(armed.onTick(now = InboxCanary.DEADLINE_MS + 1)).isEqualTo(CanaryResult.Failed)
    }
}
