package com.sidedoor.app.data

import android.app.PendingIntent
import android.content.Context
import android.util.Log
import com.sidedoor.guard.inbox.InboxRow
import com.sidedoor.guard.notify.ReelEntry
import com.sidedoor.guard.notify.ReelQueue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Persists the [ReelQueue]. The tap targets (Instagram's PendingIntents, which
 * open the exact chat) cannot be persisted, so they live in memory per sender;
 * after a restart an entry falls back to opening the inbox.
 */
class ReelInboxStore(context: Context) {

    private val prefs = context.getSharedPreferences("reel_inbox", Context.MODE_PRIVATE)
    private val openers = mutableMapOf<String, PendingIntent>()

    private val _queue = MutableStateFlow(load())
    val queue: StateFlow<ReelQueue> = _queue.asStateFlow()

    @Synchronized
    fun add(entry: ReelEntry, opener: PendingIntent?) {
        opener?.let { openers[entry.sender.lowercase()] = it }
        update(_queue.value.add(entry))
    }

    @Synchronized
    fun syncFromInbox(rows: List<InboxRow>, now: Long) = update(_queue.value.syncFromInbox(rows, now))

    @Synchronized
    fun markOneWatchedFrom(sender: String) = update(_queue.value.markOneWatchedFrom(sender))

    @Synchronized
    fun markAllWatched() = update(_queue.value.markAllWatched())

    fun openerFor(sender: String): PendingIntent? = openers[sender.lowercase()]

    private fun update(next: ReelQueue) {
        if (next == _queue.value) return
        _queue.value = next
        prefs.edit().putString(KEY, encode(next)).apply()
    }

    private fun load(): ReelQueue = try {
        prefs.getString(KEY, null)?.let(::decode) ?: ReelQueue()
    } catch (e: JSONException) {
        Log.w(TAG, "Reel inbox unreadable, starting empty", e)
        ReelQueue()
    }

    private fun encode(q: ReelQueue): String = JSONArray().apply {
        q.entries.forEach { e ->
            put(
                JSONObject()
                    .put("sender", e.sender)
                    .put("text", e.text)
                    .put("sentAt", e.sentAt)
                    .put("watched", e.watched)
            )
        }
    }.toString()

    private fun decode(raw: String): ReelQueue {
        val array = JSONArray(raw)
        val entries = (0 until array.length()).map { i ->
            val o = array.getJSONObject(i)
            ReelEntry(o.getString("sender"), o.getString("text"), o.getLong("sentAt"), o.getBoolean("watched"))
        }
        return ReelQueue(entries)
    }

    private companion object {
        const val TAG = "SidedoorReels"
        const val KEY = "queue"
    }
}
