package com.sidedoor.guard.policy

import com.sidedoor.guard.classify.Screen

/**
 * What the guard remembers between screens.
 *
 * [previous] is the last *recognised, allowed* screen. Unknown screens (sheets,
 * pickers, dialogs) and blocked screens are transparent: they neither set nor
 * clear it, because they are either incidental or about to be backed out of.
 */
data class GuardState(
    val previous: Screen? = null,
    val postsUnlocked: Boolean = false,
)

data class Decision(val verdict: Verdict, val next: GuardState)

/**
 * The allowlist. Chats, reels friends sent, Saved and the route to Saved are
 * in; everything else is out. Post lists are the one context-dependent case:
 * they are the same screen whether they came from Saved or from a stranger's
 * profile, so they are allowed only when entered directly from Saved or a chat.
 */
object GuardPolicy {

    private val ALWAYS_ALLOWED = setOf(
        Screen.INBOX,
        Screen.THREAD,
        Screen.FRIEND_REEL,
        Screen.OWN_PROFILE,
        Screen.SETTINGS,
        Screen.SAVED_HUB,
    )

    /** Screens a post list may be opened from. */
    private val POST_ENTRY_POINTS = setOf(Screen.SAVED_HUB, Screen.THREAD)

    fun decide(state: GuardState, screen: Screen): Decision = when (screen) {
        in ALWAYS_ALLOWED -> Decision(Verdict.Allow, GuardState(previous = screen))
        // Transparent: opening a saved reel full screen and backing out must
        // land you on the same Saved list, still unlocked.
        Screen.SAVED_REEL, Screen.UNKNOWN -> Decision(Verdict.Allow, state)
        Screen.POSTS_LIST -> decidePosts(state)
        else -> Decision(block(screen, state), state)
    }

    private fun decidePosts(state: GuardState): Decision {
        val enteredDirectly = state.previous in POST_ENTRY_POINTS
        val stillScrolling = state.previous == Screen.POSTS_LIST && state.postsUnlocked
        return if (enteredDirectly || stillScrolling) {
            Decision(Verdict.Allow, GuardState(previous = Screen.POSTS_LIST, postsUnlocked = true))
        } else {
            Decision(block(Screen.POSTS_LIST, state), state)
        }
    }

    /** Past a friend's reel it is the end of what they sent; anywhere else, it is simply not yours. */
    private fun reelReason(state: GuardState): BlockReason =
        if (state.previous == Screen.FRIEND_REEL || state.previous == Screen.THREAD) {
            BlockReason.END_OF_FRIEND_REELS
        } else {
            BlockReason.NOT_SAVED
        }

    private fun block(screen: Screen, state: GuardState): Verdict.Block = when (screen) {
        Screen.HOME_FEED -> Verdict.Block(screen, BlockReason.FEED, Correction.GO_TO_INBOX)
        Screen.REELS_TAB -> Verdict.Block(screen, BlockReason.REELS_TAB, Correction.GO_TO_INBOX)
        Screen.EXPLORE -> Verdict.Block(screen, BlockReason.EXPLORE, Correction.GO_TO_INBOX)
        Screen.SUGGESTED_REEL -> Verdict.Block(screen, reelReason(state), Correction.BACK)
        Screen.OTHER_PROFILE -> Verdict.Block(screen, BlockReason.PROFILE, Correction.BACK)
        Screen.POSTS_LIST -> Verdict.Block(screen, BlockReason.STRAY_POSTS, Correction.BACK)
        else -> Verdict.Block(screen, BlockReason.UNKNOWN_MEDIA, Correction.BACK)
    }
}
