package com.sidedoor.guard.classify

import com.sidedoor.guard.model.ScreenSnapshot

/**
 * Maps a snapshot to a [Screen]. Rules run in order and the first match wins;
 * the order encodes which signal is most trustworthy when several appear.
 */
object ScreenClassifier {

    fun classify(s: ScreenSnapshot): Screen = when {
        s.isEmpty -> Screen.UNKNOWN
        s.has(IgIds.CLIPS_VIEWER) -> classifyReel(s)
        s.hasAny(IgIds.THREAD) -> Screen.THREAD
        s.hasAny(IgIds.SAVED_HUB) -> Screen.SAVED_HUB
        else -> selectedTab(s)?.let(::screenForTab) ?: classifyOffTabs(s)
    }

    /** The tab currently highlighted, or null when the tab bar is not on screen. */
    fun selectedTab(s: ScreenSnapshot): String? {
        if (!s.has(IgIds.TAB_BAR)) return null
        return IgIds.TABS.firstOrNull { tab -> s.first(tab)?.let(s::isSelectedWithin) == true }
    }

    /** Friend's reel, then the Reels tab (even a saved reel there is still the tab), then your Saved. */
    private fun classifyReel(s: ScreenSnapshot): Screen = when {
        s.hasAny(IgIds.DM_REEL_REPLY) -> Screen.FRIEND_REEL
        selectedTab(s) == IgIds.TAB_REELS -> Screen.REELS_TAB
        isBookmarked(s) -> Screen.SAVED_REEL
        else -> Screen.SUGGESTED_REEL
    }

    private fun isBookmarked(s: ScreenSnapshot): Boolean {
        val bookmark = s.first(IgIds.SAVE_BUTTON) ?: return false
        return bookmark.isSelected || bookmark.description == IgIds.SAVED_STATE_DESC
    }

    private fun screenForTab(tab: String): Screen = when (tab) {
        IgIds.TAB_HOME -> Screen.HOME_FEED
        IgIds.TAB_REELS -> Screen.REELS_TAB
        IgIds.TAB_DIRECT -> Screen.INBOX
        IgIds.TAB_SEARCH -> Screen.EXPLORE
        else -> Screen.OWN_PROFILE
    }

    private fun classifyOffTabs(s: ScreenSnapshot): Screen = when {
        s.hasAny(IgIds.INBOX) -> Screen.INBOX
        s.hasAny(IgIds.PROFILE_HEADER) -> Screen.OTHER_PROFILE
        s.withLabel(IgIds.SETTINGS_TITLE) != null -> Screen.SETTINGS
        s.hasAny(IgIds.POST_ROW) -> Screen.POSTS_LIST
        showsMedia(s) -> Screen.UNKNOWN_MEDIA
        else -> Screen.UNKNOWN
    }

    private fun showsMedia(s: ScreenSnapshot): Boolean = s.nodes.any { node ->
        val id = node.viewId ?: return@any false
        IgIds.MEDIA_ID_PREFIXES.any(id::startsWith)
    }
}
