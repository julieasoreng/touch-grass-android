package com.julieasoreng.touchgrass.ui.onboarding.components

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.julieasoreng.touchgrass.ui.onboarding.TargetReductionEstimate
import com.julieasoreng.touchgrass.ui.onboarding.estimateTargetReduction
import com.julieasoreng.touchgrass.ui.onboarding.formatDuration
import com.julieasoreng.touchgrass.ui.theme.CharcoalText
import com.julieasoreng.touchgrass.ui.theme.Lavender
import com.julieasoreng.touchgrass.ui.theme.LavenderMuted
import com.julieasoreng.touchgrass.ui.theme.White

private const val TAG = "CustomGoalInput"

@Composable
fun CustomGoalInput(
    value: String,
    onValueChange: (String) -> Unit,
    onConfirm: () -> Unit,
    baselineMillis: Long,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BasicTextField(
                value = value,
                onValueChange = { input -> onValueChange(input.filter { it.isDigit() }) },
                modifier = Modifier
                    .weight(1f)
                    .dashedBorder(color = LavenderMuted, cornerRadius = 24.dp)
                    .background(Color.Transparent, RoundedCornerShape(24.dp))
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = CharcoalText),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onConfirm() }),
                decorationBox = { innerTextField ->
                    Box {
                        if (value.isEmpty()) {
                            Text(
                                text = "Target minutes per day, e.g. 60",
                                style = MaterialTheme.typography.bodyMedium,
                                color = CharcoalText.copy(alpha = 0.5f)
                            )
                        }
                        innerTextField()
                    }
                }
            )
            // A visible confirm target, not just the keyboard's Done action — this step has no
            // "Continue" fallback button, so relying on Done alone could leave the user stuck
            // with no way to advance at all if it didn't fire.
            if (value.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Lavender)
                        .clickable(onClick = onConfirm),
                    contentAlignment = Alignment.Center
                ) {
                    Text("✓", color = White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        val inputMinutes = value.toIntOrNull()
        if (inputMinutes != null && inputMinutes > 0) {
            val statusText = when (val estimate = estimateTargetReduction(baselineMillis, inputMinutes * 60_000L)) {
                is TargetReductionEstimate.Reduction -> "down ${estimate.percent}%"
                TargetReductionEstimate.Increase -> "That's more than your current average"
                TargetReductionEstimate.BaselineUnavailable -> {
                    Log.w(TAG, "Baseline screen time is 0 — can't show a reduction percent for the custom goal")
                    "${formatDuration(inputMinutes * 60_000L)} per day"
                }
            }
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodyMedium,
                color = CharcoalText,
                modifier = Modifier.padding(top = 6.dp, start = 4.dp)
            )
        }
    }
}
