package com.julieasoreng.touchgrass.service

import android.app.Service
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import android.util.Log
import androidx.core.content.ContextCompat
import com.julieasoreng.touchgrass.admin.TouchGrassDeviceAdminReceiver
import com.julieasoreng.touchgrass.data.goals.ActiveFocusSessionRepository
import com.julieasoreng.touchgrass.data.preferences.LockFeaturePreferencesRepository
import com.julieasoreng.touchgrass.data.preferences.OnboardingPreferencesRepository
import com.julieasoreng.touchgrass.notifications.LockNotifications
import com.julieasoreng.touchgrass.usage.UsageStatsHelper
import java.util.Calendar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ScreenTimeMonitorService : Service() {

    private lateinit var repository: LockFeaturePreferencesRepository
    private lateinit var onboardingPreferencesRepository: OnboardingPreferencesRepository
    private lateinit var activeFocusSessionRepository: ActiveFocusSessionRepository
    private lateinit var devicePolicyManager: DevicePolicyManager
    private var serviceScope: CoroutineScope? = null
    private var userPresentReceiver: UserPresentReceiver? = null

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "onCreate")
        repository = LockFeaturePreferencesRepository(applicationContext)
        onboardingPreferencesRepository = OnboardingPreferencesRepository(applicationContext)
        activeFocusSessionRepository = ActiveFocusSessionRepository(applicationContext)
        devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        LockNotifications.ensureChannels(this)
        registerUserPresentReceiver()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "onStartCommand (intent=$intent)")
        startForeground(LockNotifications.MONITORING_NOTIFICATION_ID, LockNotifications.monitoringNotification(this))
        startMonitoringLoop()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        Log.w(TAG, "onDestroy — monitoring loop stops here until something restarts this service")
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
        scope.launch { runFocusSessionBlockLoop() }
    }

    /**
     * Tighter, focus-session-scoped polling for the app-blocker feature: only runs while
     * [ActiveFocusSessionRepository] reports a session in progress, checked far more often than
     * the once-a-minute daily-limit loop above, since "opened a blocked app" needs to be caught
     * within seconds rather than minutes. [collectLatest] means a new emission from that Flow —
     * the session ending (cleared to null) or a different session starting — cancels whatever
     * iteration of the inner while-loop was running, so this stops immediately when a session
     * ends without any extra "is a session still active" bookkeeping.
     */
    private suspend fun runFocusSessionBlockLoop() {
        activeFocusSessionRepository.active.collectLatest { persisted ->
            if (persisted == null) return@collectLatest
            val sessionEndMillis = persisted.startEpochMillis + persisted.targetSeconds * 1000L
            var lastPollAtMillis = System.currentTimeMillis()
            // Debounces one interruption per detected "open" episode: stays true while the same
            // blocked-app activity keeps being seen poll after poll (so we don't re-lock every
            // few seconds while the user is still looking at the interruption screen), and resets
            // the moment a poll comes back clean, so reopening the app afterwards re-triggers.
            var blockTriggerPending = false
            while (currentCoroutineContext().isActive) {
                val now = System.currentTimeMillis()
                if (now >= sessionEndMillis) {
                    Log.d(TAG, "Focus-session block: session's time is up, stopping tight polling")
                    break
                }
                if (!UsageStatsHelper.hasUsageAccess(applicationContext)) {
                    Log.w(TAG, "Focus-session block: usage access permission no longer granted, stopping tight polling")
                    break
                }
                val detected = UsageStatsHelper.anySocialMediaAppActiveSince(applicationContext, lastPollAtMillis)
                lastPollAtMillis = now
                if (detected) {
                    if (!blockTriggerPending) {
                        blockTriggerPending = true
                        Log.i(TAG, "Focus-session block: blocked app detected during active session")
                        lockDeviceForFocusBlock("You opened a blocked app during your focus session.")
                    }
                } else {
                    blockTriggerPending = false
                }
                delay(FOCUS_BLOCK_POLL_INTERVAL_MS)
            }
        }
    }

    private suspend fun checkUsageAgainstLimit() {
        val state = repository.state.first()
        if (!state.isDeviceAdminActive) {
            Log.d(TAG, "Poll: device admin not active, skipping check")
            return
        }
        // Only fire once per day: the intervention moment is the next unlock after crossing
        // whichever limit tripped first, not a repeated re-lock every poll while already over budget.
        if (isSameDay(state.lastLockTimestamp, System.currentTimeMillis())) {
            Log.d(TAG, "Poll: already locked today (lastLockTimestamp=${state.lastLockTimestamp}), skipping check")
            return
        }

        val elapsedMinutes = UsageStatsHelper.screenTimeTodayMinutes(applicationContext)
        Log.d(TAG, "Poll: screenTime=${elapsedMinutes}min vs dailyLimit=${state.dailyLimitMinutes}min")
        if (elapsedMinutes >= state.dailyLimitMinutes) {
            lockDeviceAndFlagNotification("You hit your ${state.dailyLimitMinutes} min daily screen time limit.")
            return
        }

        checkSocialMediaTargetCrossed()
    }

    /** Fails safe if onboarding hasn't set a target yet (null/zero) rather than locking on a
     *  phantom goal. */
    private suspend fun checkSocialMediaTargetCrossed() {
        val targetMillis = onboardingPreferencesRepository.targetScreenTimeMillis.first()
        if (targetMillis == null || targetMillis <= 0L) {
            Log.d(TAG, "Poll: no onboarding target set yet (targetMillis=$targetMillis), skipping social check")
            return
        }
        val targetMinutes = (targetMillis / 60_000L).toInt()

        val socialMinutes = UsageStatsHelper.socialMediaScreenTimeTodayMinutes(applicationContext)
        Log.d(TAG, "Poll: socialMedia=${socialMinutes}min vs target=${targetMinutes}min")
        if (socialMinutes >= targetMinutes) {
            lockDeviceAndFlagNotification("You hit your $targetMinutes min daily social media goal.")
        }
    }

    private suspend fun lockDeviceAndFlagNotification(reasonText: String) {
        if (!lockDeviceNow()) {
            Log.w(TAG, "Threshold crossed but DevicePolicyManager reports admin inactive, not locking: $reasonText")
            return
        }
        Log.i(TAG, "Locking device: $reasonText")
        repository.markLocked(reasonText)
    }

    /** Same lock + post-unlock-notification mechanism as [lockDeviceAndFlagNotification], reused
     *  by the focus-session app blocker — see [LockFeaturePreferencesRepository.markLockedByFocusBlock]
     *  for why this writes a separate DataStore path than the daily-limit trigger. */
    private suspend fun lockDeviceForFocusBlock(reasonText: String) {
        if (!lockDeviceNow()) {
            Log.w(TAG, "Focus-session block triggered but DevicePolicyManager reports admin inactive, not locking: $reasonText")
            return
        }
        Log.i(TAG, "Locking device (focus-session block): $reasonText")
        repository.markLockedByFocusBlock(reasonText)
    }

    private fun lockDeviceNow(): Boolean {
        val adminComponent = TouchGrassDeviceAdminReceiver.componentName(applicationContext)
        if (!devicePolicyManager.isAdminActive(adminComponent)) return false
        devicePolicyManager.lockNow()
        return true
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
        private const val TAG = "ScreenTimeMonitor"
        private const val MONITOR_INTERVAL_MS = 60_000L
        private const val FOCUS_BLOCK_POLL_INTERVAL_MS = 7_000L

        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, ScreenTimeMonitorService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, ScreenTimeMonitorService::class.java))
        }
    }
}
