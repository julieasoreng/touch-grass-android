package com.julieasoreng.touchgrass.service

import android.app.Service
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import androidx.core.content.ContextCompat
import com.julieasoreng.touchgrass.admin.TouchGrassDeviceAdminReceiver
import com.julieasoreng.touchgrass.data.preferences.LockFeaturePreferencesRepository
import com.julieasoreng.touchgrass.data.preferences.OnboardingPreferencesRepository
import com.julieasoreng.touchgrass.notifications.LockNotifications
import com.julieasoreng.touchgrass.usage.UsageStatsHelper
import java.util.Calendar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ScreenTimeMonitorService : Service() {

    private lateinit var repository: LockFeaturePreferencesRepository
    private lateinit var onboardingPreferencesRepository: OnboardingPreferencesRepository
    private lateinit var devicePolicyManager: DevicePolicyManager
    private var serviceScope: CoroutineScope? = null
    private var userPresentReceiver: UserPresentReceiver? = null

    override fun onCreate() {
        super.onCreate()
        repository = LockFeaturePreferencesRepository(applicationContext)
        onboardingPreferencesRepository = OnboardingPreferencesRepository(applicationContext)
        devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        LockNotifications.ensureChannels(this)
        registerUserPresentReceiver()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(LockNotifications.MONITORING_NOTIFICATION_ID, LockNotifications.monitoringNotification(this))
        startMonitoringLoop()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        serviceScope?.cancel()
        userPresentReceiver?.let { unregisterReceiver(it) }
        super.onDestroy()
    }

    private fun startMonitoringLoop() {
        serviceScope?.cancel()
        val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
        serviceScope = scope
        scope.launch {
            while (isActive) {
                // Lets MonitoringWatchdogWorker tell "this service is alive" apart from "the OS
                // killed it in the background" — see recordHeartbeat's doc for why.
                repository.recordHeartbeat()
                checkUsageAgainstLimit()
                delay(MONITOR_INTERVAL_MS)
            }
        }
    }

    private suspend fun checkUsageAgainstLimit() {
        val state = repository.state.first()
        if (!state.isDeviceAdminActive) return
        // Only fire once per day: the intervention moment is the next unlock after crossing
        // whichever limit tripped first, not a repeated re-lock every poll while already over budget.
        if (isSameDay(state.lastLockTimestamp, System.currentTimeMillis())) return

        val elapsedMinutes = UsageStatsHelper.screenTimeTodayMinutes(applicationContext)
        if (elapsedMinutes >= state.dailyLimitMinutes) {
            lockDeviceAndFlagNotification("You hit your ${state.dailyLimitMinutes} min daily screen time limit.")
            return
        }

        checkSocialMediaTargetCrossed()
    }

    /** Fails safe if onboarding hasn't set a target yet (null/zero) rather than locking on a
     *  phantom goal. */
    private suspend fun checkSocialMediaTargetCrossed() {
        val targetMillis = onboardingPreferencesRepository.targetScreenTimeMillis.first() ?: return
        if (targetMillis <= 0L) return
        val targetMinutes = (targetMillis / 60_000L).toInt()

        val socialMinutes = UsageStatsHelper.socialMediaScreenTimeTodayMinutes(applicationContext)
        if (socialMinutes >= targetMinutes) {
            lockDeviceAndFlagNotification("You hit your $targetMinutes min daily social media goal.")
        }
    }

    private suspend fun lockDeviceAndFlagNotification(reasonText: String) {
        val adminComponent = TouchGrassDeviceAdminReceiver.componentName(applicationContext)
        if (!devicePolicyManager.isAdminActive(adminComponent)) return
        devicePolicyManager.lockNow()
        repository.markLocked(reasonText)
    }

    private fun isSameDay(timestampA: Long, timestampB: Long): Boolean {
        if (timestampA <= 0L) return false
        val calendarA = Calendar.getInstance().apply { timeInMillis = timestampA }
        val calendarB = Calendar.getInstance().apply { timeInMillis = timestampB }
        return calendarA.get(Calendar.YEAR) == calendarB.get(Calendar.YEAR) &&
            calendarA.get(Calendar.DAY_OF_YEAR) == calendarB.get(Calendar.DAY_OF_YEAR)
    }

    private fun registerUserPresentReceiver() {
        val receiver = UserPresentReceiver()
        userPresentReceiver = receiver
        ContextCompat.registerReceiver(
            this,
            receiver,
            IntentFilter(Intent.ACTION_USER_PRESENT),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    companion object {
        private const val MONITOR_INTERVAL_MS = 60_000L

        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, ScreenTimeMonitorService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, ScreenTimeMonitorService::class.java))
        }
    }
}
