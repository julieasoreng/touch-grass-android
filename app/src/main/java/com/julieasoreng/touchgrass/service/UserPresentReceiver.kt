package com.julieasoreng.touchgrass.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.julieasoreng.touchgrass.data.preferences.LockFeaturePreferencesRepository
import com.julieasoreng.touchgrass.notifications.LockNotifications
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val TAG = "SCREENTIME_TRIGGER"

/**
 * Must be registered dynamically via [Context.registerReceiver] — ACTION_USER_PRESENT
 * can no longer be declared as a manifest-registered implicit broadcast.
 *
 * Only exists while [ScreenTimeMonitorService] is alive (registered in its onCreate, unregistered
 * in onDestroy) — if the service dies between a lock firing and the user unlocking, nothing is
 * listening here and pendingUnlockNotification just sits unresolved. MainActivity's
 * resumeMonitoringIfNeeded() independently catches that case on next app open.
 */
class UserPresentReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_USER_PRESENT) return

        val appContext = context.applicationContext
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repository = LockFeaturePreferencesRepository(appContext)
                val state = repository.state.first()
                Log.d(TAG, "UserPresentReceiver: pendingUnlockNotification=${state.pendingUnlockNotification}")
                if (state.pendingUnlockNotification) {
                    LockNotifications.postUnlockAlert(appContext, state.lastLockReasonText)
                    repository.clearPendingUnlockNotification()
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
