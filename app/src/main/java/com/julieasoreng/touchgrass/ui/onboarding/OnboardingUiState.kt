package com.julieasoreng.touchgrass.ui.onboarding

data class OnboardingUiState(
    val step: OnboardingStep = OnboardingStep.USAGE,
    val answers: OnboardingAnswers = OnboardingAnswers(),
    val selectedScrollTimes: Set<String> = emptySet(),
    val selectedReplacementActivities: List<String> = emptyList(),
    val customActivityText: String = "",
    val isAdvancing: Boolean = false
)
