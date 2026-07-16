package com.julieasoreng.touchgrass.ui.lock

import android.app.admin.DevicePolicyManager
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.julieasoreng.touchgrass.admin.TouchGrassDeviceAdminReceiver
import com.julieasoreng.touchgrass.data.preferences.LockFeaturePreferencesRepository
import com.julieasoreng.touchgrass.data.preferences.LockFeatureState
import com.julieasoreng.touchgrass.service.MonitoringWatchdogWorker
import com.julieasoreng.touchgrass.service.ScreenTimeMonitorService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LockFeatureViewModel(
    private val repository: LockFeaturePreferencesRepository
) : ViewModel() {

    val state: StateFlow<LockFeatureState> = repository.state.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = LockFeatureState()
    )

    fun setDailyLimitMinutes(minutes: Int) {
        viewModelScope.launch { repository.setDailyLimitMinutes(minutes) }
    }

    fun dismissLockNudge() {
        viewModelScope.launch { repository.dismissLockNudge() }
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

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return LockFeatureViewModel(repository) as T
    }
}
