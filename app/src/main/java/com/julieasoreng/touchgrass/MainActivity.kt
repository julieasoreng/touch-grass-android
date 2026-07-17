package com.julieasoreng.touchgrass

import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.julieasoreng.touchgrass.admin.TouchGrassDeviceAdminReceiver
import com.julieasoreng.touchgrass.data.preferences.LockFeaturePreferencesRepository
import com.julieasoreng.touchgrass.service.MonitoringWatchdogWorker
import com.julieasoreng.touchgrass.service.ScreenTimeMonitorService
import com.julieasoreng.touchgrass.ui.navigation.BloomNavHost
import com.julieasoreng.touchgrass.ui.theme.BloomTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var showPostUnlock by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showPostUnlock = intent.getBooleanExtra(EXTRA_SHOW_POST_UNLOCK, false)
        resumeMonitoringIfNeeded()
        setContent {
            BloomTheme {
                BloomNavHost(
                    showPostUnlock = showPostUnlock,
                    onPostUnlockConsumed = { showPostUnlock = false }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(EXTRA_SHOW_POST_UNLOCK, false)) {
            showPostUnlock = true
        }
    }

    /**
     * Recovers screen-time monitoring after a process death (crash, or OS/battery-optimization
     * kill that a background WorkManager tick couldn't safely recover from — see
     * MonitoringWatchdogWorker) on every normal app open, not just from the one-time Device Admin
     * permission screen, which was previously the only place this got (re)started.
     */
    private fun resumeMonitoringIfNeeded() {
        val devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val isActive = devicePolicyManager.isAdminActive(TouchGrassDeviceAdminReceiver.componentName(this))
        val repository = LockFeaturePreferencesRepository(applicationContext)
        lifecycleScope.launch { repository.setDeviceAdminActive(isActive) }
        if (isActive) {
            ScreenTimeMonitorService.start(this)
            MonitoringWatchdogWorker.schedule(this)
        }
    }

    companion object {
        const val EXTRA_SHOW_POST_UNLOCK = "show_post_unlock"
    }
}
