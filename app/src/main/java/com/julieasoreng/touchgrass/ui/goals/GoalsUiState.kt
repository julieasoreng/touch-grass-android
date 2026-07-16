package com.julieasoreng.touchgrass.ui.goals

import com.julieasoreng.touchgrass.data.goals.CalendarDay
import com.julieasoreng.touchgrass.data.goals.Goal

data class ActiveSession(
    val goal: Goal,
    val targetSeconds: Int,
    val remainingSeconds: Int
)

data class GoalsUiState(
    val goals: List<Goal> = emptyList(),
    val activeSession: ActiveSession? = null,
    val dailyScrollBeforeMinutes: Int = 0,
    val dailyScrollAfterMinutes: Int = 0,
    val scrollAfterDaysOfData: Int = 0,
    val weeklyCalendar: List<CalendarDay> = emptyList()
)
