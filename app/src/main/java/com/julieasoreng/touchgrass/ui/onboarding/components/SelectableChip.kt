package com.julieasoreng.touchgrass.ui.onboarding.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.julieasoreng.touchgrass.ui.theme.CharcoalText
import com.julieasoreng.touchgrass.ui.theme.LavenderMuted
import com.julieasoreng.touchgrass.ui.theme.White

@Composable
fun SelectableChip(
    text: String,
    selected: Boolean,
    selectedColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (selected) selectedColor else White,
            contentColor = if (selected) White else CharcoalText
        ),
        border = BorderStroke(1.dp, if (selected) selectedColor else LavenderMuted),
        contentPadding = PaddingValues(vertical = 10.dp, horizontal = 16.dp)
    ) {
        Text(text = text, style = MaterialTheme.typography.bodyMedium)
    }
}
