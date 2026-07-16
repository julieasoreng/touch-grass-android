package com.julieasoreng.touchgrass.ui.goals

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.julieasoreng.touchgrass.ui.goals.components.DonutChart
import com.julieasoreng.touchgrass.ui.goals.components.DonutSlice
import com.julieasoreng.touchgrass.ui.goals.components.WeeklyActivityCalendar
import com.julieasoreng.touchgrass.ui.theme.BloomTheme
import com.julieasoreng.touchgrass.ui.theme.GoalsBackground
import com.julieasoreng.touchgrass.ui.theme.GoalsMintDark
import com.julieasoreng.touchgrass.ui.theme.GoalsTextMuted
import com.julieasoreng.touchgrass.ui.theme.GoalsTextPrimary
import com.julieasoreng.touchgrass.ui.theme.Inter
import com.julieasoreng.touchgrass.ui.theme.Quicksand
import kotlin.math.roundToInt

private const val MIN_DAYS_FOR_AFTER_COMPARISON = 4

@Composable
fun WeeklySummaryScreen(
    viewModel: GoalsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val goals = state.goals
    val totalMinutes = goals.sumOf { it.weeklyMinutes }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(GoalsBackground)
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Text(
            text = "‹",
            fontSize = 22.sp,
            color = GoalsTextMuted,
            modifier = Modifier.clickable(onClick = onBack)
        )

        Column {
            Text(
                text = "This week",
                fontFamily = Quicksand,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = GoalsTextPrimary
            )
            Text(
                text = "You traded scrolling for this",
                fontFamily = Inter,
                fontSize = 14.sp,
                color = GoalsTextMuted
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White)
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            DonutChart(
                slices = goals.map { DonutSlice(value = it.weeklyMinutes.toFloat(), color = it.color) },
                modifier = Modifier.size(104.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = formatMinutes(totalMinutes),
                        fontFamily = Quicksand,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = GoalsTextPrimary
                    )
                    Text("replaced", fontFamily = Inter, fontSize = 10.sp, color = GoalsTextMuted)
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                goals.forEach { goal ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(goal.color)
                        )
                        Text(
                            text = "${goal.name} — ${formatMinutes(goal.weeklyMinutes)}",
                            fontFamily = Inter,
                            fontSize = 13.sp,
                            color = GoalsTextPrimary.copy(alpha = 0.85f)
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White)
                .padding(vertical = 18.dp, horizontal = 20.dp)
        ) {
            Text(
                text = "Scroll time, before vs. after",
                fontFamily = Quicksand,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = GoalsTextPrimary,
                modifier = Modifier.padding(bottom = 14.dp)
            )
            val before = state.dailyScrollBeforeMinutes
            val after = state.dailyScrollAfterMinutes
            if (before <= 0 || state.scrollAfterDaysOfData < MIN_DAYS_FOR_AFTER_COMPARISON) {
                Text(
                    text = "Check back in a few days to see your progress.",
                    fontFamily = Inter,
                    fontSize = 13.sp,
                    color = GoalsTextMuted
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column {
                        Text("Before", fontFamily = Inter, fontSize = 12.sp, color = GoalsTextMuted)
                        Text(
                            text = "${formatMinutes(before)}/day",
                            fontFamily = Quicksand,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = GoalsTextPrimary
                        )
                    }
                    Text("→", fontSize = 20.sp, color = GoalsTextMuted)
                    Column {
                        Text("Now", fontFamily = Inter, fontSize = 12.sp, color = GoalsTextMuted)
                        Text(
                            text = "${formatMinutes(after)}/day",
                            fontFamily = Quicksand,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = if (after < before) GoalsMintDark else GoalsTextPrimary
                        )
                    }
                }
                val percentLess = (((before - after).toFloat() / before) * 100).roundToInt()
                Text(
                    text = if (after < before) "↓ $percentLess% less scrolling" else "Scroll time hasn't dropped yet — keep going.",
                    fontFamily = Inter,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.5.sp,
                    color = if (after < before) GoalsMintDark else GoalsTextMuted,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White)
                .padding(vertical = 18.dp, horizontal = 20.dp)
        ) {
            Text(
                text = "Your week, activity by activity",
                fontFamily = Quicksand,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = GoalsTextPrimary,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            WeeklyActivityCalendar(days = state.weeklyCalendar, goals = goals, dailyTargetMinutes = state.dailyTargetMinutes)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun WeeklySummaryScreenPreview() {
    val context = LocalContext.current
    BloomTheme {
        WeeklySummaryScreen(
            viewModel = remember { GoalsViewModelFactory(context.applicationContext).create(GoalsViewModel::class.java) },
            onBack = {}
        )
    }
}
