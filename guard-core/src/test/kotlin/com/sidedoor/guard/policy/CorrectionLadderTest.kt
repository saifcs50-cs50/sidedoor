package com.sidedoor.guard.policy

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CorrectionLadderTest {

    private val ladder = CorrectionLadder()

    @Test
    fun `first sighting applies the screen's own correction immediately`() {
        val (action, _) = ladder.next(Episode.start(Correction.BACK, now = 0), now = 0)
        assertThat(action).isEqualTo(Action.Back)
    }

    @Test
    fun `go to inbox taps the messages tab`() {
        val (action, _) = ladder.next(Episode.start(Correction.GO_TO_INBOX, now = 0), now = 0)
        assertThat(action).isEqualTo(Action.TapInboxTab)
    }

    @Test
    fun `nothing is repeated faster than the retry interval`() {
        val (_, afterFirst) = ladder.next(Episode.start(Correction.BACK, now = 0), now = 0)
        val (action, _) = ladder.next(afterFirst, now = CorrectionLadder.RETRY_MS - 1)
        assertThat(action).isNull()
    }

    @Test
    fun `a stuck screen escalates to the inbox deep link and then home`() {
        var episode = Episode.start(Correction.BACK, now = 0)
        val actions = mutableListOf<Action>()
        var now = 0L
        repeat(8) {
            val (action, next) = ladder.next(episode, now)
            action?.let(actions::add)
            episode = next
            now += CorrectionLadder.RETRY_MS
        }
        assertThat(actions.take(CorrectionLadder.OWN_ATTEMPTS)).containsExactlyElementsIn(
            List(CorrectionLadder.OWN_ATTEMPTS) { Action.Back }
        )
        assertThat(actions).contains(Action.OpenInboxLink)
        assertThat(actions.last()).isEqualTo(Action.GoHome)
        assertThat(actions.indexOf(Action.OpenInboxLink)).isLessThan(actions.indexOf(Action.GoHome))
    }
}
