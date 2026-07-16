package com.julieasoreng.touchgrass.data.goals

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.time.Instant
import java.time.ZoneId
import java.util.Calendar
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private val Context.goalsDataStore by preferencesDataStore(name = "goals_prefs")

data class CompletedSession(
    val goalId: String,
    val minutes: Int,
    val completedAtEpochMillis: Long
)

/** Persists a timestamped record of every completed focus session, so weekly totals, per-activity
 *  breakdowns, and streaks can be computed from real history instead of an in-memory running total. */
class CompletedSessionsRepository(private val context: Context) {

    private object Keys {
        val COMPLETED_SESSIONS = stringSetPreferencesKey("completed_sessions")
    }

    val sessions: Flow<List<CompletedSession>> = context.goalsDataStore.data
        .map { prefs -> prefs[Keys.COMPLETED_SESSIONS].orEmpty().mapNotNull(::decode) }
        .distinctUntilChanged()

    suspend fun logSession(goalId: String, minutes: Int, completedAtEpochMillis: Long) {
        if (minutes <= 0) return
        context.goalsDataStore.edit { prefs ->
            val current = prefs[Keys.COMPLETED_SESSIONS].orEmpty()
            prefs[Keys.COMPLETED_SESSIONS] = current + encode(goalId, minutes, completedAtEpochMillis)
        }
    }

    private fun encode(goalId: String, minutes: Int, completedAtEpochMillis: Long): String =
        "$completedAtEpochMillis|$minutes|$goalId"

    private fun decode(raw: String): CompletedSession? {
        val parts = raw.split("|", limit = 3)
        if (parts.size != 3) return null
        val completedAt = parts[0].toLongOrNull() ?: return null
        val minutes = parts[1].toIntOrNull() ?: return null
        return CompletedSession(goalId = parts[2], minutes = minutes, completedAtEpochMillis = completedAt)
    }
}

/** Filters to sessions completed since the start of the current calendar week (Monday 00:00). */
fun sessionsWithinCurrentWeek(
    sessions: List<CompletedSession>,
    nowEpochMillis: Long = System.currentTimeMillis()
): List<CompletedSession> {
    val weekStart = startOfWeekEpochMillis(nowEpochMillis)
    return sessions.filter { it.completedAtEpochMillis >= weekStart }
}

/** Counts consecutive calendar days, ending today, that have at least one completed session. */
fun currentFocusStreakDays(
    sessions: List<CompletedSession>,
    nowEpochMillis: Long = System.currentTimeMillis()
): Int {
    val zone = ZoneId.systemDefault()
    val sessionDays = sessions.map { Instant.ofEpochMilli(it.completedAtEpochMillis).atZone(zone).toLocalDate() }.toSet()
    var streak = 0
    var day = Instant.ofEpochMilli(nowEpochMillis).atZone(zone).toLocalDate()
    while (day in sessionDays) {
        streak++
        day = day.minusDays(1)
    }
    return streak
}

private fun startOfWeekEpochMillis(nowEpochMillis: Long): Long {
    val calendar = Calendar.getInstance().apply {
        timeInMillis = nowEpochMillis
        firstDayOfWeek = Calendar.MONDAY
        set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return calendar.timeInMillis
}
