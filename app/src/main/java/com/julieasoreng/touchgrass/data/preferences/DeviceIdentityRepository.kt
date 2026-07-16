package com.julieasoreng.touchgrass.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.deviceIdentityDataStore by preferencesDataStore(name = "device_identity_prefs")

/**
 * A single anonymous identifier generated and stored locally on first launch — not tied to any
 * account, login, or backend. Exists purely so locally stored data (goals, sessions, onboarding
 * answers) shares one consistent key, and so a future on-device export/analysis pass has something
 * stable to key on.
 */
class DeviceIdentityRepository(private val context: Context) {

    private object Keys {
        val DEVICE_ID = stringPreferencesKey("device_id")
    }

    /** Returns the existing device ID, generating and persisting one the first time this is called. */
    suspend fun getOrCreateDeviceId(): String {
        val existing = context.deviceIdentityDataStore.data
            .map { prefs -> prefs[Keys.DEVICE_ID] }
            .first()
        if (existing != null) return existing

        val newId = UUID.randomUUID().toString()
        context.deviceIdentityDataStore.edit { prefs -> prefs[Keys.DEVICE_ID] = newId }
        return newId
    }
}
