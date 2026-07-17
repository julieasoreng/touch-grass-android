package com.julieasoreng.touchgrass.service

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.julieasoreng.touchgrass.data.preferences.LockFeaturePreferencesRepository
import com.julieasoreng.touchgrass.notifications.LockNotifications
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first

private const val TAG = "MonitoringWatchdog"

/**
 * Periodic check-in that survives [ScreenTimeMonitorService] being killed in the background by
 * OEM battery management (Samsung/Xiaomi/OnePlus-style "app freezers"), which a plain START_STICKY
 * service can't reliably recover from on its own. Cannot do anything about an explicit user
 * Force Stop — Android revokes all of an app's scheduled work, alarms, and receivers the instant
 * that happens, by design, so there is no hook left to react from.
 */
class MonitoringWatchdogWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val repository = LockFeaturePreferencesRepository(applicationContext)
        val state = repository.state.first()
        if (!state.isDeviceAdminActive) {
            Log.d(TAG, "Tick: device admin not active, nothing to check")
            return Result.success()
        }

        val sinceLastHeartbeat = System.currentTimeMillis() - state.lastHeartbeatTimestamp
        if (state.lastHeartbeatTimestamp > 0L && sinceLastHeartbeat < STALE_THRESHOLD_MS) {
            Log.d(TAG, "Tick: heartbeat healthy (${sinceLastHeartbeat}ms ago)")
            return Result.success()
        }

        Log.w(TAG, "Heartbeat missing or stale (last=${state.lastHeartbeatTimestamp}, ${sinceLastHeartbeat}ms ago) — restarting monitoring")
        restartMonitoringService()
        return Result.success()
    }

    /** Heartbeat missing or stale means the monitoring service isn't actually running anymore, so
     *  we try to bring it back. This runs from a background execution context (no visible Activity
     *  triggered it), and on API 31+ that makes a plain startForegroundService() call targeting a
     *  different Service class subject to Android's background-start restriction — it throws
     *  ForegroundServiceStartNotAllowedException, uncaught, which kills this whole process. That's
     *  especially bad here since it happens inside the one path meant to recover an already-killed
     *  service, so it would never come back. Promoting this worker itself into a foreground service
     *  first satisfies the "app already has a foreground service running" exemption for the
     *  subsequent call; the try/catch is a hard backstop in case that doesn't hold on some OEM skin
     *  — if the restart still can't happen, at least don't crash, and fall back to the notification
     *  so the user has a way to notice and reopen the app (which independently retries this in
     *  MainActivity).
     */
    private suspend fun restartMonitoringService() {
        try {
            setForeground(
                ForegroundInfo(LockNotifications.MONITORING_NOTIFICATION_ID, LockNotifications.monitoringNotification(applicationContext))
            )
            ScreenTimeMonitorService.start(applicationContext)
            Log.i(TAG, "Monitoring service restart succeeded")
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Monitoring service restart blocked (likely background FGS-start restriction)", e)
        }
        LockNotifications.ensureChannels(applicationContext)
        LockNotifications.watchdogRestartAlert(applicationContext)
    }

    companion object {
        // Comfortably more than the service's own 60s poll interval, to absorb normal scheduling
        // jitter without treating a briefly-delayed poll as a dead service.
        private const val STALE_THRESHOLD_MS = 5 * 60 * 1000L
        private const val INTERVAL_MINUTES = 15L // WorkManager's minimum periodic interval
        private const val UNIQUE_WORK_NAME = "monitoring_watchdog"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<MonitoringWatchdogWorker>(INTERVAL_MINUTES, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().build())
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
        }
    }
}
