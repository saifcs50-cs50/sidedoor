package com.sidedoor.guard.support

import com.sidedoor.guard.model.Bounds

/**
 * Synthetic versions of the video screens recorded on the test phone with
 * `dumpsys activity top` (IDs and layout are from that dump; uiautomator
 * cannot capture them while a video plays).
 */
object IgScreens {

    /** A reel a friend sent, opened from the chat: the "Reply to <friend>" bar is the tell. */
    fun friendReel() = snapshot {
        clipsViewer()
        id("sender_username_or_fullname", text = "someone")
        id("reel_viewer_message_composer", bounds = Bounds(15, 1463, 705, 1557))
        id("reply_bar_edittext", text = "Reply to someone", bounds = Bounds(56, 1463, 411, 1557))
    }

    /** One swipe past a friend's reel: Instagram's own pick, offered with "Send to chat". */
    fun suggestedReel() = snapshot {
        clipsViewer()
        id("clips_viewer_action_bar_title", text = "Suggested")
        id("pill_container", bounds = Bounds(232, 1470, 488, 1552))
        id("send_cta", text = "Send to chat", bounds = Bounds(310, 1490, 470, 1535))
    }

    fun reelsTab(bookmark: String = "Save") = snapshot {
        clipsViewer(bookmark)
        tabBar(selected = "clips_tab")
    }

    fun savedHub() = snapshot {
        id("saved_view_pager")
        id("saved_tab_layout", bounds = Bounds(0, 180, 720, 250))
        id("saved_collection_thumbnail", bounds = Bounds(30, 344, 135, 449))
        id("image_button", bounds = Bounds(0, 683, 238, 1000))
    }

    /** Saved items open as a finite list with the same row IDs as the Home feed, but no tab bar. */
    fun savedFeed() = snapshot {
        id("action_bar_title", text = "All Posts", bounds = Bounds(0, 64, 720, 169))
        feedRow()
    }

    /** Posts reached any other way: a stranger's grid, a hashtag, a suggested chain. */
    fun postsFeed() = snapshot {
        id("action_bar_title", text = "Posts", bounds = Bounds(0, 64, 720, 169))
        feedRow()
    }

    /** A reel from your Saved, opened full screen: same viewer, bookmark selected. */
    fun savedReel() = snapshot { clipsViewer(bookmarked = true) }

    private fun SnapshotBuilder.clipsViewer(bookmark: String = "Save", bookmarked: Boolean = false) {
        id("clips_viewer_container")
        id("clips_viewer_view_pager")
        id("clips_video_container")
        id("like_button", bounds = Bounds(639, 700, 690, 750))
        id("save_button", desc = bookmark, bounds = Bounds(639, 1212, 690, 1262), selected = bookmarked)
    }

    private fun SnapshotBuilder.feedRow() {
        id("row_feed_profile_header", bounds = Bounds(0, 169, 720, 267))
        id("row_feed_photo_profile_name", text = "someone", bounds = Bounds(92, 169, 637, 216))
        id("row_feed_button_like", bounds = Bounds(20, 1100, 70, 1150))
        id("row_feed_button_save", bounds = Bounds(650, 1100, 700, 1150))
    }
}
