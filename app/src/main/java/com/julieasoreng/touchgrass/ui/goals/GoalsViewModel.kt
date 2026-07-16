package com.julieasoreng.touchgrass.ui.goals

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.julieasoreng.touchgrass.data.goals.ActivityIcon
import com.julieasoreng.touchgrass.data.goals.CompletedSession
import com.julieasoreng.touchgrass.data.goals.CompletedSessionsRepository
import com.julieasoreng.touchgrass.data.goals.Goal
import com.julieasoreng.touchgrass.data.goals.GoalsRepository
import com.julieasoreng.touchgrass.data.goals.PersistedGoal
import com.julieasoreng.touchgrass.data.goals.sessionsWithinCurrentWeek
import com.julieasoreng.touchgrass.data.goals.weeklyCalendar
import com.julieasoreng.touchgrass.data.preferences.OnboardingPreferencesRepository
import com.julieasoreng.touchgrass.data.usage.ScreenTimeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private fun PersistedGoal.toGoal(): Goal = Goal(
    id = id,
    name = name,
    icon = icon,
    color = Color(colorArgb),
    weeklyMinutes = 0
)

class GoalsViewModel(
    private val onboardingPreferencesRepository: OnboardingPreferencesRepository,
    private val completedSessionsRepository: CompletedSessionsRepository,
    private val screenTimeRepository: ScreenTimeRepository,
    private val goalsRepository: GoalsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GoalsUiState())
    val uiState: StateFlow<GoalsUiState> = _uiState.asStateFlow()

    private val _sessionEnded = Channel<Unit>(Channel.BUFFERED)
    val sessionEnded: Flow<Unit> = _sessionEnded.receiveAsFlow()

    private var tickJob: Job? = null

    init {
        // One-time (per new activity name) seeding into the persisted goals store. Idempotent —
        // see GoalsRepository.seedOnboardingActivities — so a goal the user has since removed
        // doesn't get silently re-created just because this re-runs on every launch.
        viewModelScope.launch {
            onboardingPreferencesRepository.replacementActivities.collect { activities ->
                goalsRepository.seedOnboardingActivities(activities)
            }
        }
        // The persisted goals store is now the single source of truth: add/remove only ever
        // touch it, and this reactively rebuilds the UI list — no more merging it with a
        // separately-tracked in-memory list.
        viewModelScope.launch {
            combine(
                goalsRepository.goals,
                completedSessionsRepository.sessions
            ) { persistedGoals, sessions -> persistedGoals to sessions }
                .collect { (persistedGoals, sessions) ->
                    val goals = persistedGoals.map { it.toGoal() }
                    _uiState.update { state ->
                        state.copy(
                            goals = applyWeeklyMinutes(goals, sessionsWithinCurrentWeek(sessions)),
                            weeklyCalendar = weeklyCalendar(sessions)
                        )
                    }
                }
        }
        viewModelScope.launch { loadScrollComparison() }
    }

    /** Recomputes every goal's weeklyMinutes from this week's logged sessions — the single source
     *  of truth, replacing the old in-memory running total. */
    private fun applyWeeklyMinutes(goals: List<Goal>, weekSessions: List<CompletedSession>): List<Goal> {
        val minutesByGoalId = weekSessions.groupBy { it.goalId }.mapValues { (_, sessions) -> sessions.sumOf { it.minutes } }
        return goals.map { goal -> goal.copy(weeklyMinutes = minutesByGoalId[goal.id] ?: 0) }
    }

    /** One-time "before" (onboarding baseline) vs. "after" (freshly measured) scroll-time comparison,
     *  plus the onboarding-set daily target — the same value the weekly activity chart scales its
     *  bars against. */
    private suspend fun loadScrollComparison() {
        val beforeMillis = onboardingPreferencesRepository.dailyAverageScreenTimeMillis.first()
        val targetMillis = onboardingPreferencesRepository.targetScreenTimeMillis.first() ?: 0L
        val afterBaseline = if (screenTimeRepository.hasUsageAccessPermission()) {
            withContext(Dispatchers.IO) { screenTimeRepository.getSocialMediaBaseline() }
        } else {
            null
        }
        _uiState.update {
            it.copy(
                dailyScrollBeforeMinutes = (beforeMillis / 60_000L).toInt(),
                dailyScrollAfterMinutes = ((afterBaseline?.dailyAverageMillis ?: 0L) / 60_000L).toInt(),
                scrollAfterDaysOfData = afterBaseline?.daysOfData ?: 0,
                dailyTargetMinutes = (targetMillis / 60_000L).toInt()
            )
        }
    }

    fun addGoal(name: String, icon: ActivityIcon, color: Color) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            goalsRepository.addGoal(trimmed, icon, color.toArgb())
        }
    }

    fun removeGoal(goalId: String) {
        viewModelScope.launch { goalsRepository.removeGoal(goalId) }
    }

    fun startSession(goalId: String, minutes: Int) {
        val goal = _uiState.value.goals.find { it.id == goalId } ?: return
        tickJob?.cancel()
        val targetSeconds = minutes * 60
        _uiState.update {
            it.copy(activeSession = ActiveSession(goal = goal, targetSeconds = targetSeconds, remainingSeconds = targetSeconds))
        }
        tickJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                val current = _uiState.value.activeSession ?: break
                val remaining = current.remainingSeconds - 1
                if (remaining <= 0) {
                    completeSession(current.copy(remainingSeconds = 0))
                    break
                } else {
                    _uiState.update { state -> state.copy(activeSession = current.copy(remainingSeconds = remaining)) }
                }
            }
        }
    }

    fun endSessionEarly() {
        val current = _uiState.value.activeSession ?: return
        tickJob?.cancel()
        val elapsedSeconds = current.targetSeconds - current.remainingSeconds
        logReplacedMinutes(current.goal.id, elapsedSeconds / 60)
        _uiState.update { it.copy(activeSession = null) }
        viewModelScope.launch { _sessionEnded.send(Unit) }
    }

    private fun completeSession(session: ActiveSession) {
        logReplacedMinutes(session.goal.id, session.targetSeconds / 60)
        _uiState.update { it.copy(activeSession = null) }
        viewModelScope.launch { _sessionEnded.send(Unit) }
    }

    private fun logReplacedMinutes(goalId: String, minutes: Int) {
        if (minutes <= 0) return
        val completedAt = System.currentTimeMillis()
        viewModelScope.launch {
            completedSessionsRepository.logSession(goalId, minutes, completedAt)
        }
    }
}

class GoalsViewModelFactory(context: Context) : ViewModelProvider.Factory {
    private val onboardingPreferencesRepository = OnboardingPreferencesRepository(context.applicationContext)
    private val completedSessionsRepository = CompletedSessionsRepository(context.applicationContext)
    private val screenTimeRepository = ScreenTimeRepository(context.applicationContext)
    private val goalsRepository = GoalsRepository(context.applicationContext)

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return GoalsViewModel(onboardingPreferencesRepository, completedSessionsRepository, screenTimeRepository, goalsRepository) as T
    }
}
