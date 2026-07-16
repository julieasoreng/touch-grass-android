package com.julieasoreng.touchgrass.ui.onboarding.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.julieasoreng.touchgrass.ui.theme.CharcoalText
import com.julieasoreng.touchgrass.ui.theme.LavenderMuted

private val BubbleShape = RoundedCornerShape(16.dp)

/** Shows the implementation-intention template as an illustrative example, not editable text. */
@Composable
fun IntentionExampleBubble(activityPhrase: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier
            .widthIn(max = 280.dp)
            .fillMaxWidth(),
        shape = BubbleShape,
        color = LavenderMuted
    ) {
        Text(
            text = "If I feel the urge to scroll, I will $activityPhrase instead. " +
                "I will tell [name of friend] about this.",
            color = CharcoalText,
            fontStyle = FontStyle.Italic,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )
    }
}
