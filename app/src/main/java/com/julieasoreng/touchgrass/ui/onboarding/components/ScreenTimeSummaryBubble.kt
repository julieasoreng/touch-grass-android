package com.julieasoreng.touchgrass.ui.onboarding.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.julieasoreng.touchgrass.ui.onboarding.formatDuration
import com.julieasoreng.touchgrass.ui.theme.CharcoalText
import com.julieasoreng.touchgrass.ui.theme.Lavender
import com.julieasoreng.touchgrass.ui.theme.White

private val BubbleShape = RoundedCornerShape(16.dp)

@Composable
fun ScreenTimeSummaryBubble(
    isLoading: Boolean,
    dailyAverageMillis: Long,
    daysOfData: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .padding(start = 40.dp)
            .widthIn(max = 280.dp)
            .fillMaxWidth(),
        shape = BubbleShape,
        color = White,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            when {
                isLoading -> Text(
                    text = "Calculating your screen time…",
                    color = CharcoalText,
                    style = MaterialTheme.typography.bodyLarge
                )

                daysOfData <= 0 -> Text(
                    text = "We need at least one full day of usage data before we can show you this.",
                    color = CharcoalText,
                    style = MaterialTheme.typography.bodyLarge
                )

                else -> {
                    Text(
                        text = formatDuration(dailyAverageMillis),
                        color = Lavender,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = "Average daily social media screen time, based on the last " +
                            "$daysOfData ${if (daysOfData == 1) "day" else "days"}.",
                        color = CharcoalText,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (daysOfData in 1..2) {
                        Text(
                            text = "This will get more accurate as we collect more data.",
                            color = CharcoalText.copy(alpha = 0.6f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}
