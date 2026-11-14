package com.sidedoor.guard.notify

import com.google.common.truth.Truth.assertThat
import com.sidedoor.guard.inbox.InboxRow
import org.junit.Test

class ReelQueueTest {

    private fun reel(sender: String, at: Long) = ReelEntry(sender, "sent you a reel", at)

    @Test
    fun `repeats of the same notification are collapsed`() {
        val q = ReelQueue().add(reel("Casey", 1)).add(reel("Casey", 1))
        assertThat(q.entries).hasSize(1)
    }

    @Test
    fun `newest reels come first`() {
        val q = ReelQueue().add(reel("Casey", 1)).add(reel("Riley", 3)).add(reel("Casey", 2))
        assertThat(q.entries.map { it.sentAt }).containsExactly(3L, 2L, 1L).inOrder()
    }

    @Test
    fun `watching marks the sender's oldest unwatched reel`() {
        val q = ReelQueue().add(reel("Casey", 1)).add(reel("Casey", 2)).markOneWatchedFrom("casey ")
        assertThat(q.unwatched.map { it.sentAt }).containsExactly(2L)
    }

    @Test
    fun `watching someone who sent nothing changes nothing`() {
        val q = ReelQueue().add(reel("Casey", 1))
        assertThat(q.markOneWatchedFrom("Riley")).isEqualTo(q)
    }

    @Test
    fun `summary counts who is waiting`() {
        val q = ReelQueue().add(reel("Casey", 1)).add(reel("Casey", 2)).add(reel("Riley", 3))
        assertThat(q.summary()).isEqualTo("Casey ×2 · Riley")
        assertThat(q.markAllWatched().summary()).isEmpty()
    }

    private fun row(thread: String, unread: Boolean, preview: String, ageMin: Long) =
        InboxRow(thread, unread, preview, ageMin * 60_000L)

    @Test
    fun `an unread reel in the inbox lines up, timed from its age`() {
        val q = ReelQueue().syncFromInbox(listOf(row("Alex", true, "Sent a reel by example_user", 37)), now = 100 * 60_000L)
        assertThat(q.unwatched.single()).isEqualTo(ReelEntry("Alex", "Sent a reel by example_user", 63 * 60_000L))
    }

    @Test
    fun `seeing the same waiting reel later does not add it twice`() {
        val first = ReelQueue().syncFromInbox(listOf(row("Alex", true, "Sent a reel by example_user", 37)), now = 100 * 60_000L)
        val later = first.syncFromInbox(listOf(row("Alex", true, "Sent a reel by example_user", 41)), now = 105 * 60_000L)
        assertThat(later.entries).hasSize(1)
    }

    @Test
    fun `a chat that is no longer unread has had its reels watched`() {
        val waiting = ReelQueue().syncFromInbox(listOf(row("Alex", true, "Sent a reel by example_user", 37)), now = 100 * 60_000L)
        val opened = waiting.syncFromInbox(listOf(row("Alex", false, "Sent a reel by example_user", 41)), now = 105 * 60_000L)
        assertThat(opened.unwatched).isEmpty()
    }

    @Test
    fun `read reels and other unread messages do not line up`() {
        val q = ReelQueue().syncFromInbox(
            listOf(row("Casey", false, "Sent a reel by someone", 120), row("Sam", true, "4 new messages", 32)),
            now = 1_000 * 60_000L,
        )
        assertThat(q.entries).isEmpty()
    }

    @Test
    fun `the queue is capped`() {
        var q = ReelQueue()
        repeat(ReelQueue.MAX_ENTRIES + 10) { q = q.add(reel("Casey", it.toLong())) }
        assertThat(q.entries).hasSize(ReelQueue.MAX_ENTRIES)
        assertThat(q.entries.first().sentAt).isEqualTo((ReelQueue.MAX_ENTRIES + 9).toLong())
    }
}
