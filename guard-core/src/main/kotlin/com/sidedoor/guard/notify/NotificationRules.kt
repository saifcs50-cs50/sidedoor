package com.sidedoor.guard.notify

/**
 * How Sidedoor recognises a friend's reel in Instagram's notifications.
 * Instagram files messages under the three `ig_direct*` channels; everything
 * else (likes, lives, suggestions) is ignored.
 */
object NotificationRules {

    private val MESSAGE_CHANNELS = setOf("ig_direct", "ig_direct_requests", "ig_direct_video_chat")

    /** "reel" as a whole word, so "really" does not count. */
    private val REEL = Regex("""\breels?\b""", RegexOption.IGNORE_CASE)

    fun isMessage(channelId: String?): Boolean = channelId in MESSAGE_CHANNELS

    fun isReelShare(texts: List<String>): Boolean = texts.any { REEL.containsMatchIn(it) }
}
