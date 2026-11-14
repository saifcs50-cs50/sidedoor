package com.sidedoor.guard.policy

import com.google.common.truth.Truth.assertThat
import com.sidedoor.guard.model.Bounds
import com.sidedoor.guard.support.Fixtures
import com.sidedoor.guard.support.snapshot
import org.junit.Test

class DeadZonePlannerTest {

    private fun bounds(fixture: String) = DeadZonePlanner.plan(Fixtures.load(fixture)).map { it.bounds }

    @Test
    fun `home, reels and search tabs are covered on the real inbox`() {
        val zones = DeadZonePlanner.plan(Fixtures.load("inbox.xml"))
        assertThat(zones).containsAtLeast(
            Zone(Bounds(0, 1490, 144, 1572), Paint.TAB_BAR),   // Home
            Zone(Bounds(144, 1490, 288, 1572), Paint.TAB_BAR), // Reels
            Zone(Bounds(432, 1490, 576, 1572), Paint.TAB_BAR), // Search
        )
    }

    @Test
    fun `messages and profile tabs stay tappable`() {
        assertThat(bounds("inbox.xml")).containsNoneOf(Bounds(288, 1490, 432, 1572), Bounds(576, 1490, 720, 1572))
    }

    @Test
    fun `story avatars on inbox rows are covered but the rows stay tappable`() {
        val zones = DeadZonePlanner.plan(Fixtures.load("inbox.xml"))
        // First inbox row on the test phone: row [0,644][720,786], its story avatar [35,666][133,764].
        assertThat(zones).contains(Zone(Bounds(35, 666, 133, 764), Paint.CLEAR))
        assertThat(zones.map { it.bounds }).doesNotContain(Bounds(0, 644, 720, 786))
    }

    @Test
    fun `the blend button and story avatar in a chat header are covered`() {
        assertThat(bounds("thread.xml")).containsAtLeast(
            Bounds(435, 64, 525, 169),  // Blend
            Bounds(127, 82, 195, 150),  // "Open story of ..."
        )
    }

    @Test
    fun `call buttons in a chat header stay tappable`() {
        assertThat(bounds("thread.xml")).containsNoneOf(Bounds(525, 64, 615, 169), Bounds(615, 64, 705, 169))
    }

    @Test
    fun `a friend's reel cannot be swiped but its buttons and reply bar still work`() {
        val zones = DeadZonePlanner.plan(Fixtures.load("friend_reel.xml"))
            .filterNot { it.bounds.width < 200 } // small zones are header buttons, tested separately
        // Recorded on the test phone: pager [0,64][720,1459], top bar ends at 169, buttons start at x=622.
        assertThat(zones.filter { it.paint == Paint.CLEAR })
            .containsExactly(Zone(Bounds(0, 169, 622, 1459), Paint.CLEAR))
    }

    @Test
    fun `a reel opened from blend also has its blend settings button covered`() {
        assertThat(bounds("friend_reel.xml")).contains(Bounds(616, 72, 705, 161))
    }

    @Test
    fun `the comments sheet on a friend's reel stays usable`() {
        assertThat(DeadZonePlanner.plan(Fixtures.load("friend_reel_comments.xml"))).isEmpty()
    }

    @Test
    fun `no tab bar and no friend reel means no zones`() {
        assertThat(DeadZonePlanner.plan(snapshot { id("clips_viewer_view_pager") })).isEmpty()
    }
}
