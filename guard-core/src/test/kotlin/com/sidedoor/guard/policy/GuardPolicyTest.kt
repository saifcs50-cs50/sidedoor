package com.sidedoor.guard.policy

import com.google.common.truth.Truth.assertThat
import com.sidedoor.guard.classify.Screen
import org.junit.Test

class GuardPolicyTest {

    /** Feeds screens through the policy in order, returning the final verdict. */
    private fun walk(vararg screens: Screen): Verdict {
        var state = GuardState()
        var verdict: Verdict = Verdict.Allow
        screens.forEach { screen ->
            val decision = GuardPolicy.decide(state, screen)
            state = decision.next
            verdict = decision.verdict
        }
        return verdict
    }

    @Test
    fun `chats, friend reels, saved and the path to saved are allowed`() {
        listOf(
            Screen.INBOX, Screen.THREAD, Screen.FRIEND_REEL,
            Screen.OWN_PROFILE, Screen.SETTINGS, Screen.SAVED_HUB,
        ).forEach { assertThat(walk(it)).isEqualTo(Verdict.Allow) }
    }

    @Test
    fun `feed, reels tab and explore send you to the inbox`() {
        listOf(Screen.HOME_FEED, Screen.REELS_TAB, Screen.EXPLORE).forEach {
            val verdict = walk(it) as Verdict.Block
            assertThat(verdict.correction).isEqualTo(Correction.GO_TO_INBOX)
        }
    }

    @Test
    fun `swiping past a friend's reel is sent back to the chat`() {
        val verdict = walk(Screen.THREAD, Screen.FRIEND_REEL, Screen.SUGGESTED_REEL) as Verdict.Block
        assertThat(verdict.correction).isEqualTo(Correction.BACK)
        assertThat(verdict.reason).isEqualTo(BlockReason.END_OF_FRIEND_REELS)
    }

    @Test
    fun `profiles and unrecognised media go back`() {
        listOf(Screen.OTHER_PROFILE, Screen.UNKNOWN_MEDIA).forEach {
            assertThat((walk(Screen.THREAD, it) as Verdict.Block).correction).isEqualTo(Correction.BACK)
        }
    }

    @Test
    fun `a post list opened from saved is allowed and stays allowed while scrolling`() {
        assertThat(walk(Screen.SAVED_HUB, Screen.POSTS_LIST)).isEqualTo(Verdict.Allow)
        assertThat(walk(Screen.SAVED_HUB, Screen.POSTS_LIST, Screen.POSTS_LIST, Screen.POSTS_LIST))
            .isEqualTo(Verdict.Allow)
    }

    @Test
    fun `a post opened from a chat is allowed`() {
        assertThat(walk(Screen.THREAD, Screen.POSTS_LIST)).isEqualTo(Verdict.Allow)
    }

    @Test
    fun `a post list reached any other way is blocked`() {
        assertThat(walk(Screen.POSTS_LIST)).isInstanceOf(Verdict.Block::class.java)
        assertThat(walk(Screen.OWN_PROFILE, Screen.POSTS_LIST)).isInstanceOf(Verdict.Block::class.java)
    }

    @Test
    fun `a hashtag page tapped inside a friend's reel does not unlock posts`() {
        // Friend reel -> hashtag grid (unrecognised, no media ids) -> post list.
        val verdict = walk(Screen.THREAD, Screen.FRIEND_REEL, Screen.UNKNOWN, Screen.POSTS_LIST)
        assertThat(verdict).isInstanceOf(Verdict.Block::class.java)
    }

    @Test
    fun `sheets and pickers between saved and a post do not break the saved context`() {
        assertThat(walk(Screen.SAVED_HUB, Screen.UNKNOWN, Screen.POSTS_LIST)).isEqualTo(Verdict.Allow)
    }

    @Test
    fun `leaving saved for another recognised screen ends the saved context`() {
        val verdict = walk(Screen.SAVED_HUB, Screen.POSTS_LIST, Screen.INBOX, Screen.POSTS_LIST)
        assertThat(verdict).isInstanceOf(Verdict.Block::class.java)
    }

    @Test
    fun `a blocked screen in between does not become the context`() {
        // Saved -> (a suggested post list gets blocked and backed out of) -> saved list again.
        val verdict = walk(Screen.SAVED_HUB, Screen.OTHER_PROFILE, Screen.POSTS_LIST)
        assertThat(verdict).isEqualTo(Verdict.Allow)
    }

    @Test
    fun `a saved reel opened full screen is allowed`() {
        assertThat(walk(Screen.SAVED_HUB, Screen.POSTS_LIST, Screen.SAVED_REEL)).isEqualTo(Verdict.Allow)
    }

    @Test
    fun `backing out of a saved reel returns to the saved list`() {
        val verdict = walk(Screen.SAVED_HUB, Screen.POSTS_LIST, Screen.SAVED_REEL, Screen.POSTS_LIST)
        assertThat(verdict).isEqualTo(Verdict.Allow)
    }

    @Test
    fun `swiping from a saved reel onto one you did not save is blocked`() {
        val verdict = walk(Screen.SAVED_HUB, Screen.SAVED_REEL, Screen.SUGGESTED_REEL) as Verdict.Block
        assertThat(verdict.correction).isEqualTo(Correction.BACK)
        assertThat(verdict.reason).isEqualTo(BlockReason.NOT_SAVED)
    }

    @Test
    fun `unknown screens without media are allowed`() {
        assertThat(walk(Screen.UNKNOWN)).isEqualTo(Verdict.Allow)
    }
}
