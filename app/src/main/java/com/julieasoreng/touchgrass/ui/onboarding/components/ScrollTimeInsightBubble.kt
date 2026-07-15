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
import com.julieasoreng.touchgrass.data.usage.ScrollTimePattern
import com.julieasoreng.touchgrass.ui.theme.CharcoalText
import com.julieasoreng.touchgrass.ui.theme.Lavender
import com.julieasoreng.touchgrass.ui.theme.White

private val BubbleShape = RoundedCornerShape(16.dp)

@Composable
fun ScrollTimeInsightBubble(
    isLoading: Boolean,
    dominantPattern: ScrollTimePattern?,
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
                    text = "Looking at your scroll patterns…",
                    color = CharcoalText,
                    style = MaterialTheme.typography.bodyLarge
                )

                daysOfData <= 0 -> Text(
                    text = "We haven't seen enough activity yet to spot a pattern.",
                    color = CharcoalText,
                    style = MaterialTheme.typography.bodyLarge
                )

                dominantPattern == null || dominantPattern == ScrollTimePattern.SPREAD_OUT -> Text(
                    text = "Your scrolling seems pretty spread out across the day.",
                    color = Lavender,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge
                )

                else -> Text(
                    text = "Looks like you scroll the most ${dominantPattern.phrase()}.",
                    color = Lavender,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

private fun ScrollTimePattern.phrase(): String = when (this) {
    ScrollTimePattern.MORNING -> "in the morning"
    ScrollTimePattern.MIDDAY -> "midday"
    ScrollTimePattern.AFTER_WORK -> "after work"
    ScrollTimePattern.BEFORE_BED -> "before bed"
    ScrollTimePattern.SPREAD_OUT -> "spread out across the day"
}
