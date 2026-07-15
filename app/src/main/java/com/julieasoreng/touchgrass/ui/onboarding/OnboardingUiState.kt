package com.julieasoreng.touchgrass.ui.onboarding

data class OnboardingUiState(
    val step: OnboardingStep = OnboardingStep.USAGE_PERMISSION,
    val hasUsagePermission: Boolean = false,
    val isLoadingBaseline: Boolean = false,
    val hasLoadedBaseline: Boolean = false,
    val answers: OnboardingAnswers = OnboardingAnswers(),
    val isEnteringCustomTarget: Boolean = false,
    val customTargetInputText: String = "",
    val selectedScrollTimes: Set<String> = emptySet(),
    val selectedReplacementActivities: List<String> = emptyList(),
    val customActivityText: String = "",
    val isAdvancing: Boolean = false
)
