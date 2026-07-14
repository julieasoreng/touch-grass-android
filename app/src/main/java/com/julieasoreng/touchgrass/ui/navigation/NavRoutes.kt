package com.julieasoreng.touchgrass.ui.navigation

object NavRoutes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"

    const val SET_DURATION = "goals/{goalId}/duration"
    const val ACTIVE_TIMER = "goals/{goalId}/timer/{minutes}"
    const val WEEKLY_SUMMARY = "goals/summary"

    fun setDuration(goalId: String) = "goals/$goalId/duration"
    fun activeTimer(goalId: String, minutes: Int) = "goals/$goalId/timer/$minutes"
}
