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
import com.julieasoreng.touchgrass.overlay.InterventionOverlayController
import com.julieasoreng.touchgrass.usage.UsageEventsHelper
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
import kotlinx.coroutines.withContext

class ScreenTimeMonitorService : Service() {

    private lateinit var repository: LockFeaturePreferencesRepository
    private lateinit var onboardingPreferencesRepository: OnboardingPreferencesRepository
    private lateinit var activeFocusSessionRepository: ActiveFocusSessionRepository
    private lateinit var devicePolicyManager: DevicePolicyManager
    private var serviceScope: CoroutineScope? = null
    private var userPresentReceiver: UserPresentReceiver? = null
    private val overlayController by lazy { InterventionOverlayController(applicationContext) }

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
        // Don't leave a dangling WindowManager window behind if the service dies mid-overlay.
        if (overlayController.isShowing()) {
            overlayController.hideBecauseUserLeftMonitoredApp()
        }
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
        scope.launch { runOverlayInterventionLoop() }
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
                    Log.d(TRIGGER_TAG, "runFocusSessionBlockLoop: session's time is up, stopping tight polling")
                    break
                }
                if (!UsageStatsHelper.hasUsageAccess(applicationContext)) {
                    Log.w(TRIGGER_TAG, "runFocusSessionBlockLoop: usage access permission no longer granted, stopping tight polling")
                    break
                }
                val detected = UsageStatsHelper.anySocialMediaAppActiveSince(applicationContext, lastPollAtMillis)
                Log.d(TRIGGER_TAG, "runFocusSessionBlockLoop: detected=$detected, blockTriggerPending=$blockTriggerPending (since=$lastPollAtMillis)")
                lastPollAtMillis = now
                if (detected) {
                    if (!blockTriggerPending) {
                        blockTriggerPending = true
                        Log.i(TRIGGER_TAG, "runFocusSessionBlockLoop: TRIGGER FIRED (blocked app opened during focus session)")
                        lockDeviceForFocusBlock("You opened a blocked app during your focus session.")
                    }
                } else {
                    blockTriggerPending = false
                }
                delay(FOCUS_BLOCK_POLL_INTERVAL_MS)
            }
        }
    }

    private data class ActiveSocialSession(val packageName: String, val startMillis: Long)

    /**
     * Continuous, in-app-overlay intervention: unlike [runFocusSessionBlockLoop] (which only cares
     * whether a blocked app was opened at all during a focus session) and [checkUsageAgainstLimit]
     * (which checks cumulative usage against a daily/UsageStats-aggregated total), this tracks one
     * specific *continuous* foreground stretch in a social media app using raw [UsageEventsHelper]
     * transitions, and shows [overlayController] directly over that app once this stretch alone
     * crosses [OVERLAY_SESSION_THRESHOLD_SECONDS] — without requiring the user to ever leave the app.
     *
     * State (`session`, `triggeredForSessionStart`) lives in this function's local scope rather than
     * on the service, so it can't be accidentally reset by anything else touching service state —
     * the only things that change it are actual RESUMED/PAUSED transitions read from UsageEvents.
     */
    private suspend fun runOverlayInterventionLoop() {
        // Generous first lookback so a session already in progress when this loop starts (e.g. the
        // service was just (re)started while the user was already mid-scroll) is still caught,
        // rather than only detected on the *next* open. Subsequent polls use a tight incremental
        // window instead, since by then `session` already tracks anything ongoing.
        var lastPollMillis = System.currentTimeMillis() - INITIAL_LOOKBACK_MILLIS
        var session: ActiveSocialSession? = null
        var triggeredForSessionStart: Long? = null

        while (currentCoroutineContext().isActive) {
            val now = System.currentTimeMillis()
            if (!UsageStatsHelper.hasUsageAccess(applicationContext)) {
                Log.w(OVERLAY_TAG, "runOverlayInterventionLoop: usage access not granted, skipping tick")
                delay(OVERLAY_POLL_INTERVAL_MS)
                continue
            }

            val transitions = UsageEventsHelper.socialMediaTransitionsSince(applicationContext, lastPollMillis)
            lastPollMillis = now
            for (transition in transitions) {
                when (transition.type) {
                    UsageEventsHelper.TransitionType.RESUMED -> {
                        session = ActiveSocialSession(transition.packageName, transition.timestampMillis)
                        triggeredForSessionStart = null
                        Log.d(OVERLAY_TAG, "runOverlayInterventionLoop: session started pkg=${transition.packageName}, startMillis=${transition.timestampMillis}")
                    }
                    UsageEventsHelper.TransitionType.PAUSED -> {
                        if (session?.packageName == transition.packageName) {
                            Log.d(OVERLAY_TAG, "runOverlayInterventionLoop: session ended pkg=${transition.packageName} (left app or force-closed)")
                            session = null
                            if (overlayController.isShowing()) {
                                withContext(Dispatchers.Main) { overlayController.hideBecauseUserLeftMonitoredApp() }
                            }
                        }
                    }
                }
            }

            val activeSession = session
            if (activeSession == null) {
                delay(OVERLAY_POLL_INTERVAL_MS)
                continue
            }

            val elapsedSeconds = (now - activeSession.startMillis) / 1000
            val alreadyTriggered = triggeredForSessionStart == activeSession.startMillis
            Log.d(
                OVERLAY_TAG,
                "runOverlayInterventionLoop: pkg=${activeSession.packageName}, elapsedSeconds=$elapsedSeconds, " +
                    "thresholdSeconds=$OVERLAY_SESSION_THRESHOLD_SECONDS, alreadyTriggered=$alreadyTriggered"
            )
            if (!alreadyTriggered && elapsedSeconds >= OVERLAY_SESSION_THRESHOLD_SECONDS) {
                triggeredForSessionStart = activeSession.startMillis
                val crossedAtMillis = System.currentTimeMillis()
                Log.i(OVERLAY_TAG, "runOverlayInterventionLoop: TRIGGER FIRED pkg=${activeSession.packageName}, crossedAtMillis=$crossedAtMillis")
                withContext(Dispatchers.Main) {
                    overlayController.show("You've been scrolling ${activeSession.packageName} for ${elapsedSeconds / 60} min straight.") {
                        Log.i(OVERLAY_TAG, "runOverlayInterventionLoop: overlay dismissed by user at ${System.currentTimeMillis()}")
                    }
                }
                val shownAtMillis = System.currentTimeMillis()
                Log.i(OVERLAY_TAG, "runOverlayInterventionLoop: overlay shown at $shownAtMillis, reactionTimeMs=${shownAtMillis - crossedAtMillis}")
            }
            delay(OVERLAY_POLL_INTERVAL_MS)
        }
    }

    private suspend fun checkUsageAgainstLimit() {
        var state = repository.state.first()
        if (!state.isDeviceAdminActive) {
            Log.d(TRIGGER_TAG, "checkUsageAgainstLimit: device admin not active, skipping check")
            return
        }

        val rawUsageMinutes = UsageStatsHelper.screenTimeTodayMinutes(applicationContext)
        val rawSocialMinutes = UsageStatsHelper.socialMediaScreenTimeTodayMinutes(applicationContext)
        // UsageStatsManager reports cumulative usage since local midnight, which includes usage
        // from before this app was even installed/monitoring was enabled. Snapshot that raw total
        // once, the first time monitoring runs, so today's trigger only counts usage from this
        // point forward instead of the whole day. See captureBaselineIfNeeded's doc for why.
        if (state.baselineTimestamp == 0L) {
            repository.captureBaselineIfNeeded(rawUsageMinutes, rawSocialMinutes)
            state = repository.state.first()
        }

        // Only fire once per day: the intervention moment is the next unlock after crossing
        // whichever limit tripped first, not a repeated re-lock every poll while already over budget.
        val alreadyLockedToday = isSameDay(state.lastLockTimestamp, System.currentTimeMillis())
        if (alreadyLockedToday) {
            Log.d(TRIGGER_TAG, "checkUsageAgainstLimit: already locked today (lastLockTimestamp=${state.lastLockTimestamp}), skipping check")
            return
        }

        // The baseline snapshot only means "usage before monitoring started" on the day it was
        // captured — on every later day it's stale, so raw midnight-to-now usage is used as-is.
        val baselineAppliesToday = isSameDay(state.baselineTimestamp, System.currentTimeMillis())
        val baselineUsageMinutes = if (baselineAppliesToday) state.baselineUsageMinutes else 0
        val elapsedMinutes = (rawUsageMinutes - baselineUsageMinutes).coerceAtLeast(0)
        val dailyLimitCrossed = elapsedMinutes >= state.dailyLimitMinutes
        Log.d(
            TRIGGER_TAG,
            "checkUsageAgainstLimit: raw=${rawUsageMinutes}min, baseline=${baselineUsageMinutes}min(appliesToday=$baselineAppliesToday), " +
                "measured=${elapsedMinutes}min, threshold=${state.dailyLimitMinutes}min, crossed=$dailyLimitCrossed"
        )
        if (dailyLimitCrossed) {
            Log.i(TRIGGER_TAG, "checkUsageAgainstLimit: TRIGGER FIRED (daily screen time limit)")
            lockDeviceAndFlagNotification("You hit your ${state.dailyLimitMinutes} min daily screen time limit.")
            return
        }

        checkSocialMediaTargetCrossed(rawSocialMinutes, baselineAppliesToday, state.baselineSocialMinutes)
    }

    /** Fails safe if onboarding hasn't set a target yet (null/zero) rather than locking on a
     *  phantom goal. [rawSocialMinutes]/[baselineAppliesToday]/[baselineSocialMinutes] are passed
     *  in from [checkUsageAgainstLimit] so both checks share one UsageStatsManager query and one
     *  baseline snapshot per poll. */
    private suspend fun checkSocialMediaTargetCrossed(
        rawSocialMinutes: Int,
        baselineAppliesToday: Boolean,
        baselineSocialMinutes: Int
    ) {
        val targetMillis = onboardingPreferencesRepository.targetScreenTimeMillis.first()
        if (targetMillis == null || targetMillis <= 0L) {
            Log.d(TRIGGER_TAG, "checkSocialMediaTargetCrossed: no onboarding target set yet (targetMillis=$targetMillis), skipping check")
            return
        }
        val targetMinutes = (targetMillis / 60_000L).toInt()

        val baselineMinutes = if (baselineAppliesToday) baselineSocialMinutes else 0
        val socialMinutes = (rawSocialMinutes - baselineMinutes).coerceAtLeast(0)
        val targetCrossed = socialMinutes >= targetMinutes
        Log.d(
            TRIGGER_TAG,
            "checkSocialMediaTargetCrossed: raw=${rawSocialMinutes}min, baseline=${baselineMinutes}min(appliesToday=$baselineAppliesToday), " +
                "measured=${socialMinutes}min, threshold=${targetMinutes}min, crossed=$targetCrossed"
        )
        if (targetCrossed) {
            Log.i(TRIGGER_TAG, "checkSocialMediaTargetCrossed: TRIGGER FIRED (social media target)")
            lockDeviceAndFlagNotification("You hit your $targetMinutes min daily social media goal.")
        }
    }

    private suspend fun lockDeviceAndFlagNotification(reasonText: String) {
        if (!lockDeviceNow()) {
            Log.w(TRIGGER_TAG, "lockDeviceAndFlagNotification: BLOCKED — DevicePolicyManager reports admin inactive, not locking: $reasonText")
            return
        }
        Log.i(TRIGGER_TAG, "lockDeviceAndFlagNotification: lockNow() succeeded, marking pending unlock notification: $reasonText")
        repository.markLocked(reasonText)
    }

    /** Same lock + post-unlock-notification mechanism as [lockDeviceAndFlagNotification], reused
     *  by the focus-session app blocker — see [LockFeaturePreferencesRepository.markLockedByFocusBlock]
     *  for why this writes a separate DataStore path than the daily-limit trigger. */
    private suspend fun lockDeviceForFocusBlock(reasonText: String) {
        if (!lockDeviceNow()) {
            Log.w(TRIGGER_TAG, "lockDeviceForFocusBlock: BLOCKED — DevicePolicyManager reports admin inactive, not locking: $reasonText")
            return
        }
        Log.i(TRIGGER_TAG, "lockDeviceForFocusBlock: lockNow() succeeded, marking pending unlock notification: $reasonText")
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
        // Dedicated tag for the measure→threshold→trigger decision pipeline specifically, so it
        // can be isolated in Logcat (`adb logcat -s SCREENTIME_TRIGGER`) independent of the more
        // general service lifecycle logs above under TAG.
        private const val TRIGGER_TAG = "SCREENTIME_TRIGGER"
        // Dedicated tag for the continuous in-app-overlay intervention loop specifically, so its
        // poll -> threshold -> addView() chain can be isolated in Logcat
        // (`adb logcat -s OVERLAY_TRIGGER`) independent of the daily-limit/focus-block logs above.
        private const val OVERLAY_TAG = "OVERLAY_TRIGGER"
        private const val MONITOR_INTERVAL_MS = 60_000L
        private const val FOCUS_BLOCK_POLL_INTERVAL_MS = 7_000L
        // Poll cadence for the continuous overlay-intervention loop; spec calls for 3-5s.
        private const val OVERLAY_POLL_INTERVAL_MS = 4_000L
        // Test-artifact constant, not wired to onboarding/settings — short on purpose so a real
        // test session doesn't require sitting through a full "production" daily limit to see the
        // overlay fire. Adjust directly here for a given test run.
        private const val OVERLAY_SESSION_THRESHOLD_SECONDS = 90L
        private const val INITIAL_LOOKBACK_MILLIS = 6 * 60 * 60 * 1000L

        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, ScreenTimeMonitorService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, ScreenTimeMonitorService::class.java))
        }
    }
}
