package com.sidedoor.guard.classify

/**
 * Every Instagram view ID and label the guard relies on, in one place.
 *
 * Recorded on an Android phone running Instagram 448.0.0.52.84 (2026-09-28).
 * When Instagram renames something, this file and the fixtures are what
 * change; the rules in [ScreenClassifier] should not have to.
 */
object IgIds {

    const val PACKAGE = "com.instagram.android"

    // Bottom tab bar (order on screen: Home, Reels, Messages, Search, Profile).
    const val TAB_BAR = "tab_bar"
    const val TAB_HOME = "feed_tab"
    const val TAB_REELS = "clips_tab"
    const val TAB_DIRECT = "direct_tab"
    const val TAB_SEARCH = "search_tab"
    const val TAB_PROFILE = "profile_tab"
    val TABS = listOf(TAB_HOME, TAB_REELS, TAB_DIRECT, TAB_SEARCH, TAB_PROFILE)

    /** Tabs that lead only to algorithmic content. Covered by dead zones. */
    val FORBIDDEN_TABS = listOf(TAB_HOME, TAB_REELS, TAB_SEARCH)

    val INBOX = setOf("inbox_refreshable_thread_list_recyclerview", "direct_inbox_action_bar")
    val THREAD = setOf("direct_thread_header", "row_thread_composer_edittext", "message_list")

    /** The full-screen reel pager. Shared by the Reels tab, DM reels, Blend and suggestions. */
    const val CLIPS_VIEWER = "clips_viewer_view_pager"

    /**
     * Only a reel someone actually sent you carries a reply bar ("Reply to X").
     * Swipe past it and Instagram's picks show a "Send to chat" pill instead.
     */
    val DM_REEL_REPLY = setOf("reel_viewer_message_composer", "reply_bar_edittext")

    /**
     * The bookmark on a full-screen reel, which is how a reel from your Saved
     * is told apart from Instagram's picks in the same viewer. Recorded on the
     * test phone: the full-screen viewer keeps the label "Save" and marks the button
     * *selected* when the reel is saved; post lists relabel it "Remove from
     * saved" instead. Either signal counts.
     */
    const val SAVE_BUTTON = "save_button"
    const val SAVED_STATE_DESC = "Remove from saved"

    val SAVED_HUB = setOf("saved_view_pager", "saved_tab_layout", "saved_collection_thumbnail", "collections_list")

    /** Rows of a vertical post list: the Home feed, Saved's "All Posts", a profile's posts. */
    val POST_ROW = setOf("row_feed_profile_header", "row_feed_button_save", "row_feed_view_group_buttons")

    val PROFILE_HEADER = setOf("row_profile_header_imageview", "profile_header_familiar_followers_value")

    const val SETTINGS_TITLE = "Settings and activity"
    const val PROFILE_OPTIONS_DESC = "Options"
    const val SAVED_LABEL = "Saved"
    const val BLEND_DESC = "Blend"

    /**
     * Any of these on an unrecognised screen means it plays or grids media,
     * which is enough to block it. This is the default-deny net for surfaces
     * Instagram has not shipped yet.
     */
    val MEDIA_ID_PREFIXES = listOf(
        "clips_",
        "row_feed_",
        "reel_viewer_media",
        "reel_viewer_texture",
        "reel_viewer_video",
        "video_container",
        "media_group",
        "explore_",
        "grid_card",
    )
}
