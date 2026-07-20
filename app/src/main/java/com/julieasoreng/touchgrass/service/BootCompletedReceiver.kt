package com.julieasoreng.touchgrass.service

import android.app.admin.DevicePolicyManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.julieasoreng.touchgrass.admin.TouchGrassDeviceAdminReceiver
import com.julieasoreng.touchgrass.data.preferences.LockFeaturePreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val TAG = "SCREENTIME_TRIGGER"

/**
 * Without this, a reboot leaves [ScreenTimeMonitorService] dead until the user manually reopens
 * the app — a foreground service never comes back on its own after boot, and waiting on
 * [MonitoringWatchdogWorker]'s own next scheduled tick (up to 15 minutes away) isn't a substitute
 * for restarting monitoring as soon as the device is usable again. Calling startForegroundService()
 * from BOOT_COMPLETED is one of Android's documented exemptions to the background-start
 * restriction, so this doesn't need the setForeground()-on-self workaround MonitoringWatchdogWorker
 * needs from a plain WorkManager context.
 */
class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val appContext = context.applicationContext
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val devicePolicyManager = appContext.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
                val isActive = devicePolicyManager.isAdminActive(TouchGrassDeviceAdminReceiver.componentName(appContext))
                Log.d(TAG, "BootCompletedReceiver: isDeviceAdminActive=$isActive")
                LockFeaturePreferencesRepository(appContext).setDeviceAdminActive(isActive)
                if (isActive) {
                    ScreenTimeMonitorService.start(appContext)
                    MonitoringWatchdogWorker.schedule(appContext)
                    Log.i(TAG, "BootCompletedReceiver: monitoring restarted after boot")
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
