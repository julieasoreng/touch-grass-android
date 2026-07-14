package com.julieasoreng.touchgrass.ui.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.julieasoreng.touchgrass.data.goals.Goal
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

private val initialGoals = listOf(
    Goal(id = "read", name = "Read", emoji = "📖", color = GoalsMint, weeklyMinutes = 260),
    Goal(id = "guitar", name = "Play guitar", emoji = "🎸", color = GoalsPeach, weeklyMinutes = 65),
    Goal(id = "study", name = "Study", emoji = "📚", color = GoalsLavender, weeklyMinutes = 160)
)

class GoalsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(GoalsUiState(goals = initialGoals))
    val uiState: StateFlow<GoalsUiState> = _uiState.asStateFlow()

    private val _sessionEnded = Channel<Unit>(Channel.BUFFERED)
    val sessionEnded: Flow<Unit> = _sessionEnded.receiveAsFlow()

    private var tickJob: Job? = null

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

class GoalsViewModelFactory : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return GoalsViewModel() as T
    }
}
