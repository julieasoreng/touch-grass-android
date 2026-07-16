package com.julieasoreng.touchgrass.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.julieasoreng.touchgrass.data.preferences.LockFeaturePreferencesRepository
import com.julieasoreng.touchgrass.notifications.LockNotifications
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Must be registered dynamically via [Context.registerReceiver] — ACTION_USER_PRESENT
 * can no longer be declared as a manifest-registered implicit broadcast.
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
