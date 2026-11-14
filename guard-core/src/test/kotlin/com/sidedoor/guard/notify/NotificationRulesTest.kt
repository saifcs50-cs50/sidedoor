package com.sidedoor.guard.notify

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class NotificationRulesTest {

    @Test
    fun `direct message channels are messages`() {
        listOf("ig_direct", "ig_direct_requests", "ig_direct_video_chat").forEach {
            assertThat(NotificationRules.isMessage(it)).isTrue()
        }
    }

    @Test
    fun `engagement channels and unlabelled notifications are not messages`() {
        listOf("ig_likes", "ig_first_posts_and_stories", "ig_live_videos", "ig_other", null).forEach {
            assertThat(NotificationRules.isMessage(it)).isFalse()
        }
    }

    @Test
    fun `a message that mentions a reel is a reel share`() {
        assertThat(NotificationRules.isReelShare(listOf("sent you a reel by someone"))).isTrue()
        assertThat(NotificationRules.isReelShare(listOf("Shared a Reel"))).isTrue()
    }

    @Test
    fun `ordinary messages are not reel shares`() {
        assertThat(NotificationRules.isReelShare(listOf("are you coming tonight", "lol"))).isFalse()
        assertThat(NotificationRules.isReelShare(listOf("I really like that"))).isFalse()
    }
}
