package com.julieasoreng.touchgrass.ui.onboarding.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.julieasoreng.touchgrass.ui.theme.Lavender
import com.julieasoreng.touchgrass.ui.theme.LavenderMuted

@Composable
fun ProgressPills(
    currentStepIndex: Int,
    totalSteps: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        repeat(totalSteps) { index ->
            val filled = index <= currentStepIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(6.dp)
                    .background(
                        color = if (filled) Lavender else LavenderMuted,
                        shape = RoundedCornerShape(3.dp)
                    )
            )
        }
    }
}
