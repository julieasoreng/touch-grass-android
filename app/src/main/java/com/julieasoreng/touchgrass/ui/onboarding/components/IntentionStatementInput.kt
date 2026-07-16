package com.julieasoreng.touchgrass.ui.onboarding.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.julieasoreng.touchgrass.ui.onboarding.MIN_INTENTION_STATEMENT_LENGTH
import com.julieasoreng.touchgrass.ui.theme.CharcoalText
import com.julieasoreng.touchgrass.ui.theme.Lavender
import com.julieasoreng.touchgrass.ui.theme.LavenderMuted

@Composable
fun IntentionStatementInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val trimmedLength = value.trim().length
    val showSoftError = trimmedLength in 1 until MIN_INTENTION_STATEMENT_LENGTH

    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("If I feel the urge to scroll, I will...") },
            minLines = 3,
            textStyle = MaterialTheme.typography.bodyMedium,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Lavender,
                unfocusedBorderColor = LavenderMuted,
                cursorColor = Lavender,
                focusedTextColor = CharcoalText,
                unfocusedTextColor = CharcoalText
            )
        )
        if (showSoftError) {
            Text(
                text = "Try writing the full sentence, it helps to be specific.",
                style = MaterialTheme.typography.bodySmall,
                color = CharcoalText.copy(alpha = 0.6f),
                modifier = Modifier.padding(top = 6.dp, start = 4.dp)
            )
        }
    }
}
