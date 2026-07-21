package com.julieasoreng.touchgrass.usage

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import com.julieasoreng.touchgrass.data.usage.SOCIAL_MEDIA_PACKAGES

/**
 * Raw ACTIVITY_RESUMED/ACTIVITY_PAUSED transitions for [SOCIAL_MEDIA_PACKAGES], read straight off
 * [UsageEvents] rather than [android.app.usage.UsageStats] aggregates. Deliberately dumb — this
 * only reports what happened in [sinceMillis, now); it does not try to track "is a session active"
 * itself, since a short poll window can miss the RESUMED event that started an ongoing session
 * (e.g. the app was opened before this window began). Callers own that state machine so a gap here
 * doesn't get misread as "nothing is happening" — see runOverlayInterventionLoop in
 * ScreenTimeMonitorService.
 */
object UsageEventsHelper {

    enum class TransitionType { RESUMED, PAUSED }

    data class SocialAppTransition(
        val packageName: String,
        val type: TransitionType,
        val timestampMillis: Long
    )

    fun socialMediaTransitionsSince(context: Context, sinceMillis: Long): List<SocialAppTransition> {
        if (!UsageStatsHelper.hasUsageAccess(context)) return emptyList()
        val now = System.currentTimeMillis()
        if (sinceMillis >= now) return emptyList()

        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val events = usageStatsManager.queryEvents(sinceMillis, now)
        val event = UsageEvents.Event()
        val transitions = mutableListOf<SocialAppTransition>()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.packageName !in SOCIAL_MEDIA_PACKAGES) continue
            val type = when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> TransitionType.RESUMED
                UsageEvents.Event.ACTIVITY_PAUSED -> TransitionType.PAUSED
                else -> continue
            }
            transitions.add(SocialAppTransition(event.packageName, type, event.timeStamp))
        }
        return transitions
    }
}
