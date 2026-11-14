package com.sidedoor.guard.classify

/** What Instagram is showing, as far as the guard is concerned. */
enum class Screen {
    INBOX,
    THREAD,

    /** A reel a friend sent, opened from the chat. */
    FRIEND_REEL,
    /** A reel from your Saved, opened full screen. Its bookmark reads "Remove from saved". */
    SAVED_REEL,
    OWN_PROFILE,
    SETTINGS,
    SAVED_HUB,

    /** A vertical list of posts. Allowed only when entered from Saved or a chat. */
    POSTS_LIST,

    HOME_FEED,
    REELS_TAB,
    EXPLORE,

    /** Instagram's picks, reached by swiping past a friend's reel or through Blend. */
    SUGGESTED_REEL,
    OTHER_PROFILE,

    /** Not recognised, but shows media. Blocked by default. */
    UNKNOWN_MEDIA,

    /** Not recognised and shows no media: settings pages, pickers, sheets. */
    UNKNOWN,
}
