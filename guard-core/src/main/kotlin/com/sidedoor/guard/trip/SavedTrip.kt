package com.sidedoor.guard.trip

import com.sidedoor.guard.classify.IgIds
import com.sidedoor.guard.classify.Screen
import com.sidedoor.guard.model.Bounds
import com.sidedoor.guard.model.ScreenSnapshot

/** Something on screen to tap: by view ID, by its exact label, or by exactly where it is. */
sealed interface Target {
    data class ById(val viewId: String) : Target
    data class ByLabel(val label: String) : Target
    data class ByBounds(val bounds: Bounds) : Target
}

/** Where a trip is headed. */
sealed interface TripGoal {
    data object Saved : TripGoal

    /** A chat, by the name its inbox row and header show. */
    data class Chat(val thread: String) : TripGoal
}

/** One hop toward [goal] from the current screen. */
fun nextStep(goal: TripGoal, screen: Screen, s: ScreenSnapshot): TripStep = when (goal) {
    TripGoal.Saved -> SavedTrip.next(screen, s)
    is TripGoal.Chat -> ChatTrip.next(screen, s, goal.thread)
}

sealed interface TripStep {
    data class Tap(val target: Target) : TripStep
    data object Back : TripStep
    data object Wait : TripStep
    data object Arrived : TripStep
}

/**
 * The Saved button. Instagram 448 registers an `instagram://saved` link but
 * every variant lands on Home, so the guard walks the real route instead:
 * Profile tab -> Options -> Saved, one hop per screen, behind the shield.
 */
object SavedTrip {

    fun next(screen: Screen, s: ScreenSnapshot): TripStep = when (screen) {
        Screen.SAVED_HUB -> TripStep.Arrived
        Screen.SETTINGS -> TripStep.Tap(Target.ByLabel(IgIds.SAVED_LABEL))
        Screen.OWN_PROFILE -> TripStep.Tap(Target.ByLabel(IgIds.PROFILE_OPTIONS_DESC))
        Screen.THREAD, Screen.FRIEND_REEL, Screen.SAVED_REEL, Screen.POSTS_LIST -> TripStep.Back
        else -> if (s.has(IgIds.TAB_BAR)) TripStep.Tap(Target.ById(IgIds.TAB_PROFILE)) else TripStep.Wait
    }
}
