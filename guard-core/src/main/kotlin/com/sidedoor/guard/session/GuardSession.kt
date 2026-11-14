package com.sidedoor.guard.session

/**
 * A guarded visit to Instagram.
 *
 * The guard is opt-in per visit: a session starts only when Sidedoor opens
 * Instagram (Chats, Saved, a reel from the list, a home-screen shortcut).
 * Opening Instagram any other way - its own icon, a notification - is left
 * completely alone.
 *
 * A session ends when you go to the home screen (or back to Sidedoor). A
 * short detour into another app - the photo picker, sharing to a chat - keeps
 * it, so it does not drop out from under you mid-conversation. Being covered
 * by the shade or the lock screen is not leaving at all.
 */
data class GuardSession(val startedAt: Long, val awaySince: Long? = null) {

    /**
     * Instagram is no longer in front. [frontApp] is the app that is, or null
     * when a system window (shade, lock screen) covers everything.
     * Returns null when the session is over.
     */
    fun leftFor(frontApp: String?, enders: Set<String>, now: Long): GuardSession? = when {
        frontApp == null -> this
        frontApp in enders -> null
        else -> copy(awaySince = awaySince ?: now)
    }

    /** Instagram is back in front. Returns null if the detour ran too long. */
    fun returned(now: Long): GuardSession? = when {
        awaySince == null -> this
        isOverdue(now) -> null
        else -> copy(awaySince = null)
    }

    /** Away longer than a detour is allowed to take. */
    fun isOverdue(now: Long): Boolean = awaySince?.let { now - it > DETOUR_MS } ?: false

    companion object {
        /** Long enough to pick a photo or share to a chat and come back. */
        const val DETOUR_MS = 60_000L

        fun start(now: Long) = GuardSession(startedAt = now)
    }
}
