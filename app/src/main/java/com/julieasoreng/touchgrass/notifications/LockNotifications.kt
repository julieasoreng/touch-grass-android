package com.julieasoreng.touchgrass.notifications

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.julieasoreng.touchgrass.MainActivity

object LockNotifications {
    const val MONITORING_CHANNEL_ID = "screen_time_monitoring"
    const val UNLOCK_CHANNEL_ID = "screen_time_unlock_alert"
    const val WATCHDOG_CHANNEL_ID = "screen_time_watchdog"
    const val MONITORING_NOTIFICATION_ID = 1001
    const val UNLOCK_NOTIFICATION_ID = 1002
    const val WATCHDOG_NOTIFICATION_ID = 1003

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(
                MONITORING_CHANNEL_ID,
                "Screen time monitoring",
                NotificationManager.IMPORTANCE_MIN
            )
        )
        manager.createNotificationChannel(
            NotificationChannel(
                UNLOCK_CHANNEL_ID,
                "Limit reached alerts",
                NotificationManager.IMPORTANCE_HIGH
            )
        )
        manager.createNotificationChannel(
            NotificationChannel(
                WATCHDOG_CHANNEL_ID,
                "Monitoring restarted alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            )
        )
    }

    fun monitoringNotification(context: Context): Notification =
        NotificationCompat.Builder(context, MONITORING_CHANNEL_ID)
            .setContentTitle("Touch Grass is watching your screen time")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()

    /** No-ops if POST_NOTIFICATIONS hasn't been granted (API 33+) rather than crashing. */
    fun postUnlockAlert(context: Context, reasonText: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val contentIntent = Intent(context, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_SHOW_POST_UNLOCK, true)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            UNLOCK_NOTIFICATION_ID,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, UNLOCK_CHANNEL_ID)
            .setContentTitle("You hit your limit")
            .setContentText(reasonText)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(pendingIntent, true)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(UNLOCK_NOTIFICATION_ID, notification)
    }

    /** Posted by MonitoringWatchdogWorker when it finds the monitoring service's heartbeat stale
     *  or missing and has just restarted it — the closest we can get to "notify me if the app
     *  gets force-closed": Android gives no hook to run code as an explicit Force Stop happens,
     *  but a background kill by OEM battery management leaves a stale heartbeat this can detect. */
    fun watchdogRestartAlert(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            WATCHDOG_NOTIFICATION_ID,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, WATCHDOG_CHANNEL_ID)
            .setContentTitle("Screen time monitoring restarted")
            .setContentText("Your phone stopped it in the background, so Touch Grass just restarted it.")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(WATCHDOG_NOTIFICATION_ID, notification)
    }
}
