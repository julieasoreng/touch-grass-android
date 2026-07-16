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
data class ActivityStyle(val icon: ActivityIcon, val color: Color)

/** Fallback for custom/free-text activities that aren't one of the seeded presets below. */
val DEFAULT_ACTIVITY_STYLE = ActivityStyle(icon = ActivityIcon.GARDENING, color = GoalsChevron)

/** One-time seed styles for the onboarding-predefined replacement activities. Keyed by name rather
 *  than list position, so an activity's color can't drift depending on where it lands in a list.
 *  "Writing" and "Dancing" don't have a dedicated icon in the design set yet, so they borrow the
 *  closest existing one (Journaling, Music) until a matching icon is added. */
private val seedActivityStyles = mapOf(
    "Reading" to ActivityStyle(ActivityIcon.READING, GoalsMint),
    "Writing" to ActivityStyle(ActivityIcon.JOURNALING, GoalsPeach),
    "Painting" to ActivityStyle(ActivityIcon.ART, GoalsLavender),
    "Dancing" to ActivityStyle(ActivityIcon.MUSIC, GoalsDustyRose),
    "Exercise" to ActivityStyle(ActivityIcon.EXERCISE, GoalsPurple),
    "Journaling" to ActivityStyle(ActivityIcon.JOURNALING, GoalsMintDark)
)

fun seedActivityStyle(name: String): ActivityStyle = seedActivityStyles[name] ?: DEFAULT_ACTIVITY_STYLE

/** Icon choices offered when a user manually adds a goal — the full design-provided icon set. */
val ACTIVITY_ICON_OPTIONS = listOf(
    ActivityIcon.READING,
    ActivityIcon.EXERCISE,
    ActivityIcon.ART,
    ActivityIcon.MEDITATION,
    ActivityIcon.MUSIC,
    ActivityIcon.WALK,
    ActivityIcon.JOURNALING,
    ActivityIcon.GARDENING,
    ActivityIcon.BREATHING,
    ActivityIcon.COOKING,
    ActivityIcon.PHOTOGRAPHY,
    ActivityIcon.PUZZLE,
    ActivityIcon.SLEEP,
    ActivityIcon.TEA,
    ActivityIcon.CALL
)

/** Color swatches offered when a user manually adds a goal — existing Goals palette tokens only. */
val ACTIVITY_COLOR_OPTIONS = listOf(GoalsMint, GoalsPeach, GoalsLavender, GoalsDustyRose, GoalsPurple, GoalsMintDark)
