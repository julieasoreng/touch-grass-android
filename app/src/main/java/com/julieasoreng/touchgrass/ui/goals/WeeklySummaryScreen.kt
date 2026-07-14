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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.julieasoreng.touchgrass.ui.goals.components.DonutChart
import com.julieasoreng.touchgrass.ui.goals.components.DonutSlice
import com.julieasoreng.touchgrass.ui.goals.components.ScrollComparisonBar
import com.julieasoreng.touchgrass.ui.theme.BloomTheme
import com.julieasoreng.touchgrass.ui.theme.GoalsBackground
import com.julieasoreng.touchgrass.ui.theme.GoalsDustyRose
import com.julieasoreng.touchgrass.ui.theme.GoalsMint
import com.julieasoreng.touchgrass.ui.theme.GoalsMintDark
import com.julieasoreng.touchgrass.ui.theme.GoalsMintBannerText
import com.julieasoreng.touchgrass.ui.theme.GoalsTextMuted
import com.julieasoreng.touchgrass.ui.theme.GoalsTextPrimary
import com.julieasoreng.touchgrass.ui.theme.Inter
import com.julieasoreng.touchgrass.ui.theme.Quicksand
import kotlin.math.roundToInt

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
            val after = state.dailyScrollThisWeekMinutes
            val maxMinutes = before.coerceAtLeast(after).coerceAtLeast(1)
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ScrollComparisonBar(
                    label = "Before",
                    valueLabel = formatMinutes(before),
                    fraction = before.toFloat() / maxMinutes,
                    barColor = GoalsDustyRose
                )
                ScrollComparisonBar(
                    label = "This week",
                    valueLabel = formatMinutes(after),
                    fraction = after.toFloat() / maxMinutes,
                    barColor = GoalsMint
                )
            }
            val percentLess = if (before > 0) (((before - after).toFloat() / before) * 100).roundToInt() else 0
            Text(
                text = "↓ $percentLess% less scrolling",
                fontFamily = Inter,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.5.sp,
                color = GoalsMintDark,
                modifier = Modifier.padding(top = 12.dp)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(GoalsMint)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "🔥 ${state.focusStreakDays}-day focus streak",
                fontFamily = Quicksand,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = GoalsMintBannerText
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun WeeklySummaryScreenPreview() {
    BloomTheme {
        WeeklySummaryScreen(
            viewModel = remember { GoalsViewModel() },
            onBack = {}
        )
    }
}
