package com.sidedoor.app.ui

import com.sidedoor.app.data.WeekStats
import com.sidedoor.guard.notify.ReelEntry
import com.sidedoor.guard.notify.ReelQueue

data class Setup(
    val instagramInstalled: Boolean = true,
    val guardOn: Boolean = false,
    val reelsOn: Boolean = false,
    val alertsOn: Boolean = false,
) {
    val complete: Boolean get() = guardOn && reelsOn && alertsOn
}

data class FrontDoorState(
    val setup: Setup = Setup(),
    val guardRunning: Boolean = false,
    val week: WeekStats = WeekStats(),
    val reels: ReelQueue = ReelQueue(),
    val instagramChangedOn: String? = null,
)

/** Everything the screen can ask for. Implemented by [MainActivity]. */
interface FrontDoorActions {
    fun openChats()
    fun openSaved()
    fun openReel(entry: ReelEntry)
    fun markAllWatched()
    fun turnOnGuard()
    fun turnOnReels()
    fun allowAlerts()
}
