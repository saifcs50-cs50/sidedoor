package com.sidedoor.guard.policy

/** A concrete thing the service does to Instagram. */
sealed interface Action {
    data object Back : Action
    data object TapInboxTab : Action
    data object OpenInboxLink : Action
    data object GoHome : Action
}

/**
 * One continuous stretch of time spent on a blocked screen.
 * [attempts] counts actions already taken; [lastActionAt] rate-limits them.
 */
data class Episode(
    val correction: Correction,
    val startedAt: Long,
    val attempts: Int = 0,
    val lastActionAt: Long? = null,
) {
    companion object {
        fun start(correction: Correction, now: Long) = Episode(correction, startedAt = now)
    }
}

/**
 * Escalation for a screen that will not go away. The screen's own correction
 * is tried a few times; if Instagram is still showing it, the guard opens the
 * inbox by deep link; if even that fails, it leaves Instagram entirely. Failing
 * closed like this is annoying at worst, which is the right way round.
 */
class CorrectionLadder {

    fun next(episode: Episode, now: Long): Pair<Action?, Episode> {
        val last = episode.lastActionAt
        if (last != null && now - last < RETRY_MS) return null to episode

        val action = when {
            episode.attempts < OWN_ATTEMPTS -> own(episode.correction)
            episode.attempts < OWN_ATTEMPTS + LINK_ATTEMPTS -> Action.OpenInboxLink
            else -> Action.GoHome
        }
        return action to episode.copy(attempts = episode.attempts + 1, lastActionAt = now)
    }

    private fun own(correction: Correction): Action = when (correction) {
        Correction.BACK -> Action.Back
        Correction.GO_TO_INBOX -> Action.TapInboxTab
    }

    companion object {
        /** Long enough for Instagram to finish the transition the last action started. */
        const val RETRY_MS = 700L
        const val OWN_ATTEMPTS = 3
        const val LINK_ATTEMPTS = 1
    }
}
