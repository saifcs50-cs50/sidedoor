package com.sidedoor.guard.canary

import com.sidedoor.guard.classify.Screen

enum class CanaryResult { Pending, Passed, Failed }

/**
 * The cheapest reliable test that Sidedoor still understands Instagram.
 *
 * The Chats button opens `instagram://direct-inbox`, which always lands on the
 * inbox. If the guard does not recognise an inbox within a few seconds of
 * that, Instagram has changed its screens and the rules need updating - which
 * is exactly what an Instagram update breaks first.
 */
class InboxCanary private constructor(private val armedAt: Long) {

    fun onScreen(screen: Screen, now: Long): CanaryResult = when {
        screen == Screen.INBOX -> CanaryResult.Passed
        else -> onTick(now)
    }

    fun onTick(now: Long): CanaryResult =
        if (now - armedAt > DEADLINE_MS) CanaryResult.Failed else CanaryResult.Pending

    companion object {
        /** Cold start measured at ~1.5 s on the test phone; this leaves room for a slow day. */
        const val DEADLINE_MS = 6_000L

        fun arm(now: Long) = InboxCanary(armedAt = now)
    }
}
