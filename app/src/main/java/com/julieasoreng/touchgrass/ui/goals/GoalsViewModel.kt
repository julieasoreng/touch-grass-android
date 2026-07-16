package com.julieasoreng.touchgrass.ui.goals

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.julieasoreng.touchgrass.data.goals.Goal
import com.julieasoreng.touchgrass.data.preferences.OnboardingPreferencesRepository
import com.julieasoreng.touchgrass.ui.theme.GoalsLavender
import com.julieasoreng.touchgrass.ui.theme.GoalsMint
import com.julieasoreng.touchgrass.ui.theme.GoalsPeach
import java.util.UUID
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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

private fun goalIdForActivity(name: String): String = "onboarding-" + name.lowercase().replace(Regex("\\s+"), "-")

class GoalsViewModel(
    private val onboardingPreferencesRepository: OnboardingPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GoalsUiState())
    val uiState: StateFlow<GoalsUiState> = _uiState.asStateFlow()

    private val _sessionEnded = Channel<Unit>(Channel.BUFFERED)
    val sessionEnded: Flow<Unit> = _sessionEnded.receiveAsFlow()

    private var tickJob: Job? = null

    init {
        viewModelScope.launch {
            onboardingPreferencesRepository.replacementActivities.collect { activities ->
                _uiState.update { it.copy(goals = mergeGoalsFromActivities(it.goals, activities)) }
            }
        }
    }

    /** Rebuilds the goal list from the onboarding activities (the source of truth), but keeps the
     *  existing Goal instance — and any weeklyMinutes already logged this session — for activities
     *  that were already present, instead of resetting progress every time this re-emits. */
    private fun mergeGoalsFromActivities(currentGoals: List<Goal>, activities: List<String>): List<Goal> {
        val existingById = currentGoals.associateBy { it.id }
        return activities.mapIndexed { index, name ->
            val id = goalIdForActivity(name)
            existingById[id] ?: Goal(
                id = id,
                name = name,
                emoji = emojiForActivity(name),
                color = goalColorPalette[index % goalColorPalette.size],
                weeklyMinutes = 0
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
        _uiState.update { state ->
            state.copy(
                goals = state.goals.map { goal ->
                    if (goal.id == goalId) goal.copy(weeklyMinutes = goal.weeklyMinutes + minutes) else goal
                }
            )
        }
    }
}

class GoalsViewModelFactory(context: Context) : ViewModelProvider.Factory {
    private val onboardingPreferencesRepository = OnboardingPreferencesRepository(context.applicationContext)

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return GoalsViewModel(onboardingPreferencesRepository) as T
    }
}
