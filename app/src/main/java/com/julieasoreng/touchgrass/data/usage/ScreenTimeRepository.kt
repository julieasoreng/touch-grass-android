package com.julieasoreng.touchgrass.data.usage

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process
import java.util.Calendar
import java.util.concurrent.TimeUnit

data class ScreenTimeBaseline(
    val dailyAverageMillis: Long,
    val daysOfData: Int
)

enum class ScrollTimePattern {
    MORNING,
    MIDDAY,
    AFTER_WORK,
    BEFORE_BED,
    SPREAD_OUT
}

data class ScrollTimeInsight(
    val dominantPattern: ScrollTimePattern,
    val daysOfData: Int
)

class ScreenTimeRepository(private val context: Context) {

    companion object {
        private const val DAYS_TO_QUERY = 7

        // How close a runner-up bucket needs to be to the leading bucket (as a fraction of the
        // leader's total) before we treat the result as "no dominant pattern" instead of forcing a winner.
        private const val DOMINANCE_MARGIN = 0.10

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

    /**
     * Pairs ACTIVITY_RESUMED/ACTIVITY_PAUSED events for [SOCIAL_MEDIA_PACKAGES] into sessions over
     * the last [DAYS_TO_QUERY] days, splits each session across the fixed time-of-day buckets it
     * overlaps, and reports whichever bucket accumulated the most time — or [ScrollTimePattern.SPREAD_OUT]
     * if no single bucket clearly leads.
     */
    fun getScrollTimeInsight(): ScrollTimeInsight {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val endTime = System.currentTimeMillis()
        val startTime = endTime - TimeUnit.DAYS.toMillis(DAYS_TO_QUERY.toLong())

        val events = usageStatsManager.queryEvents(startTime, endTime)
        val bucketMillis = mutableMapOf<ScrollTimePattern, Long>()
        val daysWithAnyData = mutableSetOf<Int>()
        val pendingResumeByPackage = mutableMapOf<String, Long>()
        val calendar = Calendar.getInstance()

        fun dayOf(timestamp: Long): Int {
            calendar.timeInMillis = timestamp
            return calendar.get(Calendar.YEAR) * 1000 + calendar.get(Calendar.DAY_OF_YEAR)
        }

        fun addSession(sessionStart: Long, sessionEnd: Long) {
            if (sessionEnd <= sessionStart) return
            daysWithAnyData += dayOf(sessionStart)

            var segmentStart = sessionStart
            while (segmentStart < sessionEnd) {
                calendar.timeInMillis = segmentStart
                val hour = calendar.get(Calendar.HOUR_OF_DAY)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                calendar.add(Calendar.HOUR_OF_DAY, 1)
                val segmentEnd = minOf(sessionEnd, calendar.timeInMillis)

                bucketOf(hour)?.let { bucket ->
                    bucketMillis[bucket] = (bucketMillis[bucket] ?: 0L) + (segmentEnd - segmentStart)
                }
                segmentStart = segmentEnd
            }
        }

        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.packageName !in SOCIAL_MEDIA_PACKAGES) continue
            when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> pendingResumeByPackage[event.packageName] = event.timeStamp
                UsageEvents.Event.ACTIVITY_PAUSED -> {
                    val resumeTs = pendingResumeByPackage.remove(event.packageName)
                    if (resumeTs != null) addSession(resumeTs, event.timeStamp)
                }
            }
        }

        return ScrollTimeInsight(
            dominantPattern = dominantBucket(bucketMillis),
            daysOfData = daysWithAnyData.size.coerceAtMost(DAYS_TO_QUERY)
        )
    }

    private fun bucketOf(hourOfDay: Int): ScrollTimePattern? = when (hourOfDay) {
        in 6..10 -> ScrollTimePattern.MORNING // 06:00–11:00
        in 11..16 -> ScrollTimePattern.MIDDAY // 11:00–17:00
        in 17..21 -> ScrollTimePattern.AFTER_WORK // 17:00–22:00
        22, 23, 0 -> ScrollTimePattern.BEFORE_BED // 22:00–01:00
        else -> null
    }

    private fun dominantBucket(bucketMillis: Map<ScrollTimePattern, Long>): ScrollTimePattern {
        val ranked = bucketMillis.entries.sortedByDescending { it.value }
        val leader = ranked.firstOrNull { it.value > 0L } ?: return ScrollTimePattern.SPREAD_OUT
        val runnerUpMillis = ranked.getOrNull(1)?.value ?: 0L
        val tooClose = runnerUpMillis > 0L && (leader.value - runnerUpMillis).toDouble() / leader.value <= DOMINANCE_MARGIN
        return if (tooClose) ScrollTimePattern.SPREAD_OUT else leader.key
    }
}
