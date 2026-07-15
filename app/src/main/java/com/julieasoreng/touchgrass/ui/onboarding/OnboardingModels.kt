package com.julieasoreng.touchgrass.ui.onboarding

import com.julieasoreng.touchgrass.data.usage.ScrollTimePattern

enum class OnboardingStep {
    USAGE_PERMISSION,
    USAGE,
    TARGET,
    SCROLL_TIMES,
    REPLACEMENT
}

enum class TargetPreset(val label: String, val reductionPercent: Int) {
    A_LITTLE("A little", 10),
    MODERATE("Moderate", 25),
    A_LOT("A lot", 50);

    fun targetMillis(baselineMillis: Long): Long = baselineMillis * (100 - reductionPercent) / 100
}

data class OnboardingAnswers(
    val dailyAverageScreenTimeMillis: Long = 0L,
    val screenTimeDaysOfData: Int = 0,
    val targetScreenTimeMillis: Long? = null,
    val targetPreset: TargetPreset? = null,
    val scrollTimePattern: ScrollTimePattern? = null,
    val scrollTimePatternDaysOfData: Int = 0,
    val replacementActivities: List<String> = emptyList()
)
