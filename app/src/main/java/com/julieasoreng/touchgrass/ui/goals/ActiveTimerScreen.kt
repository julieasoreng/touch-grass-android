package com.julieasoreng.touchgrass.ui.goals

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.julieasoreng.touchgrass.data.preferences.OnboardingPreferencesRepository
import com.julieasoreng.touchgrass.ui.goals.components.FocusProgressRing
import com.julieasoreng.touchgrass.ui.theme.BloomTheme
import com.julieasoreng.touchgrass.ui.theme.GoalsDarkBackground
import com.julieasoreng.touchgrass.ui.theme.GoalsDarkBorder
import com.julieasoreng.touchgrass.ui.theme.GoalsDarkMintText
import com.julieasoreng.touchgrass.ui.theme.GoalsDarkTrack
import com.julieasoreng.touchgrass.ui.theme.GoalsMint
import com.julieasoreng.touchgrass.ui.theme.Quicksand

@Composable
fun ActiveTimerScreen(
    goalId: String,
    minutes: Int,
    viewModel: GoalsViewModel,
    onSessionEnded: () -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(goalId, minutes) {
        viewModel.startSession(goalId, minutes)
    }

    LaunchedEffect(Unit) {
        viewModel.sessionEnded.collect { onSessionEnded() }
    }

    val state by viewModel.uiState.collectAsState()
    val session = state.activeSession
    var showConfirmEnd by remember { mutableStateOf(false) }

    if (showConfirmEnd) {
        AlertDialog(
            onDismissRequest = { showConfirmEnd = false },
            title = { Text("End session early?") },
            text = { Text("You'll only get credit for the time you've focused so far, not the full duration.") },
            confirmButton = {
                TextButton(onClick = {
                    showConfirmEnd = false
                    viewModel.endSessionEarly()
                }) { Text("End session") }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmEnd = false }) { Text("Keep going") }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(GoalsDarkBackground)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (session == null) return@Column

        Text(
            text = "Focused on ${session.goal.name} ${session.goal.emoji}".uppercase(),
            fontFamily = Quicksand,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            color = GoalsDarkMintText,
            letterSpacing = 0.6.sp
        )

        Box(modifier = Modifier.padding(top = 24.dp, bottom = 24.dp)) {
            val progress = 1f - session.remainingSeconds.toFloat() / session.targetSeconds.toFloat()
            FocusProgressRing(
                progress = progress,
                ringColor = GoalsMint,
                trackColor = GoalsDarkTrack,
                modifier = Modifier.size(230.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = formatClock(session.remainingSeconds),
                        fontFamily = Quicksand,
                        fontWeight = FontWeight.Bold,
                        fontSize = 42.sp,
                        color = Color.White
                    )
                    Text(
                        text = "of ${formatClock(session.targetSeconds)}",
                        fontSize = 12.5.sp,
                        color = GoalsDarkMintText
                    )
                }
            }
        }

        Text(
            text = "Your phone stays locked to distracting apps until this ends.",
            fontSize = 13.sp,
            color = GoalsDarkMintText,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 220.dp)
        )

        Box(
            modifier = Modifier
                .padding(top = 24.dp)
                .clip(RoundedCornerShape(100.dp))
                .border(1.5.dp, GoalsDarkBorder, RoundedCornerShape(100.dp))
                .clickable { showConfirmEnd = true }
                .padding(vertical = 11.dp, horizontal = 20.dp)
        ) {
            Text(
                text = "End session early",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = GoalsDarkMintText
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ActiveTimerScreenPreview() {
    val context = LocalContext.current
    BloomTheme {
        val viewModel = remember {
            GoalsViewModel(OnboardingPreferencesRepository(context.applicationContext)).apply { startSession("read", 25) }
        }
        ActiveTimerScreen(
            goalId = "read",
            minutes = 25,
            viewModel = viewModel,
            onSessionEnded = {}
        )
    }
}
