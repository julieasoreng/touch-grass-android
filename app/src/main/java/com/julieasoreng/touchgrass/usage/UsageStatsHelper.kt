package com.julieasoreng.touchgrass.usage

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process
import com.julieasoreng.touchgrass.data.usage.SOCIAL_MEDIA_PACKAGES
import java.util.Calendar

object UsageStatsHelper {

    @Suppress("DEPRECATION")
    fun hasUsageAccess(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        // checkOpNoThrow (not unsafeCheckOpNoThrow) since minSdk 26 is below the API 29 floor
        // for the newer overload.
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /** Returns 0 if usage-access hasn't been granted yet, rather than throwing. */
    fun screenTimeTodayMinutes(context: Context): Int {
        if (!hasUsageAccess(context)) return 0
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val endTime = System.currentTimeMillis()
        val startTime = startOfTodayMillis()
        val stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime)
        val totalForegroundMillis = stats.orEmpty().sumOf { it.totalTimeInForeground }
        return (totalForegroundMillis / 60_000L).toInt()
    }

    /** Same day-boundary and permission handling as [screenTimeTodayMinutes], filtered to the
     *  same social media app list used for the onboarding baseline and scroll-time detection. */
    fun socialMediaScreenTimeTodayMinutes(context: Context): Int {
        if (!hasUsageAccess(context)) return 0
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val endTime = System.currentTimeMillis()
        val startTime = startOfTodayMillis()
        val stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime)
        val socialMillis = stats.orEmpty()
            .filter { it.packageName in SOCIAL_MEDIA_PACKAGES }
            .sumOf { it.totalTimeInForeground }
        return (socialMillis / 60_000L).toInt()
    }

    private fun startOfTodayMillis(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
}
