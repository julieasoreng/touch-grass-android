package com.julieasoreng.touchgrass.ui.goals

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.julieasoreng.touchgrass.ui.theme.BloomTheme
import com.julieasoreng.touchgrass.ui.theme.GoalsBackground
import com.julieasoreng.touchgrass.ui.theme.GoalsPurple
import com.julieasoreng.touchgrass.ui.theme.GoalsTextMuted
import com.julieasoreng.touchgrass.ui.theme.GoalsTextPrimary
import com.julieasoreng.touchgrass.ui.theme.Quicksand

private val quickPickOptions = listOf(15, 25, 45, 60)
private const val STEP_MINUTES = 5
private const val MIN_MINUTES = 5
private const val MAX_MINUTES = 180
private const val DEFAULT_MINUTES = 25

@Composable
fun SetDurationScreen(
    goalId: String,
    viewModel: GoalsViewModel,
    onBack: () -> Unit,
    onStartFocusing: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val goal = state.goals.find { it.id == goalId }

    if (goal == null) {
        LaunchedEffect(Unit) { onBack() }
        return
    }

    var minutes by remember(goalId) { mutableIntStateOf(DEFAULT_MINUTES) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(GoalsBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp, start = 12.dp, end = 20.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "‹",
                fontSize = 22.sp,
                color = GoalsTextMuted,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(onClick = onBack)
                    .padding(8.dp)
            )
            Text(
                text = "My Goals",
                fontFamily = Quicksand,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = GoalsTextPrimary.copy(alpha = 0.85f)
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(goal.color),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(goal.icon.drawableRes),
                    contentDescription = null,
                    tint = GoalsTextPrimary,
                    modifier = Modifier.size(30.dp)
                )
            }
            Spacer(Modifier.height(28.dp))
            Text(
                text = "How long do you want\nto ${goal.name.lowercase()} for?",
                fontFamily = Quicksand,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = GoalsTextPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(28.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                StepperButton(symbol = "–", onClick = { minutes = (minutes - STEP_MINUTES).coerceAtLeast(MIN_MINUTES) })
                Text(
                    text = "$minutes min",
                    fontFamily = Quicksand,
                    fontWeight = FontWeight.Bold,
                    fontSize = 48.sp,
                    color = GoalsTextPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.widthIn(min = 140.dp)
                )
                StepperButton(symbol = "+", onClick = { minutes = (minutes + STEP_MINUTES).coerceAtMost(MAX_MINUTES) })
            }
            Spacer(Modifier.height(28.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                quickPickOptions.forEach { option ->
                    val selected = minutes == option
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(100.dp))
                            .background(if (selected) GoalsPurple else Color.White)
                            .then(
                                if (selected) Modifier else Modifier.border(1.5.dp, GoalsPurple.copy(alpha = 0.3f), RoundedCornerShape(100.dp))
                            )
                            .clickable { minutes = option }
                            .padding(vertical = 9.dp, horizontal = 16.dp)
                    ) {
                        Text(
                            text = if (option < 60) "${option}m" else "${option / 60}h",
                            fontFamily = Quicksand,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                            fontSize = 13.5.sp,
                            color = if (selected) Color.White else GoalsTextPrimary.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        Box(modifier = Modifier.padding(20.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(100.dp))
                    .background(GoalsPurple)
                    .clickable { onStartFocusing(minutes) }
                    .padding(vertical = 15.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Start focusing",
                    fontFamily = Quicksand,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun StepperButton(symbol: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Color.White)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text = symbol, fontSize = 20.sp, color = GoalsTextMuted, fontWeight = FontWeight.Bold)
    }
}

@Preview(showBackground = true)
@Composable
private fun SetDurationScreenPreview() {
    val context = LocalContext.current
    BloomTheme {
        SetDurationScreen(
            goalId = "read",
            viewModel = remember { GoalsViewModelFactory(context.applicationContext).create(GoalsViewModel::class.java) },
            onBack = {},
            onStartFocusing = {}
        )
    }
}
