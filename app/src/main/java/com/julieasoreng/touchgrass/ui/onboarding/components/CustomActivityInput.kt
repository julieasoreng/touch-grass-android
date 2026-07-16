package com.julieasoreng.touchgrass.ui.onboarding.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.julieasoreng.touchgrass.ui.theme.CharcoalText
import com.julieasoreng.touchgrass.ui.theme.LavenderMuted
import com.julieasoreng.touchgrass.ui.theme.SageGreen
import com.julieasoreng.touchgrass.ui.theme.White

fun Modifier.dashedBorder(
    color: Color,
    cornerRadius: Dp,
    strokeWidth: Dp = 1.5.dp
): Modifier = this.drawBehind {
    val stroke = Stroke(
        width = strokeWidth.toPx(),
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
    )
    drawRoundRect(
        color = color,
        style = stroke,
        cornerRadius = CornerRadius(cornerRadius.toPx())
    )
}

@Composable
fun CustomActivityInput(
    value: String,
    onValueChange: (String) -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .weight(1f)
                .dashedBorder(color = LavenderMuted, cornerRadius = 24.dp)
                .background(Color.Transparent, RoundedCornerShape(24.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp),
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = CharcoalText),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onConfirm() }),
            decorationBox = { innerTextField ->
                Box {
                    if (value.isEmpty()) {
                        Text(
                            text = "+ Something else... tell us what",
                            style = MaterialTheme.typography.bodyMedium,
                            color = CharcoalText.copy(alpha = 0.5f)
                        )
                    }
                    innerTextField()
                }
            }
        )
        // A visible confirm target, not just the keyboard's Done action — relying on Done alone
        // meant typed text could be silently lost if the user tapped "Continue" instead of it.
        if (value.isNotBlank()) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(SageGreen)
                    .clickable(onClick = onConfirm),
                contentAlignment = Alignment.Center
            ) {
                Text("✓", color = White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
