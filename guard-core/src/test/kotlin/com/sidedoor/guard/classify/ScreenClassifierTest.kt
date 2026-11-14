package com.sidedoor.guard.classify

import com.google.common.truth.Truth.assertThat
import com.sidedoor.guard.model.ScreenSnapshot
import com.sidedoor.guard.support.Fixtures
import com.sidedoor.guard.support.IgScreens
import com.sidedoor.guard.support.snapshot
import org.junit.Test

class ScreenClassifierTest {

    private fun classify(s: ScreenSnapshot) = ScreenClassifier.classify(s)

    // --- real dumps from the test phone (Instagram 448.0.0.52.84) -----------------

    @Test
    fun `home feed dump is the home feed`() {
        assertThat(classify(Fixtures.load("home_feed.xml"))).isEqualTo(Screen.HOME_FEED)
    }

    @Test
    fun `inbox dump is the inbox`() {
        assertThat(classify(Fixtures.load("inbox.xml"))).isEqualTo(Screen.INBOX)
    }

    @Test
    fun `chat dump is a thread even though it contains reel shares`() {
        assertThat(classify(Fixtures.load("thread.xml"))).isEqualTo(Screen.THREAD)
    }

    @Test
    fun `own profile dump is own profile`() {
        assertThat(classify(Fixtures.load("own_profile.xml"))).isEqualTo(Screen.OWN_PROFILE)
    }

    @Test
    fun `settings and activity dump is settings`() {
        assertThat(classify(Fixtures.load("settings.xml"))).isEqualTo(Screen.SETTINGS)
    }

    @Test
    fun `live friend reel dump is a friend reel`() {
        assertThat(classify(Fixtures.load("friend_reel.xml"))).isEqualTo(Screen.FRIEND_REEL)
    }

    @Test
    fun `opening comments on a friend reel keeps it a friend reel`() {
        assertThat(classify(Fixtures.load("friend_reel_comments.xml"))).isEqualTo(Screen.FRIEND_REEL)
    }

    // --- video screens, from dumpsys IDs ------------------------------------

    @Test
    fun `reel with a reply bar is a friend reel`() {
        assertThat(classify(IgScreens.friendReel())).isEqualTo(Screen.FRIEND_REEL)
    }

    @Test
    fun `reel reached by swiping past a friend reel is suggested`() {
        assertThat(classify(IgScreens.suggestedReel())).isEqualTo(Screen.SUGGESTED_REEL)
    }

    @Test
    fun `a full screen reel you saved is a saved reel`() {
        assertThat(classify(IgScreens.savedReel())).isEqualTo(Screen.SAVED_REEL)
    }

    @Test
    fun `live dump of a reel opened from saved is a saved reel`() {
        // The viewer says "Save" but marks the bookmark selected - the case that was wrongly blocked.
        assertThat(classify(Fixtures.load("saved_reel.xml"))).isEqualTo(Screen.SAVED_REEL)
    }

    @Test
    fun `a bookmark relabelled remove from saved also counts`() {
        val s = snapshot {
            id("clips_viewer_view_pager")
            id("save_button", desc = "Remove from saved")
        }
        assertThat(classify(s)).isEqualTo(Screen.SAVED_REEL)
    }

    @Test
    fun `a saved reel met on the reels tab is still the reels tab`() {
        assertThat(classify(IgScreens.reelsTab(bookmark = "Remove from saved"))).isEqualTo(Screen.REELS_TAB)
    }

    @Test
    fun `live saved dumps are the saved hub and a posts list`() {
        assertThat(classify(Fixtures.load("saved_hub.xml"))).isEqualTo(Screen.SAVED_HUB)
        assertThat(classify(Fixtures.load("saved_feed.xml"))).isEqualTo(Screen.POSTS_LIST)
    }

    @Test
    fun `reels tab is the reels tab`() {
        assertThat(classify(IgScreens.reelsTab())).isEqualTo(Screen.REELS_TAB)
    }

    @Test
    fun `saved collections page is the saved hub`() {
        assertThat(classify(IgScreens.savedHub())).isEqualTo(Screen.SAVED_HUB)
    }

    @Test
    fun `post rows without a tab bar are a posts list whose meaning depends on where you came from`() {
        assertThat(classify(IgScreens.savedFeed())).isEqualTo(Screen.POSTS_LIST)
        assertThat(classify(IgScreens.postsFeed())).isEqualTo(Screen.POSTS_LIST)
    }

    // --- tab bar ------------------------------------------------------------

    @Test
    fun `each selected tab maps to its screen`() {
        val expected = mapOf(
            "feed_tab" to Screen.HOME_FEED,
            "clips_tab" to Screen.REELS_TAB,
            "direct_tab" to Screen.INBOX,
            "search_tab" to Screen.EXPLORE,
            "profile_tab" to Screen.OWN_PROFILE,
        )
        expected.forEach { (tab, screen) ->
            assertThat(classify(snapshot { tabBar(selected = tab) })).isEqualTo(screen)
        }
    }

    @Test
    fun `someone else's profile is not your own`() {
        val s = snapshot {
            id("row_profile_header_imageview")
            id("profile_header_familiar_followers_value", text = "201")
        }
        assertThat(classify(s)).isEqualTo(Screen.OTHER_PROFILE)
    }

    // --- default deny for media ---------------------------------------------

    @Test
    fun `an unrecognised screen that plays media is unknown media`() {
        val s = snapshot { id("brand_new_feed_surface"); id("clips_video_container") }
        assertThat(classify(s)).isEqualTo(Screen.UNKNOWN_MEDIA)
    }

    @Test
    fun `an unrecognised screen without media is plain unknown`() {
        val s = snapshot { id("some_settings_page"); label("Notification preferences") }
        assertThat(classify(s)).isEqualTo(Screen.UNKNOWN)
    }

    @Test
    fun `story tray avatars in the inbox do not count as media`() {
        val s = snapshot { id("reel_viewer_front_avatar"); id("pog_root_view") }
        assertThat(classify(s)).isEqualTo(Screen.UNKNOWN)
    }

    @Test
    fun `an empty snapshot is unknown so the guard never acts on a half-drawn window`() {
        assertThat(classify(ScreenSnapshot.EMPTY)).isEqualTo(Screen.UNKNOWN)
    }
}
