package com.julieasoreng.touchgrass.ui.goals

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.julieasoreng.touchgrass.data.goals.CompletedSession
import com.julieasoreng.touchgrass.data.goals.CompletedSessionsRepository
import com.julieasoreng.touchgrass.data.goals.Goal
import com.julieasoreng.touchgrass.data.goals.currentFocusStreakDays
import com.julieasoreng.touchgrass.data.goals.sessionsWithinCurrentWeek
import com.julieasoreng.touchgrass.data.preferences.OnboardingPreferencesRepository
import com.julieasoreng.touchgrass.data.usage.ScreenTimeRepository
import com.julieasoreng.touchgrass.ui.theme.GoalsLavender
import com.julieasoreng.touchgrass.ui.theme.GoalsMint
import com.julieasoreng.touchgrass.ui.theme.GoalsPeach
import java.util.UUID
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

private const val ONBOARDING_GOAL_ID_PREFIX = "onboarding-"

private val goalColorPalette = listOf(GoalsMint, GoalsPeach, GoalsLavender)

private val knownActivityEmojis = mapOf(
    "Reading" to "📖",
    "Writing" to "✍️",
    "Painting" to "🎨",
    "Dancing" to "💃",
    "Exercise" to "🏃",
    "Journaling" to "📓"
)

private fun emojiForActivity(name: String): String = knownActivityEmojis[name] ?: "🌱"

private fun goalIdForActivity(name: String): String = ONBOARDING_GOAL_ID_PREFIX + name.lowercase().replace(Regex("\\s+"), "-")

class GoalsViewModel(
    private val onboardingPreferencesRepository: OnboardingPreferencesRepository,
    private val completedSessionsRepository: CompletedSessionsRepository,
    private val screenTimeRepository: ScreenTimeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GoalsUiState())
    val uiState: StateFlow<GoalsUiState> = _uiState.asStateFlow()

    private val _sessionEnded = Channel<Unit>(Channel.BUFFERED)
    val sessionEnded: Flow<Unit> = _sessionEnded.receiveAsFlow()

    private var tickJob: Job? = null

    init {
        viewModelScope.launch {
            combine(
                onboardingPreferencesRepository.replacementActivities,
                completedSessionsRepository.sessions
            ) { activities, sessions -> activities to sessions }
                .collect { (activities, sessions) ->
                    _uiState.update { state ->
                        val goalsWithIdentity = applyOnboardingActivities(state.goals, activities)
                        state.copy(
                            goals = applyWeeklyMinutes(goalsWithIdentity, sessionsWithinCurrentWeek(sessions)),
                            focusStreakDays = currentFocusStreakDays(sessions)
                        )
                    }
                }
        }
        viewModelScope.launch { loadScrollComparison() }
    }

    /** Rebuilds onboarding-derived goals from the current activity list, but leaves any manually
     *  added goals (from the "+ Add a new goal" dialog) untouched. */
    private fun applyOnboardingActivities(currentGoals: List<Goal>, activities: List<String>): List<Goal> {
        val existingById = currentGoals.associateBy { it.id }
        val onboardingGoals = activities.mapIndexed { index, name ->
            val id = goalIdForActivity(name)
            existingById[id]?.copy(name = name) ?: Goal(
                id = id,
                name = name,
                emoji = emojiForActivity(name),
                color = goalColorPalette[index % goalColorPalette.size],
                weeklyMinutes = 0
            )
        }
        val manuallyAddedGoals = currentGoals.filterNot { it.id.startsWith(ONBOARDING_GOAL_ID_PREFIX) }
        return onboardingGoals + manuallyAddedGoals
    }

    /** Recomputes every goal's weeklyMinutes from this week's logged sessions — the single source
     *  of truth, replacing the old in-memory running total. */
    private fun applyWeeklyMinutes(goals: List<Goal>, weekSessions: List<CompletedSession>): List<Goal> {
        val minutesByGoalId = weekSessions.groupBy { it.goalId }.mapValues { (_, sessions) -> sessions.sumOf { it.minutes } }
        return goals.map { goal -> goal.copy(weeklyMinutes = minutesByGoalId[goal.id] ?: 0) }
    }

    /** One-time "before" (onboarding baseline) vs. "after" (freshly measured) scroll-time comparison. */
    private suspend fun loadScrollComparison() {
        val beforeMillis = onboardingPreferencesRepository.dailyAverageScreenTimeMillis.first()
        val afterMillis = if (screenTimeRepository.hasUsageAccessPermission()) {
            withContext(Dispatchers.IO) { screenTimeRepository.getSocialMediaBaseline().dailyAverageMillis }
        } else {
            0L
        }
        _uiState.update {
            it.copy(
                dailyScrollBeforeMinutes = (beforeMillis / 60_000L).toInt(),
                dailyScrollThisWeekMinutes = (afterMillis / 60_000L).toInt()
            )
        }
    }

    fun addGoal(name: String, emoji: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        _uiState.update { state ->
            val color = goalColorPalette[state.goals.size % goalColorPalette.size]
            val goal = Goal(
                id = UUID.randomUUID().toString(),
                name = trimmed,
                emoji = emoji,
                color = color,
                weeklyMinutes = 0
            )
            state.copy(goals = state.goals + goal)
        }
    }

    fun removeGoal(goalId: String) {
        _uiState.update { state -> state.copy(goals = state.goals.filterNot { it.id == goalId }) }
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

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return GoalsViewModel(onboardingPreferencesRepository, completedSessionsRepository, screenTimeRepository) as T
    }
}
