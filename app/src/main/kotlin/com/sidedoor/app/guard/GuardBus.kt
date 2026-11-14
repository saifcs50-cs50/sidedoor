package com.sidedoor.app.guard

import com.sidedoor.guard.trip.TripGoal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Requests from the Front Door screen to the guard service, and the guard's
 * status back. Requests expire so a stale tap cannot fire minutes later.
 *
 * A session request is what makes a visit guarded at all: Instagram opened
 * any way other than through Sidedoor never makes one, and is left alone.
 */
class GuardBus {

    private val _running = MutableStateFlow(false)
    val running: StateFlow<Boolean> = _running.asStateFlow()

    /** Set when Instagram stops looking the way the rules expect. Holds the Instagram version. */
    private val _canaryFailure = MutableStateFlow<String?>(null)
    val canaryFailure: StateFlow<String?> = _canaryFailure.asStateFlow()

    @Volatile private var sessionAt: Long? = null
    @Volatile private var trip: Pair<TripGoal, Long>? = null
    @Volatile private var canaryAt: Long? = null

    fun setRunning(running: Boolean) {
        _running.value = running
    }

    fun requestSession(now: Long) {
        sessionAt = now
    }

    fun hasPendingSession(now: Long): Boolean = take(sessionAt, now)

    fun takeSession(now: Long): Boolean = take(sessionAt, now).also { if (it) sessionAt = null }

    fun requestTrip(goal: TripGoal, now: Long) {
        trip = goal to now
    }

    fun takeTrip(now: Long): TripGoal? = trip?.takeIf { take(it.second, now) }?.first.also { trip = null }

    fun requestCanary(now: Long) {
        canaryAt = now
    }

    fun takeCanary(now: Long): Long? = canaryAt?.takeIf { take(it, now) }.also { canaryAt = null }

    fun reportCanary(failedOnVersion: String?) {
        _canaryFailure.value = failedOnVersion
    }

    private fun take(at: Long?, now: Long): Boolean = at != null && now - at < REQUEST_TTL_MS

    private companion object {
        const val REQUEST_TTL_MS = 10_000L
    }
}
