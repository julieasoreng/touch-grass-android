package com.julieasoreng.touchgrass.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.lockFeatureDataStore by preferencesDataStore(name = "lock_feature_prefs")

data class LockFeatureState(
    val isDeviceAdminActive: Boolean = false,
    val dailyLimitMinutes: Int = 60,
    val pendingUnlockNotification: Boolean = false,
    val lastLockTimestamp: Long = 0L,
    val lastLockReasonText: String = ""
)

class LockFeaturePreferencesRepository(private val context: Context) {

    private object Keys {
        val DEVICE_ADMIN_ACTIVE = booleanPreferencesKey("device_admin_active")
        val DAILY_LIMIT_MINUTES = intPreferencesKey("daily_limit_minutes")
        val PENDING_UNLOCK_NOTIFICATION = booleanPreferencesKey("pending_unlock_notification")
        val LAST_LOCK_TIMESTAMP = longPreferencesKey("last_lock_timestamp")
        val LAST_LOCK_REASON_TEXT = stringPreferencesKey("last_lock_reason_text")
    }

    val state: Flow<LockFeatureState> = context.lockFeatureDataStore.data.map { prefs ->
        LockFeatureState(
            isDeviceAdminActive = prefs[Keys.DEVICE_ADMIN_ACTIVE] ?: false,
            dailyLimitMinutes = prefs[Keys.DAILY_LIMIT_MINUTES] ?: 60,
            pendingUnlockNotification = prefs[Keys.PENDING_UNLOCK_NOTIFICATION] ?: false,
            lastLockTimestamp = prefs[Keys.LAST_LOCK_TIMESTAMP] ?: 0L,
            lastLockReasonText = prefs[Keys.LAST_LOCK_REASON_TEXT] ?: ""
        )
    }

    suspend fun setDeviceAdminActive(active: Boolean) {
        context.lockFeatureDataStore.edit { it[Keys.DEVICE_ADMIN_ACTIVE] = active }
    }

    suspend fun setDailyLimitMinutes(minutes: Int) {
        context.lockFeatureDataStore.edit { it[Keys.DAILY_LIMIT_MINUTES] = minutes }
    }

    suspend fun markLocked(reasonText: String) {
        context.lockFeatureDataStore.edit { prefs ->
            prefs[Keys.PENDING_UNLOCK_NOTIFICATION] = true
            prefs[Keys.LAST_LOCK_TIMESTAMP] = System.currentTimeMillis()
            prefs[Keys.LAST_LOCK_REASON_TEXT] = reasonText
        }
    }

    suspend fun clearPendingUnlockNotification() {
        context.lockFeatureDataStore.edit { it[Keys.PENDING_UNLOCK_NOTIFICATION] = false }
    }
}
