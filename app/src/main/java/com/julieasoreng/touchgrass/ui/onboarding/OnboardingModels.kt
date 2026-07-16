package com.julieasoreng.touchgrass.ui.onboarding

import com.julieasoreng.touchgrass.data.usage.ScrollTimePattern

enum class OnboardingStep {
    USAGE_PERMISSION,
    USAGE,
    TARGET,
    SCROLL_TIMES,
    REPLACEMENT,
    INTENTION
}

const val MIN_INTENTION_STATEMENT_LENGTH = 20

enum class TargetPreset(val label: String, val reductionPercent: Int) {
    A_LITTLE("A little", 10),
    MODERATE("Moderate", 25),
    A_LOT("A lot", 50);

    fun targetMillis(baselineMillis: Long): Long = baselineMillis * (100 - reductionPercent) / 100
}

sealed interface TargetReductionEstimate {
    data class Reduction(val percent: Int) : TargetReductionEstimate
    data object Increase : TargetReductionEstimate
    data object BaselineUnavailable : TargetReductionEstimate
}

/** Shared by the preset buttons and the custom-goal input so both agree on the same edge cases. */
fun estimateTargetReduction(baselineMillis: Long, targetMillis: Long): TargetReductionEstimate = when {
    baselineMillis <= 0L -> TargetReductionEstimate.BaselineUnavailable
    targetMillis > baselineMillis -> TargetReductionEstimate.Increase
    else -> TargetReductionEstimate.Reduction((((baselineMillis - targetMillis) * 100L) / baselineMillis).toInt())
}

data class OnboardingAnswers(
    val dailyAverageScreenTimeMillis: Long = 0L,
    val screenTimeDaysOfData: Int = 0,
    val targetScreenTimeMillis: Long? = null,
    val targetPreset: TargetPreset? = null,
    val targetReductionPercent: Int? = null,
    val scrollTimePattern: ScrollTimePattern? = null,
    val scrollTimePatternDaysOfData: Int = 0,
    val replacementActivities: List<String> = emptyList(),
    val intentionStatement: String = "",
    val intentionPrefilledActivity: String = ""
)
