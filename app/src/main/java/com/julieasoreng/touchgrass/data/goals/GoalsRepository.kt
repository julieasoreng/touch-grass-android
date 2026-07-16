package com.julieasoreng.touchgrass.data.goals

import android.content.Context
import androidx.compose.ui.graphics.toArgb
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.goalsDataStore by preferencesDataStore(name = "goals_list_prefs")

private const val ONBOARDING_GOAL_ID_PREFIX = "onboarding-"

private fun goalIdForActivity(name: String): String =
    ONBOARDING_GOAL_ID_PREFIX + name.lowercase().replace(Regex("\\s+"), "-")

data class PersistedGoal(
    val id: String,
    val icon: String,
    val colorArgb: Int,
    val name: String
)

/**
 * The single persisted source of truth for every goal shown on My Goals — both onboarding-seeded
 * and manually added via "+ Add a new goal" — so add/remove survive process death instead of
 * living only in GoalsViewModel's in-memory state, and a removed goal can't come back just
 * because it was re-derived from a separate source on the next read.
 */
class GoalsRepository(private val context: Context) {

    private object Keys {
        val GOALS = stringSetPreferencesKey("goals")
        val SEEDED_ACTIVITY_NAMES = stringSetPreferencesKey("seeded_onboarding_activity_names")
    }

    val goals: Flow<List<PersistedGoal>> = context.goalsDataStore.data
        .map { prefs -> prefs[Keys.GOALS].orEmpty().mapNotNull(::decode) }
        .distinctUntilChanged()

    suspend fun addGoal(name: String, icon: String, colorArgb: Int) {
        val goal = PersistedGoal(id = UUID.randomUUID().toString(), icon = icon, colorArgb = colorArgb, name = name)
        context.goalsDataStore.edit { prefs ->
            prefs[Keys.GOALS] = prefs[Keys.GOALS].orEmpty() + encode(goal)
        }
    }

    suspend fun removeGoal(goalId: String) {
        context.goalsDataStore.edit { prefs ->
            val current = prefs[Keys.GOALS].orEmpty()
            prefs[Keys.GOALS] = current.filterNot { decode(it)?.id == goalId }.toSet()
        }
    }

    /** Adds a persisted goal for any onboarding activity name not already seeded. Once a name has
     *  been seeded it's never re-added, so removing the resulting goal is permanent even though
     *  the onboarding activity list itself is unaffected by that removal. */
    suspend fun seedOnboardingActivities(activityNames: List<String>) {
        val alreadySeeded = context.goalsDataStore.data.map { it[Keys.SEEDED_ACTIVITY_NAMES].orEmpty() }.first()
        val newNames = activityNames.filterNot { it in alreadySeeded }
        if (newNames.isEmpty()) return

        context.goalsDataStore.edit { prefs ->
            val currentGoals = prefs[Keys.GOALS].orEmpty()
            val addedGoals = newNames.map { name ->
                val style = seedActivityStyle(name)
                encode(
                    PersistedGoal(
                        id = goalIdForActivity(name),
                        icon = style.icon,
                        colorArgb = style.color.toArgb(),
                        name = name
                    )
                )
            }
            prefs[Keys.GOALS] = currentGoals + addedGoals
            prefs[Keys.SEEDED_ACTIVITY_NAMES] = alreadySeeded + newNames
        }
    }

    private fun encode(goal: PersistedGoal): String = "${goal.id}|${goal.icon}|${goal.colorArgb}|${goal.name}"

    private fun decode(raw: String): PersistedGoal? {
        val parts = raw.split("|", limit = 4)
        if (parts.size != 4) return null
        val colorArgb = parts[2].toIntOrNull() ?: return null
        return PersistedGoal(id = parts[0], icon = parts[1], colorArgb = colorArgb, name = parts[3])
    }
}
