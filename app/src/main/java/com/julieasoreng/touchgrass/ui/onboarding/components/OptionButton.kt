package com.julieasoreng.touchgrass.ui.onboarding.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.julieasoreng.touchgrass.ui.theme.CharcoalText
import com.julieasoreng.touchgrass.ui.theme.Lavender
import com.julieasoreng.touchgrass.ui.theme.LavenderMuted
import com.julieasoreng.touchgrass.ui.theme.White

@Composable
fun OptionButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (selected) Lavender else White,
            contentColor = if (selected) White else CharcoalText
        ),
        border = BorderStroke(1.dp, if (selected) Lavender else LavenderMuted),
        contentPadding = PaddingValues(vertical = 14.dp, horizontal = 20.dp)
    ) {
        Text(text = text, style = MaterialTheme.typography.bodyLarge)
    }
}
