package com.julieasoreng.touchgrass.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.lockFeatureDataStore by preferencesDataStore(name = "lock_feature_prefs")

data class LockFeatureState(
    val isDeviceAdminActive: Boolean = false,
    val dailyLimitMinutes: Int = 60,
    val pendingUnlockNotification: Boolean = false,
    val lastLockTimestamp: Long = 0L,
    val lastLockReasonText: String = "",
    val lastHeartbeatTimestamp: Long = 0L,
    val hasDismissedLockNudge: Boolean = false,
    val baselineTimestamp: Long = 0L,
    val baselineUsageMinutes: Int = 0,
    val baselineSocialMinutes: Int = 0
)

class LockFeaturePreferencesRepository(private val context: Context) {

    private object Keys {
        val DEVICE_ADMIN_ACTIVE = booleanPreferencesKey("device_admin_active")
        val DAILY_LIMIT_MINUTES = intPreferencesKey("daily_limit_minutes")
        val PENDING_UNLOCK_NOTIFICATION = booleanPreferencesKey("pending_unlock_notification")
        val LAST_LOCK_TIMESTAMP = longPreferencesKey("last_lock_timestamp")
        val LAST_LOCK_REASON_TEXT = stringPreferencesKey("last_lock_reason_text")
        val LAST_HEARTBEAT_TIMESTAMP = longPreferencesKey("last_heartbeat_timestamp")
        val HAS_DISMISSED_LOCK_NUDGE = booleanPreferencesKey("has_dismissed_lock_nudge")
        val BASELINE_TIMESTAMP = longPreferencesKey("baseline_timestamp")
        val BASELINE_USAGE_MINUTES = intPreferencesKey("baseline_usage_minutes")
        val BASELINE_SOCIAL_MINUTES = intPreferencesKey("baseline_social_minutes")
    }

    val state: Flow<LockFeatureState> = context.lockFeatureDataStore.data.map { prefs ->
        LockFeatureState(
            isDeviceAdminActive = prefs[Keys.DEVICE_ADMIN_ACTIVE] ?: false,
            dailyLimitMinutes = prefs[Keys.DAILY_LIMIT_MINUTES] ?: 60,
            pendingUnlockNotification = prefs[Keys.PENDING_UNLOCK_NOTIFICATION] ?: false,
            lastLockTimestamp = prefs[Keys.LAST_LOCK_TIMESTAMP] ?: 0L,
            lastLockReasonText = prefs[Keys.LAST_LOCK_REASON_TEXT] ?: "",
            lastHeartbeatTimestamp = prefs[Keys.LAST_HEARTBEAT_TIMESTAMP] ?: 0L,
            hasDismissedLockNudge = prefs[Keys.HAS_DISMISSED_LOCK_NUDGE] ?: false,
            baselineTimestamp = prefs[Keys.BASELINE_TIMESTAMP] ?: 0L,
            baselineUsageMinutes = prefs[Keys.BASELINE_USAGE_MINUTES] ?: 0,
            baselineSocialMinutes = prefs[Keys.BASELINE_SOCIAL_MINUTES] ?: 0
        )
    }

    suspend fun setDeviceAdminActive(active: Boolean) {
        context.lockFeatureDataStore.edit { it[Keys.DEVICE_ADMIN_ACTIVE] = active }
    }

    suspend fun setDailyLimitMinutes(minutes: Int) {
        context.lockFeatureDataStore.edit { it[Keys.DAILY_LIMIT_MINUTES] = minutes }
    }

    /** False only until the user (or a one-time onboarding seed) ever explicitly sets a daily
     *  limit — lets callers distinguish "never set, showing the 60min fallback" from "user chose
     *  60min on purpose", so the onboarding-target seed doesn't clobber a real choice. */
    suspend fun hasExplicitDailyLimit(): Boolean {
        return context.lockFeatureDataStore.data.first().contains(Keys.DAILY_LIMIT_MINUTES)
    }

    /** One-time snapshot of that moment's cumulative UsageStatsManager totals, taken the first
     *  time monitoring actually runs with device admin active. [ScreenTimeMonitorService]'s
     *  daily-limit check subtracts this from the day's raw usage — otherwise a user who installs
     *  mid-afternoon (after already using the phone that day) gets an immediate lock, since
     *  UsageStatsManager reports usage from local midnight regardless of when the app arrived.
     *  Guarded by `contains` so repeated calls (every poll) after the first are no-ops. */
    suspend fun captureBaselineIfNeeded(usageMinutesNow: Int, socialMinutesNow: Int) {
        context.lockFeatureDataStore.edit { prefs ->
            if (!prefs.contains(Keys.BASELINE_TIMESTAMP)) {
                prefs[Keys.BASELINE_TIMESTAMP] = System.currentTimeMillis()
                prefs[Keys.BASELINE_USAGE_MINUTES] = usageMinutesNow
                prefs[Keys.BASELINE_SOCIAL_MINUTES] = socialMinutesNow
            }
        }
    }

    /** Debug-only: clears today's lock so the daily-limit trigger can be re-tested without
     *  waiting for midnight or changing the system clock. */
    suspend fun debugResetDailyLockGate() {
        context.lockFeatureDataStore.edit { it[Keys.LAST_LOCK_TIMESTAMP] = 0L }
    }

    suspend fun markLocked(reasonText: String) {
        context.lockFeatureDataStore.edit { prefs ->
            prefs[Keys.PENDING_UNLOCK_NOTIFICATION] = true
            prefs[Keys.LAST_LOCK_TIMESTAMP] = System.currentTimeMillis()
            prefs[Keys.LAST_LOCK_REASON_TEXT] = reasonText
        }
    }

    /** Same post-unlock notification/reason plumbing as [markLocked], for the focus-session app
     *  blocker — deliberately does NOT touch [Keys.LAST_LOCK_TIMESTAMP], since that field is the
     *  daily-limit feature's once-per-calendar-day debounce marker (see isSameDay in
     *  ScreenTimeMonitorService). A focus-session block is a separate trigger with its own
     *  per-open-event debounce and must not suppress the daily-limit check for the rest of the day. */
    suspend fun markLockedByFocusBlock(reasonText: String) {
        context.lockFeatureDataStore.edit { prefs ->
            prefs[Keys.PENDING_UNLOCK_NOTIFICATION] = true
            prefs[Keys.LAST_LOCK_REASON_TEXT] = reasonText
        }
    }

    suspend fun clearPendingUnlockNotification() {
        context.lockFeatureDataStore.edit { it[Keys.PENDING_UNLOCK_NOTIFICATION] = false }
    }

    /** Written every poll cycle while [ScreenTimeMonitorService][com.julieasoreng.touchgrass.service.ScreenTimeMonitorService]
     *  is actually alive, so [com.julieasoreng.touchgrass.service.MonitoringWatchdogWorker] can tell a
     *  stale/missing heartbeat apart from a healthy one. */
    suspend fun recordHeartbeat() {
        context.lockFeatureDataStore.edit { it[Keys.LAST_HEARTBEAT_TIMESTAMP] = System.currentTimeMillis() }
    }

    /** The user tapped "Not now" on the My Goals nudge — stop showing it until they turn Screen
     *  Lock on some other way (at which point isDeviceAdminActive alone hides it). */
    suspend fun dismissLockNudge() {
        context.lockFeatureDataStore.edit { it[Keys.HAS_DISMISSED_LOCK_NUDGE] = true }
    }
}
