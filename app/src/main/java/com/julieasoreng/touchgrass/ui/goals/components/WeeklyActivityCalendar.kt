package com.julieasoreng.touchgrass.ui.goals.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.julieasoreng.touchgrass.data.goals.CalendarDay
import com.julieasoreng.touchgrass.data.goals.Goal
import com.julieasoreng.touchgrass.ui.theme.GoalsNeutralTrack
import com.julieasoreng.touchgrass.ui.theme.GoalsTextMuted
import com.julieasoreng.touchgrass.ui.theme.GoalsTextPrimary
import com.julieasoreng.touchgrass.ui.theme.Inter

private val dayLabels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
private val barHeight = 88.dp

/** A Mon–Sun view of logged focus-session time, one stacked bar per day (proportional composition
 *  of that day's activities), with a legend matching each activity's existing icon/color. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WeeklyActivityCalendar(
    days: List<CalendarDay>,
    goals: List<Goal>,
    dailyTargetMinutes: Int,
    modifier: Modifier = Modifier
) {
    val goalsById = goals.associateBy { it.id }
    val dayTotals = days.map { day -> day.activityMinutes.sumOf { it.minutes } }
    // Onboarding validation should always give us a positive target, but fall back to this week's
    // own busiest day rather than dividing by zero if one somehow isn't set yet.
    val effectiveTarget = if (dailyTargetMinutes > 0) dailyTargetMinutes else (dayTotals.maxOrNull() ?: 0)

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            days.forEachIndexed { index, day ->
                val totalMinutes = dayTotals[index]
                val dayFraction = if (effectiveTarget > 0) (totalMinutes.toFloat() / effectiveTarget).coerceIn(0f, 1f) else 0f
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .width(18.dp)
                            .height(barHeight)
                            .clip(RoundedCornerShape(6.dp))
                            .background(GoalsNeutralTrack),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        if (totalMinutes > 0) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(dayFraction),
                                verticalArrangement = Arrangement.Bottom
                            ) {
                                day.activityMinutes.sortedByDescending { it.minutes }.forEach { activity ->
                                    val goal = goalsById[activity.goalId]
                                    val fraction = (activity.minutes.toFloat() / totalMinutes).coerceIn(0f, 1f)
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(fraction)
                                            .background(goal?.color ?: GoalsTextMuted)
                                    )
                                }
                            }
                        }
                    }
                    Text(
                        text = dayLabels[index],
                        fontFamily = Inter,
                        fontSize = 11.sp,
                        color = GoalsTextMuted,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
        }

        if (goals.isEmpty()) {
            Text(
                text = "No goals to show yet.",
                fontFamily = Inter,
                fontSize = 12.sp,
                color = GoalsTextMuted
            )
        } else {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                goals.forEach { goal ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(goal.color)
                        )
                        Icon(
                            painter = painterResource(goal.icon.drawableRes),
                            contentDescription = null,
                            tint = goal.color,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = goal.name,
                            fontFamily = Inter,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = GoalsTextPrimary.copy(alpha = 0.85f)
                        )
                    }
                }
            }
        }
    }
}
