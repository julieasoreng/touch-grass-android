package com.julieasoreng.touchgrass.data.goals

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.activeSessionDataStore by preferencesDataStore(name = "active_session_prefs")

data class PersistedActiveSession(
    val goalId: String,
    val targetSeconds: Int,
    val startEpochMillis: Long
)

/** Mirrors the in-memory focus-timer countdown to disk so a session survives the process being
 *  killed mid-countdown (OS memory pressure, not just backgrounding) — [GoalsViewModel] resumes
 *  or credits it from wall-clock elapsed time on next launch instead of silently losing it. */
class ActiveFocusSessionRepository(private val context: Context) {

    private object Keys {
        val GOAL_ID = stringPreferencesKey("goal_id")
        val TARGET_SECONDS = intPreferencesKey("target_seconds")
        val START_EPOCH_MILLIS = longPreferencesKey("start_epoch_millis")
    }

    val active: Flow<PersistedActiveSession?> = context.activeSessionDataStore.data.map { prefs ->
        val goalId = prefs[Keys.GOAL_ID] ?: return@map null
        val targetSeconds = prefs[Keys.TARGET_SECONDS] ?: return@map null
        val startEpochMillis = prefs[Keys.START_EPOCH_MILLIS] ?: return@map null
        PersistedActiveSession(goalId, targetSeconds, startEpochMillis)
    }

    suspend fun save(goalId: String, targetSeconds: Int, startEpochMillis: Long) {
        context.activeSessionDataStore.edit { prefs ->
            prefs[Keys.GOAL_ID] = goalId
            prefs[Keys.TARGET_SECONDS] = targetSeconds
            prefs[Keys.START_EPOCH_MILLIS] = startEpochMillis
        }
    }

    suspend fun clear() {
        context.activeSessionDataStore.edit { it.clear() }
    }
}
