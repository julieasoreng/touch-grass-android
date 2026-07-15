package com.julieasoreng.touchgrass.ui.onboarding

enum class OnboardingStep {
    USAGE_PERMISSION,
    USAGE,
    TARGET,
    SCROLL_TIMES,
    REPLACEMENT
}

data class OnboardingAnswers(
    val dailyAverageScreenTimeMillis: Long = 0L,
    val screenTimeDaysOfData: Int = 0,
    val targetUsage: String = "",
    val scrollTimes: List<String> = emptyList(),
    val replacementActivities: List<String> = emptyList()
)
