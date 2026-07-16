package com.julieasoreng.touchgrass.ui.lock

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.julieasoreng.touchgrass.ui.theme.GoalsPurple
import com.julieasoreng.touchgrass.ui.theme.Inter
import com.julieasoreng.touchgrass.ui.theme.LockScreenBlack
import com.julieasoreng.touchgrass.ui.theme.LockScreenTextMuted
import com.julieasoreng.touchgrass.ui.theme.Quicksand

@Composable
fun PostUnlockScreen(
    viewModel: LockFeatureViewModel,
    onStartFocusSession: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(LockScreenBlack)
    ) {
        Text(
            text = "This is you right now.",
            fontFamily = Quicksand,
            fontWeight = FontWeight.Bold,
            fontSize = 26.sp,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 40.dp, start = 24.dp, end = 24.dp)
        )

        // The large black gap between the title and this footer is deliberate — the
        // phone's own glass reflects the person's face back at them there.
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 56.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = state.lastLockReasonText.ifBlank { "You hit your daily screen time limit." },
                fontFamily = Inter,
                fontSize = 14.sp,
                color = LockScreenTextMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(100.dp))
                    .background(GoalsPurple)
                    .clickable(onClick = onStartFocusSession)
                    .padding(vertical = 15.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Start a focus session instead",
                    fontFamily = Quicksand,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White
                )
            }

            Text(
                text = "Dismiss",
                fontFamily = Inter,
                fontSize = 13.sp,
                color = LockScreenTextMuted,
                modifier = Modifier
                    .padding(top = 16.dp)
                    .clickable(onClick = onDismiss)
            )
        }
    }
}
