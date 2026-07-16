package com.julieasoreng.touchgrass.data.goals

import androidx.compose.ui.graphics.Color
import com.julieasoreng.touchgrass.ui.theme.GoalsChevron
import com.julieasoreng.touchgrass.ui.theme.GoalsDustyRose
import com.julieasoreng.touchgrass.ui.theme.GoalsLavender
import com.julieasoreng.touchgrass.ui.theme.GoalsMint
import com.julieasoreng.touchgrass.ui.theme.GoalsMintDark
import com.julieasoreng.touchgrass.ui.theme.GoalsPeach
import com.julieasoreng.touchgrass.ui.theme.GoalsPurple

/** A goal's visual identity — one fixed icon + color pair. The single source of truth for every
 *  screen that renders a goal (goal card, weekly calendar, focus timer ring), so an activity
 *  always looks the same everywhere instead of each screen picking its own value. */
data class ActivityStyle(val icon: String, val color: Color)

/** Fallback for custom/free-text activities that aren't one of the seeded presets below. */
val DEFAULT_ACTIVITY_STYLE = ActivityStyle(icon = "🌱", color = GoalsChevron)

/** One-time seed styles for the onboarding-predefined replacement activities. Keyed by name rather
 *  than list position, so an activity's color can't drift depending on where it lands in a list. */
private val seedActivityStyles = mapOf(
    "Reading" to ActivityStyle("📖", GoalsMint),
    "Writing" to ActivityStyle("✍️", GoalsPeach),
    "Painting" to ActivityStyle("🎨", GoalsLavender),
    "Dancing" to ActivityStyle("💃", GoalsDustyRose),
    "Exercise" to ActivityStyle("🏃", GoalsPurple),
    "Journaling" to ActivityStyle("📓", GoalsMintDark)
)

fun seedActivityStyle(name: String): ActivityStyle = seedActivityStyles[name] ?: DEFAULT_ACTIVITY_STYLE

/** Icon choices offered when a user manually adds a goal. */
val ACTIVITY_ICON_OPTIONS = listOf("📖", "🏋️", "✍️", "🎨", "🧘", "🎵", "🚶", "💃", "📓", "🌱")

/** Color swatches offered when a user manually adds a goal — existing Goals palette tokens only. */
val ACTIVITY_COLOR_OPTIONS = listOf(GoalsMint, GoalsPeach, GoalsLavender, GoalsDustyRose, GoalsPurple, GoalsMintDark)
