package com.sidedoor.guard.notify

import com.sidedoor.guard.inbox.InboxRow

/** One reel a friend sent, as learned from their DM notification. */
data class ReelEntry(
    val sender: String,
    val text: String,
    val sentAt: Long,
    val watched: Boolean = false,
) {
    /** Instagram re-posts a chat's notification with every new message; this collapses the repeats. */
    val key: String get() = "$sender|$sentAt|$text"
}

/**
 * The Reel Inbox: a finite list of reels friends sent, newest first.
 * Algorithmic feeds never end; this one does.
 */
data class ReelQueue(val entries: List<ReelEntry> = emptyList()) {

    val unwatched: List<ReelEntry> get() = entries.filterNot { it.watched }

    fun add(entry: ReelEntry): ReelQueue {
        if (entries.any { it.key == entry.key }) return this
        return ReelQueue((entries + entry).sortedByDescending { it.sentAt }.take(MAX_ENTRIES))
    }

    /**
     * Mirrors what the inbox shows: an unread "Sent a reel" row is a reel
     * waiting; a chat that is no longer unread has been opened, so its reels
     * are no longer waiting. Needs no notification access at all.
     *
     * A row's age ticks on ("37m", then "41m"), so a reel already waiting from
     * that chat with that text is the same reel whatever its computed time.
     * (Notification entries are not merged this way: their text is often just
     * "sent you a reel", identical for different reels.)
     */
    fun syncFromInbox(rows: List<InboxRow>, now: Long): ReelQueue = rows.fold(this) { q, row ->
        when {
            row.isWaitingReel && !q.isWaiting(row.thread, row.preview) ->
                q.add(ReelEntry(row.thread, row.preview, now - (row.ageMs ?: 0L)))
            !row.unread -> q.markAllWatchedFrom(row.thread)
            else -> q
        }
    }

    private fun isWaiting(sender: String, text: String): Boolean =
        entries.any { !it.watched && it.sender == sender && it.text == text }

    fun markAllWatchedFrom(sender: String): ReelQueue {
        if (entries.none { !it.watched && it.sender == sender }) return this
        return ReelQueue(entries.map { if (it.sender == sender) it.copy(watched = true) else it })
    }

    /**
     * Opening a friend's reel marks their oldest unwatched one as seen: the
     * viewer names who sent it but not which reel, and people watch a chat's
     * reels in order.
     */
    fun markOneWatchedFrom(sender: String): ReelQueue {
        val target = entries
            .filter { !it.watched && it.sender.equals(sender.trim(), ignoreCase = true) }
            .minByOrNull { it.sentAt } ?: return this
        return ReelQueue(entries.map { if (it.key == target.key) it.copy(watched = true) else it })
    }

    fun markAllWatched(): ReelQueue = ReelQueue(entries.map { it.copy(watched = true) })

    /** "Casey ×2 · Riley" - who is waiting on you, most reels first. */
    fun summary(): String = unwatched
        .groupingBy { it.sender }
        .eachCount()
        .entries
        .sortedByDescending { it.value }
        .joinToString(" · ") { (sender, count) -> if (count > 1) "$sender ×$count" else sender }

    companion object {
        const val MAX_ENTRIES = 50
    }
}
