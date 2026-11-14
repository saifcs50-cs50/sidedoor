package com.sidedoor.guard.inbox

import com.google.common.truth.Truth.assertThat
import com.sidedoor.guard.model.Bounds
import com.sidedoor.guard.support.SnapshotBuilder
import com.sidedoor.guard.support.snapshot
import org.junit.Test

/** An inbox row as the test phone reports it: one wide described node, with the name label inside it. */
fun SnapshotBuilder.inboxRow(description: String, top: Int, name: String? = description.substringBefore(", ")) {
    desc(description, Bounds(0, top, 720, top + 142))
    name?.let { label(it, Bounds(168, top + 40, 400, top + 71)) }
}

class InboxRowsTest {

    /** Row descriptions exactly as the test phone reported them on 2026-09-28 (names changed). */
    private val inbox = snapshot {
        id("inbox_refreshable_thread_list_recyclerview")
        label("Messages", Bounds(30, 596, 151, 632))
        inboxRow("Sam, unread, 4 new messages ·, 32m", 644)
        inboxRow("Alex 🤍, unread, Sent a reel by example_user ·, 37m", 786)
        inboxRow("Jordan 💅, Sent 37m ago", 928)
        inboxRow("The Example Group, Seen by all", 1070)
        inboxRow("Casey, Sent a reel by someone ·, 2h", 1212)
    }

    @Test
    fun `rows are read with their thread, unread state, preview and place`() {
        val rows = InboxRows.parse(inbox)
        assertThat(rows.map { it.thread }).containsExactly("Sam", "Alex 🤍", "Jordan 💅", "The Example Group", "Casey").inOrder()
        assertThat(rows.first { it.thread == "Alex 🤍" }).isEqualTo(
            InboxRow("Alex 🤍", unread = true, preview = "Sent a reel by example_user", ageMs = 37 * 60_000L, bounds = Bounds(0, 786, 720, 928))
        )
    }

    @Test
    fun `only an unread reel is a reel waiting`() {
        val waiting = InboxRows.parse(inbox).filter { it.isWaitingReel }
        assertThat(waiting.map { it.thread }).containsExactly("Alex 🤍")
    }

    @Test
    fun `a group named after its members keeps its whole name`() {
        val s = snapshot { inboxRow("Casey, Riley, unread, Sent a reel by x ·, 5m", 644, name = "Casey, Riley") }
        val row = InboxRows.parse(s).single()
        assertThat(row.thread).isEqualTo("Casey, Riley")
        assertThat(row.isWaitingReel).isTrue()
    }

    @Test
    fun `without a name label the description is split at the first comma`() {
        val s = snapshot { inboxRow("Alex, unread, Sent a reel by x ·, 5m", 644, name = null) }
        assertThat(InboxRows.parse(s).single().thread).isEqualTo("Alex")
    }

    @Test
    fun `ages parse from minutes to weeks`() {
        assertThat(InboxRows.parseAge("now")).isEqualTo(0L)
        assertThat(InboxRows.parseAge("5m")).isEqualTo(5 * 60_000L)
        assertThat(InboxRows.parseAge("2h")).isEqualTo(2 * 3_600_000L)
        assertThat(InboxRows.parseAge("3d")).isEqualTo(3 * 86_400_000L)
        assertThat(InboxRows.parseAge("1w")).isEqualTo(7 * 86_400_000L)
        assertThat(InboxRows.parseAge("null")).isNull()
    }

    @Test
    fun `narrow nodes and non-row labels are ignored`() {
        val s = snapshot {
            desc("Open story of someone", Bounds(35, 666, 133, 764))
            label("Search or ask Meta AI", Bounds(120, 218, 690, 254))
        }
        assertThat(InboxRows.parse(s)).isEmpty()
    }
}
