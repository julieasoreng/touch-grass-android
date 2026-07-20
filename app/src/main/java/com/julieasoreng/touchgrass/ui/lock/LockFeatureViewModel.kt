package com.julieasoreng.touchgrass.ui.lock

import android.app.admin.DevicePolicyManager
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.julieasoreng.touchgrass.admin.TouchGrassDeviceAdminReceiver
import com.julieasoreng.touchgrass.data.preferences.LockFeaturePreferencesRepository
import com.julieasoreng.touchgrass.data.preferences.LockFeatureState
import com.julieasoreng.touchgrass.data.preferences.OnboardingPreferencesRepository
import com.julieasoreng.touchgrass.service.MonitoringWatchdogWorker
import com.julieasoreng.touchgrass.service.ScreenTimeMonitorService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

const val MIN_DAILY_LIMIT_MINUTES = 5
const val MAX_DAILY_LIMIT_MINUTES = 480
const val DAILY_LIMIT_STEP_MINUTES = 5

class LockFeatureViewModel(
    private val repository: LockFeaturePreferencesRepository,
    private val onboardingRepository: OnboardingPreferencesRepository
) : ViewModel() {

    val state: StateFlow<LockFeatureState> = repository.state.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = LockFeatureState()
    )

    init {
        seedDailyLimitFromOnboardingTargetIfUnset()
    }

    /** The daily-limit chips (total screen time) and the onboarding target (social-media-only
     *  time) measure different things and stay independent — but defaulting this screen to a
     *  flat 60min the first time it's shown, right after the user just told onboarding what they
     *  actually wanted, would be a jarring disconnect. Seed once from that target as a sensible
     *  starting point; once the user (or this seed) has explicitly set a value, never overwrite
     *  it again. */
    private fun seedDailyLimitFromOnboardingTargetIfUnset() {
        viewModelScope.launch {
            if (repository.hasExplicitDailyLimit()) return@launch
            val targetMillis = onboardingRepository.targetScreenTimeMillis.first() ?: return@launch
            if (targetMillis <= 0L) return@launch
            val targetMinutes = (targetMillis / 60_000L).toInt()
            val rounded = Math.round(targetMinutes / DAILY_LIMIT_STEP_MINUTES.toDouble()).toInt() * DAILY_LIMIT_STEP_MINUTES
            setDailyLimitMinutes(rounded)
        }
    }

    fun setDailyLimitMinutes(minutes: Int) {
        val clamped = minutes.coerceIn(MIN_DAILY_LIMIT_MINUTES, MAX_DAILY_LIMIT_MINUTES)
        viewModelScope.launch { repository.setDailyLimitMinutes(clamped) }
    }

    fun dismissLockNudge() {
        viewModelScope.launch { repository.dismissLockNudge() }
    }

    /** Debug-only: lets a tester re-trip the daily-limit trigger without waiting for midnight. */
    fun debugResetDailyLockGate() {
        viewModelScope.launch { repository.debugResetDailyLockGate() }
    }

    /**
     * DevicePolicyManager is the real source of truth for admin-active status — the user can
     * revoke it anytime from system Settings without the app running, so this reconciles our
     * cached DataStore copy against it whenever the permission screen is shown or resumed.
     */
    fun refreshAdminState(context: Context) {
        val devicePolicyManager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val isActive = devicePolicyManager.isAdminActive(TouchGrassDeviceAdminReceiver.componentName(context))
        viewModelScope.launch { repository.setDeviceAdminActive(isActive) }
        if (isActive) {
            ScreenTimeMonitorService.start(context)
            MonitoringWatchdogWorker.schedule(context)
        } else {
            ScreenTimeMonitorService.stop(context)
            MonitoringWatchdogWorker.cancel(context)
        }
    }
}

class LockFeatureViewModelFactory(context: Context) : ViewModelProvider.Factory {
    private val repository = LockFeaturePreferencesRepository(context.applicationContext)
    private val onboardingRepository = OnboardingPreferencesRepository(context.applicationContext)

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return LockFeatureViewModel(repository, onboardingRepository) as T
    }
}
