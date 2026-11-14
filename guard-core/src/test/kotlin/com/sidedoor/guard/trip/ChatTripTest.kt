package com.sidedoor.guard.trip

import com.google.common.truth.Truth.assertThat
import com.sidedoor.guard.classify.Screen
import com.sidedoor.guard.inbox.inboxRow
import com.sidedoor.guard.model.Bounds
import com.sidedoor.guard.support.snapshot
import org.junit.Test

class ChatTripTest {

    private val inbox = snapshot {
        id("inbox_refreshable_thread_list_recyclerview")
        inboxRow("Casey, Riley, Seen by all", 502, name = "Casey, Riley")
        inboxRow("Alex, unread, Sent a reel by example_user ·, 37m", 644)
        inboxRow("Casey, Sent 3m ago", 786)
    }

    private fun thread(title: String) = snapshot {
        id("direct_thread_header")
        id("header_title", text = title, bounds = Bounds(217, 84, 340, 118))
    }

    @Test
    fun `from the inbox it taps that chat's row`() {
        assertThat(ChatTrip.next(Screen.INBOX, inbox, "Alex"))
            .isEqualTo(TripStep.Tap(Target.ByBounds(Bounds(0, 644, 720, 786))))
    }

    @Test
    fun `a chat is not confused with a group named after its members`() {
        assertThat(ChatTrip.next(Screen.INBOX, inbox, "Casey"))
            .isEqualTo(TripStep.Tap(Target.ByBounds(Bounds(0, 786, 720, 928))))
    }

    @Test
    fun `a chat that is not on screen leaves you at the inbox`() {
        assertThat(ChatTrip.next(Screen.INBOX, inbox, "Riley")).isEqualTo(TripStep.Arrived)
    }

    @Test
    fun `only the right chat counts as arriving`() {
        assertThat(ChatTrip.next(Screen.THREAD, thread("Alex"), "Alex")).isEqualTo(TripStep.Arrived)
        assertThat(ChatTrip.next(Screen.THREAD, thread("Casey"), "Alex")).isEqualTo(TripStep.Back)
    }

    @Test
    fun `saved and chat goals route to their own trips`() {
        assertThat(nextStep(TripGoal.Chat("Alex"), Screen.INBOX, inbox))
            .isEqualTo(TripStep.Tap(Target.ByBounds(Bounds(0, 644, 720, 786))))
        assertThat(nextStep(TripGoal.Saved, Screen.INBOX, inbox)).isEqualTo(TripStep.Wait)
    }
}
