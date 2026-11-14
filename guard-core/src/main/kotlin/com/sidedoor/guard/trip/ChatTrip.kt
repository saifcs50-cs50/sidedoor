package com.sidedoor.guard.trip

import com.sidedoor.guard.classify.IgIds
import com.sidedoor.guard.classify.Screen
import com.sidedoor.guard.inbox.InboxRows
import com.sidedoor.guard.model.ScreenSnapshot

/**
 * Opens one chat from Sidedoor's reels list: the inbox deep link, then a tap
 * on that chat's row. Arrives only in the right chat - its header must match -
 * so a chat Instagram happened to have open is backed out of, not mistaken for
 * the target. If the row is not on screen, the trip stops at the inbox.
 */
object ChatTrip {

    private const val HEADER_TITLE = "header_title"

    fun next(screen: Screen, s: ScreenSnapshot, thread: String): TripStep = when (screen) {
        Screen.THREAD -> {
            val header = s.first(HEADER_TITLE)?.text
            if (header == null || header == thread) TripStep.Arrived else TripStep.Back
        }
        Screen.INBOX -> InboxRows.parse(s).firstOrNull { it.thread == thread }
            ?.let { TripStep.Tap(Target.ByBounds(it.bounds)) }
            ?: TripStep.Arrived
        Screen.FRIEND_REEL, Screen.SAVED_REEL, Screen.POSTS_LIST -> TripStep.Back
        else -> if (s.has(IgIds.TAB_BAR)) TripStep.Tap(Target.ById(IgIds.TAB_DIRECT)) else TripStep.Wait
    }
}
