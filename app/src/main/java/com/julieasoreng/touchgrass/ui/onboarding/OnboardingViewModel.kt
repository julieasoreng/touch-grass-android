package com.julieasoreng.touchgrass.ui.onboarding

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.julieasoreng.touchgrass.data.preferences.OnboardingPreferencesRepository
import com.julieasoreng.touchgrass.data.usage.ScreenTimeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val AUTO_ADVANCE_DELAY_MS = 600L

class OnboardingViewModel(
    private val preferencesRepository: OnboardingPreferencesRepository,
    private val screenTimeRepository: ScreenTimeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    private val _navigateToHome = Channel<Unit>(Channel.BUFFERED)
    val navigateToHome: Flow<Unit> = _navigateToHome.receiveAsFlow()

    init {
        refreshUsagePermission()
    }

    /** Re-checks Usage Access permission. Called on init and whenever the screen resumes
     *  (e.g. after the user returns from Settings). */
    fun refreshUsagePermission() {
        val granted = screenTimeRepository.hasUsageAccessPermission()
        _uiState.update { it.copy(hasUsagePermission = granted) }

        if (!granted) {
            _uiState.update { it.copy(step = OnboardingStep.USAGE_PERMISSION) }
            return
        }

        if (_uiState.value.step == OnboardingStep.USAGE_PERMISSION) {
            _uiState.update { it.copy(step = OnboardingStep.USAGE) }
        }
        if (!_uiState.value.hasLoadedBaseline) {
            loadScreenTimeBaseline()
        }
    }

    private fun loadScreenTimeBaseline() {
        _uiState.update { it.copy(isLoadingBaseline = true) }
        viewModelScope.launch {
            val baseline = withContext(Dispatchers.IO) { screenTimeRepository.getSocialMediaBaseline() }
            _uiState.update {
                it.copy(
                    answers = it.answers.copy(
                        dailyAverageScreenTimeMillis = baseline.dailyAverageMillis,
                        screenTimeDaysOfData = baseline.daysOfData
                    ),
                    isLoadingBaseline = false,
                    hasLoadedBaseline = true
                )
            }
        }
    }

    fun confirmUsageBaseline() {
        if (_uiState.value.isLoadingBaseline) return
        _uiState.update { it.copy(step = OnboardingStep.TARGET) }
    }

    fun selectTarget(option: String) = advanceSingleSelect(
        applyAnswer = { it.copy(targetUsage = option) },
        nextStep = OnboardingStep.SCROLL_TIMES
    )

    private fun advanceSingleSelect(
        applyAnswer: (OnboardingAnswers) -> OnboardingAnswers,
        nextStep: OnboardingStep
    ) {
        if (_uiState.value.isAdvancing) return
        _uiState.update { it.copy(answers = applyAnswer(it.answers), isAdvancing = true) }
        viewModelScope.launch {
            delay(AUTO_ADVANCE_DELAY_MS)
            _uiState.update { it.copy(step = nextStep, isAdvancing = false) }
        }
    }

    fun toggleScrollTime(option: String) {
        _uiState.update { state ->
            val updated = if (option in state.selectedScrollTimes) {
                state.selectedScrollTimes - option
            } else {
                state.selectedScrollTimes + option
            }
            state.copy(selectedScrollTimes = updated)
        }
    }

    fun confirmScrollTimes() {
        val state = _uiState.value
        if (state.selectedScrollTimes.isEmpty()) return
        _uiState.update {
            it.copy(
                answers = it.answers.copy(scrollTimes = it.selectedScrollTimes.toList()),
                step = OnboardingStep.REPLACEMENT
            )
        }
    }

    fun toggleReplacementActivity(option: String) {
        _uiState.update { state ->
            val current = state.selectedReplacementActivities
            val updated = if (option in current) current - option else current + option
            state.copy(selectedReplacementActivities = updated)
        }
    }

    fun updateCustomActivityText(text: String) {
        _uiState.update { it.copy(customActivityText = text) }
    }

    fun addCustomActivity() {
        val text = _uiState.value.customActivityText.trim()
        if (text.isEmpty()) return
        _uiState.update {
            it.copy(
                selectedReplacementActivities = it.selectedReplacementActivities + text,
                customActivityText = ""
            )
        }
    }

    fun completeOnboarding() {
        val state = _uiState.value
        if (state.selectedReplacementActivities.isEmpty()) return
        val finalAnswers = state.answers.copy(replacementActivities = state.selectedReplacementActivities)
        _uiState.update { it.copy(answers = finalAnswers) }
        viewModelScope.launch {
            preferencesRepository.saveOnboardingAnswers(finalAnswers)
            _navigateToHome.send(Unit)
        }
    }

    fun goBack(): Boolean {
        val current = _uiState.value.step
        if (current.ordinal <= OnboardingStep.USAGE.ordinal) return false
        val previous = OnboardingStep.entries[current.ordinal - 1]
        _uiState.update { it.copy(step = previous) }
        return true
    }
}

class OnboardingViewModelFactory(context: Context) : ViewModelProvider.Factory {
    private val preferencesRepository = OnboardingPreferencesRepository(context.applicationContext)
    private val screenTimeRepository = ScreenTimeRepository(context.applicationContext)

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return OnboardingViewModel(preferencesRepository, screenTimeRepository) as T
    }
}
