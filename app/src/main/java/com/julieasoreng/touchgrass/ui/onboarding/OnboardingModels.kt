package com.julieasoreng.touchgrass.ui.onboarding

enum class OnboardingStep {
    USAGE,
    TARGET,
    SCROLL_TIMES,
    REPLACEMENT
}

data class OnboardingAnswers(
    val currentUsage: String = "",
    val targetUsage: String = "",
    val scrollTimes: List<String> = emptyList(),
    val replacementActivities: List<String> = emptyList()
)
