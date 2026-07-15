package com.julieasoreng.touchgrass.data.usage

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process
import java.util.Calendar
import java.util.concurrent.TimeUnit

data class ScreenTimeBaseline(
    val dailyAverageMillis: Long,
    val daysOfData: Int
)

class ScreenTimeRepository(private val context: Context) {

    companion object {
        private const val DAYS_TO_QUERY = 7

        private val SOCIAL_MEDIA_PACKAGES = setOf(
            "com.instagram.android",
            "com.zhiliaoapp.musically", // TikTok
            "com.snapchat.android",
            "com.google.android.youtube",
            "com.facebook.katana",
            "com.twitter.android" // X / Twitter
        )
    }

    fun hasUsageAccessPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /**
     * Aggregates foreground time for [SOCIAL_MEDIA_PACKAGES] over the last [DAYS_TO_QUERY] days,
     * bucketed by calendar day, and averages it over however many of those days actually have
     * usage history (a freshly installed/reset device may have fewer).
     */
    fun getSocialMediaBaseline(): ScreenTimeBaseline {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val endTime = System.currentTimeMillis()
        val startTime = endTime - TimeUnit.DAYS.toMillis(DAYS_TO_QUERY.toLong())

        val statsList = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        ) ?: emptyList()

        val calendar = Calendar.getInstance()
        fun dayBucket(timestamp: Long): Int {
            calendar.timeInMillis = timestamp
            return calendar.get(Calendar.YEAR) * 1000 + calendar.get(Calendar.DAY_OF_YEAR)
        }

        val daysWithAnyData = mutableSetOf<Int>()
        val socialMillisByDay = mutableMapOf<Int, Long>()

        for (stats in statsList) {
            if (stats.totalTimeInForeground <= 0L) continue
            val day = dayBucket(stats.firstTimeStamp)
            daysWithAnyData += day
            if (stats.packageName in SOCIAL_MEDIA_PACKAGES) {
                socialMillisByDay[day] = (socialMillisByDay[day] ?: 0L) + stats.totalTimeInForeground
            }
        }

        val daysOfData = daysWithAnyData.size.coerceAtMost(DAYS_TO_QUERY)
        if (daysOfData == 0) {
            return ScreenTimeBaseline(dailyAverageMillis = 0L, daysOfData = 0)
        }

        val totalSocialMillis = socialMillisByDay.values.sum()
        return ScreenTimeBaseline(
            dailyAverageMillis = totalSocialMillis / daysOfData,
            daysOfData = daysOfData
        )
    }
}
