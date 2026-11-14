package com.sidedoor.guard.policy

import com.sidedoor.guard.classify.IgIds
import com.sidedoor.guard.model.Bounds
import com.sidedoor.guard.model.ScreenSnapshot

/** How a dead zone looks. It eats touches either way. */
enum class Paint {
    /** Instagram's tab-bar colour: the covered tab reads as an empty slot. */
    TAB_BAR,

    /** Invisible: the content shows through but cannot be swiped. */
    CLEAR,
}

data class Zone(val bounds: Bounds, val paint: Paint)

/**
 * Rectangles to cover with touch-eating overlays, so the doors to the feed
 * are simply not there. Blocking after the fact still works, but a door that
 * does nothing when pushed never shows you a single frame.
 */
object DeadZonePlanner {

    private const val STORY_AVATAR_PREFIX = "Open story of"
    private const val REEL_ACTION_BAR = "clips_viewer_action_bar"
    private const val REEL_BUTTONS = "clips_ufi_component"
    private const val BOTTOM_SHEET = "bottom_sheet_container"

    fun plan(s: ScreenSnapshot): List<Zone> =
        (tabZones(s) + blendZones(s) + storyZones(s) + reelLock(s)).distinct()

    private fun tabZones(s: ScreenSnapshot): List<Zone> {
        if (!s.has(IgIds.TAB_BAR)) return emptyList()
        return IgIds.FORBIDDEN_TABS.mapNotNull { s.first(it)?.bounds }.map { Zone(it, Paint.TAB_BAR) }
    }

    private fun blendZones(s: ScreenSnapshot): List<Zone> =
        s.nodes.filter { it.description == IgIds.BLEND_DESC }.map { Zone(it.bounds, Paint.TAB_BAR) }

    /**
     * Story avatars - on inbox rows and in a chat's header. Stories are not on
     * the list, and opening one tells your friend you watched it, so the tap
     * never reaches Instagram. Clear, so the avatar still shows.
     */
    private fun storyZones(s: ScreenSnapshot): List<Zone> = s.nodes
        .filter { it.description?.startsWith(STORY_AVATAR_PREFIX) == true }
        .map { Zone(it.bounds, Paint.CLEAR) }
        .distinctBy { it.bounds }

    /**
     * On a friend's reel, the video itself becomes unswipeable: a clear zone
     * over the pager, below its top bar and left of the like/comment/share/save
     * column. That turns "one reel per tap" from a correction into a fact.
     * Skipped while a sheet (comments, share) is up, so the sheet stays usable.
     */
    private fun reelLock(s: ScreenSnapshot): List<Zone> {
        if (!s.has(IgIds.CLIPS_VIEWER) || !s.hasAny(IgIds.DM_REEL_REPLY) || s.has(BOTTOM_SHEET)) return emptyList()
        val pager = s.first(IgIds.CLIPS_VIEWER)!!.bounds
        val top = s.first(REEL_ACTION_BAR)?.bounds?.bottom ?: pager.top
        val right = s.first(REEL_BUTTONS)?.bounds?.left ?: return emptyList()
        val lock = Bounds(pager.left, top, right, pager.bottom)
        return if (lock.isEmpty) emptyList() else listOf(Zone(lock, Paint.CLEAR))
    }
}
