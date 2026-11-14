package com.sidedoor.guard.policy

import com.sidedoor.guard.classify.Screen

/** How the guard gets you off a blocked screen. */
enum class Correction {
    /** Tab screens: switch to the Messages tab. Back from Home would just exit Instagram. */
    GO_TO_INBOX,

    /** Screens reached from a chat or Saved: step back to where you were. */
    BACK,
}

/** Why a screen was blocked. Drives the one-line message on the shield. */
enum class BlockReason(val message: String) {
    FEED("No feed in here"),
    REELS_TAB("No Reels tab in here"),
    EXPLORE("No Explore in here"),
    END_OF_FRIEND_REELS("That's everything they sent"),
    NOT_SAVED("That one isn't in your Saved"),
    PROFILE("Profiles stay closed"),
    STRAY_POSTS("Only posts from chats and Saved"),
    UNKNOWN_MEDIA("Not part of Sidedoor"),
}

sealed interface Verdict {
    data object Allow : Verdict

    data class Block(val screen: Screen, val reason: BlockReason, val correction: Correction) : Verdict
}
