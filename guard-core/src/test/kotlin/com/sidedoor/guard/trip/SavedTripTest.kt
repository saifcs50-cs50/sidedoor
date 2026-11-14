package com.sidedoor.guard.trip

import com.google.common.truth.Truth.assertThat
import com.sidedoor.guard.classify.Screen
import com.sidedoor.guard.support.Fixtures
import com.sidedoor.guard.support.IgScreens
import org.junit.Test

class SavedTripTest {

    @Test
    fun `from the inbox the first hop is the profile tab`() {
        val step = SavedTrip.next(Screen.INBOX, Fixtures.load("inbox.xml"))
        assertThat(step).isEqualTo(TripStep.Tap(Target.ById("profile_tab")))
    }

    @Test
    fun `from your profile open the options menu`() {
        val step = SavedTrip.next(Screen.OWN_PROFILE, Fixtures.load("own_profile.xml"))
        assertThat(step).isEqualTo(TripStep.Tap(Target.ByLabel("Options")))
    }

    @Test
    fun `from settings tap saved`() {
        val step = SavedTrip.next(Screen.SETTINGS, Fixtures.load("settings.xml"))
        assertThat(step).isEqualTo(TripStep.Tap(Target.ByLabel("Saved")))
    }

    @Test
    fun `arriving at saved ends the trip`() {
        assertThat(SavedTrip.next(Screen.SAVED_HUB, IgScreens.savedHub())).isEqualTo(TripStep.Arrived)
    }

    @Test
    fun `a chat is backed out of first`() {
        assertThat(SavedTrip.next(Screen.THREAD, Fixtures.load("thread.xml"))).isEqualTo(TripStep.Back)
    }

    @Test
    fun `a screen that is still drawing is waited on`() {
        assertThat(SavedTrip.next(Screen.UNKNOWN, IgScreens.savedHub())).isEqualTo(TripStep.Wait)
    }
}
