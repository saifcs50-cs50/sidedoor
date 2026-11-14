package com.sidedoor.app.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate

/** Totals for the last seven days, shown on the Front Door. */
data class WeekStats(
    val instagramMinutes: Int = 0,
    val friendReels: Int = 0,
    val doorsClosed: Int = 0,
)

/**
 * Daily counters in SharedPreferences. Time in Instagram arrives in small
 * increments many times a second, so it is buffered and written in chunks.
 */
class StatsStore(context: Context) {

    private val prefs = context.getSharedPreferences("stats", Context.MODE_PRIVATE)
    private var bufferedMs = 0L

    private val _week = MutableStateFlow(readWeek())
    val week: StateFlow<WeekStats> = _week.asStateFlow()

    @Synchronized
    fun addInstagramTime(ms: Long) {
        bufferedMs += ms
        if (bufferedMs >= FLUSH_EVERY_MS) flush()
    }

    @Synchronized
    fun flush() {
        if (bufferedMs == 0L) return
        increment(KEY_IG_MS, bufferedMs)
        bufferedMs = 0
    }

    fun countFriendReel() = increment(KEY_FRIEND_REELS, 1)

    fun countDoorClosed() = increment(KEY_DOORS, 1)

    @Synchronized
    private fun increment(key: String, by: Long) {
        val dayKey = "${LocalDate.now()}:$key"
        prefs.edit().putLong(dayKey, prefs.getLong(dayKey, 0) + by).apply()
        _week.value = readWeek()
    }

    private fun readWeek(): WeekStats {
        val days = (0L until 7L).map { LocalDate.now().minusDays(it).toString() }
        fun sum(key: String) = days.sumOf { prefs.getLong("$it:$key", 0) }
        return WeekStats(
            instagramMinutes = (sum(KEY_IG_MS) / 60_000).toInt(),
            friendReels = sum(KEY_FRIEND_REELS).toInt(),
            doorsClosed = sum(KEY_DOORS).toInt(),
        )
    }

    private companion object {
        const val FLUSH_EVERY_MS = 15_000L
        const val KEY_IG_MS = "igMs"
        const val KEY_FRIEND_REELS = "friendReels"
        const val KEY_DOORS = "doors"
    }
}
