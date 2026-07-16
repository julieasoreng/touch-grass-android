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
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.julieasoreng.touchgrass.data.preferences.OnboardingPreferencesRepository
import com.julieasoreng.touchgrass.ui.goals.components.AddGoalDialog
import com.julieasoreng.touchgrass.ui.goals.components.GoalCard
import com.julieasoreng.touchgrass.ui.goals.components.dashedBorder
import com.julieasoreng.touchgrass.ui.theme.BloomTheme
import com.julieasoreng.touchgrass.ui.theme.GoalsBackground
import com.julieasoreng.touchgrass.ui.theme.GoalsMint
import com.julieasoreng.touchgrass.ui.theme.GoalsMintBannerText
import com.julieasoreng.touchgrass.ui.theme.GoalsPurple
import com.julieasoreng.touchgrass.ui.theme.GoalsTextMuted
import com.julieasoreng.touchgrass.ui.theme.GoalsTextPrimary
import com.julieasoreng.touchgrass.ui.theme.Inter
import com.julieasoreng.touchgrass.ui.theme.Quicksand

@Composable
fun MyGoalsScreen(
    viewModel: GoalsViewModel,
    onGoalSelected: (String) -> Unit,
    onViewSummary: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    var showAddGoalDialog by remember { mutableStateOf(false) }

    if (showAddGoalDialog) {
        AddGoalDialog(
            onDismiss = { showAddGoalDialog = false },
            onConfirm = { name, emoji ->
                viewModel.addGoal(name, emoji)
                showAddGoalDialog = false
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(GoalsBackground)
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "My Goals",
                fontFamily = Quicksand,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = GoalsTextPrimary
            )
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(GoalsPurple)
                    .clickable { showAddGoalDialog = true },
                contentAlignment = Alignment.Center
            ) {
                Text("+", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }

        Text(
            text = "Tap a goal to start focusing on it.",
            fontFamily = Inter,
            fontSize = 14.sp,
            color = GoalsTextMuted
        )

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (state.goals.isEmpty()) {
                Text(
                    text = "No goals yet — add one below to get started.",
                    fontFamily = Inter,
                    fontSize = 14.sp,
                    color = GoalsTextMuted
                )
            }

            state.goals.forEach { goal ->
                key(goal.id) {
                    GoalCard(
                        goal = goal,
                        onClick = { onGoalSelected(goal.id) },
                        onRemove = { viewModel.removeGoal(goal.id) }
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .dashedBorder(color = GoalsTextMuted.copy(alpha = 0.4f), cornerRadius = 18.dp, strokeWidth = 1.5.dp)
                    .clickable { showAddGoalDialog = true }
                    .padding(vertical = 16.dp, horizontal = 18.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+ Add a new goal",
                    fontFamily = Quicksand,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = GoalsTextMuted
                )
            }
        }

        Text(
            text = "Swipe a goal left to remove it",
            fontFamily = Inter,
            fontSize = 12.sp,
            color = GoalsTextMuted
        )

        val totalReplacedMinutes = state.goals.sumOf { it.weeklyMinutes }
        val bannerText = remember(totalReplacedMinutes) {
            buildAnnotatedString {
                append("You've replaced ")
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(formatMinutes(totalReplacedMinutes))
                }
                append(" of scrolling this week. See your ")
                pushStringAnnotation(tag = "summary", annotation = "summary")
                withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) {
                    append("full summary")
                }
                pop()
                append(".")
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(GoalsMint)
                .padding(vertical = 14.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("🌿", fontSize = 20.sp)
            ClickableText(
                text = bannerText,
                style = TextStyle(fontFamily = Inter, fontSize = 13.sp, color = GoalsMintBannerText, lineHeight = 18.sp),
                onClick = { offset ->
                    bannerText.getStringAnnotations("summary", offset, offset).firstOrNull()?.let {
                        onViewSummary()
                    }
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MyGoalsScreenPreview() {
    val context = LocalContext.current
    BloomTheme {
        MyGoalsScreen(
            viewModel = remember { GoalsViewModel(OnboardingPreferencesRepository(context.applicationContext)) },
            onGoalSelected = {},
            onViewSummary = {}
        )
    }
}
