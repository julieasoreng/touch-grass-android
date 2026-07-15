package com.julieasoreng.touchgrass.ui.onboarding

data class OnboardingUiState(
    val step: OnboardingStep = OnboardingStep.USAGE_PERMISSION,
    val hasUsagePermission: Boolean = false,
    val isLoadingScreenTimeData: Boolean = false,
    val hasLoadedScreenTimeData: Boolean = false,
    val answers: OnboardingAnswers = OnboardingAnswers(),
    val isEnteringCustomTarget: Boolean = false,
    val customTargetInputText: String = "",
    val selectedReplacementActivities: List<String> = emptyList(),
    val customActivityText: String = "",
    val isAdvancing: Boolean = false
)
