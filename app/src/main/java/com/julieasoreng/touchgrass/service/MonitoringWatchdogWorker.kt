package com.julieasoreng.touchgrass.service

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.julieasoreng.touchgrass.data.preferences.LockFeaturePreferencesRepository
import com.julieasoreng.touchgrass.notifications.LockNotifications
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first

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
        if (!state.isDeviceAdminActive) return Result.success()

        val sinceLastHeartbeat = System.currentTimeMillis() - state.lastHeartbeatTimestamp
        if (state.lastHeartbeatTimestamp > 0L && sinceLastHeartbeat < STALE_THRESHOLD_MS) {
            return Result.success()
        }

        // Heartbeat missing or stale: the monitoring service isn't actually running anymore.
        ScreenTimeMonitorService.start(applicationContext)
        LockNotifications.ensureChannels(applicationContext)
        LockNotifications.watchdogRestartAlert(applicationContext)
        return Result.success()
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
