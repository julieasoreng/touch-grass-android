package com.julieasoreng.touchgrass.admin

import android.app.admin.DeviceAdminReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.julieasoreng.touchgrass.data.preferences.LockFeaturePreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TouchGrassDeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        setAdminActive(context, active = true)
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        setAdminActive(context, active = false)
    }

    private fun setAdminActive(context: Context, active: Boolean) {
        val repository = LockFeaturePreferencesRepository(context.applicationContext)
        CoroutineScope(Dispatchers.IO).launch {
            repository.setDeviceAdminActive(active)
        }
    }

    companion object {
        fun componentName(context: Context): ComponentName =
            ComponentName(context.applicationContext, TouchGrassDeviceAdminReceiver::class.java)
    }
}
