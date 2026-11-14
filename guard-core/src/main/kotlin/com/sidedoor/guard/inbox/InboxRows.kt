package com.sidedoor.guard.inbox

import com.sidedoor.guard.model.Bounds
import com.sidedoor.guard.model.ScreenSnapshot
import com.sidedoor.guard.model.UiNode

/** One chat in the inbox list, as its row describes itself. */
data class InboxRow(
    val thread: String,
    val unread: Boolean,
    val preview: String,
    /** How long ago the last message arrived, when the row says. */
    val ageMs: Long?,
    /** Where the row is on screen, so a trip can tap exactly this one. */
    val bounds: Bounds = Bounds.EMPTY,
) {
    /** A reel a friend sent that you have not opened yet. */
    val isWaitingReel: Boolean get() = unread && preview.contains(REEL_SHARE, ignoreCase = true)

    private companion object {
        const val REEL_SHARE = "sent a reel"
    }
}

/**
 * Reads the inbox rows. Instagram gives each row one description, recorded on
 * the test phone as "<thread>, [unread, ]<preview>[ ·, <age>]", for example
 * "Alex, unread, Sent a reel by example_user ·, 37m". English only, like the
 * Saved trip's labels.
 *
 * The thread name is taken from the row's own name label where there is one:
 * a group Instagram names after its members ("Casey, Riley") has commas in its
 * name, and splitting the description at the first comma would read it as
 * "Casey".
 */
object InboxRows {

    /** Rows span the list; anything narrower is an avatar, a button, or a label inside a row. */
    private const val MIN_ROW_WIDTH_FRACTION = 0.9
    private val ROW = Regex("""^(.+?), (.+)$""")
    private val REST = Regex("""^(unread, )?(.+?)(?: ·, (\S+))?$""")
    private val AGE = Regex("""^(\d+)([mhdw])$""")

    fun parse(s: ScreenSnapshot): List<InboxRow> {
        val width = s.nodes.maxOfOrNull { it.bounds.right } ?: return emptyList()
        return s.nodes
            .filter { it.bounds.width >= width * MIN_ROW_WIDTH_FRACTION && it.description != null }
            .mapNotNull { row -> parseRow(row, nameIn(s, row)) }
            .distinctBy { it.thread }
    }

    /** The topmost text drawn inside the row: the chat's name. */
    private fun nameIn(s: ScreenSnapshot, row: UiNode): String? = s.nodes
        .filter { it.text != null && it.bounds != row.bounds && it.bounds in row.bounds }
        .minWithOrNull(compareBy({ it.bounds.top }, { it.bounds.left }))
        ?.text

    private fun parseRow(row: UiNode, name: String?): InboxRow? {
        val desc = row.description ?: return null
        val (thread, rest) = if (name != null && desc.startsWith("$name, ")) {
            name to desc.removePrefix("$name, ")
        } else {
            val m = ROW.matchEntire(desc) ?: return null
            m.groupValues[1] to m.groupValues[2]
        }
        val parts = REST.matchEntire(rest) ?: return null
        val (unread, preview, age) = parts.destructured
        return InboxRow(thread, unread.isNotEmpty(), preview, parseAge(age), row.bounds)
    }

    fun parseAge(age: String): Long? {
        if (age == "now") return 0L
        val m = AGE.matchEntire(age) ?: return null
        val n = m.groupValues[1].toLong()
        return n * when (m.groupValues[2]) {
            "m" -> 60_000L
            "h" -> 3_600_000L
            "d" -> 86_400_000L
            else -> 7 * 86_400_000L
        }
    }
}
