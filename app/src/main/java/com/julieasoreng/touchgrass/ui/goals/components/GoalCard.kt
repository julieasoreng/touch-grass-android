package com.julieasoreng.touchgrass.ui.goals.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.julieasoreng.touchgrass.data.goals.Goal
import com.julieasoreng.touchgrass.ui.goals.formatMinutes
import com.julieasoreng.touchgrass.ui.theme.GoalsChevron
import com.julieasoreng.touchgrass.ui.theme.GoalsDeleteBackground
import com.julieasoreng.touchgrass.ui.theme.GoalsTextMuted
import com.julieasoreng.touchgrass.ui.theme.GoalsTextPrimary
import com.julieasoreng.touchgrass.ui.theme.Inter
import com.julieasoreng.touchgrass.ui.theme.Quicksand

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalCard(
    goal: Goal,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) onRemove()
            true
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        // Clipping the box as a whole (rather than relying on the background/content each
        // clipping themselves) prevents a stray sliver of the delete-reveal background from
        // showing past the rounded corners when the swipe offset isn't settled at exactly zero.
        modifier = modifier.clip(RoundedCornerShape(18.dp)),
        enableDismissFromStartToEnd = false,
        backgroundContent = { GoalCardDeleteBackground() }
    ) {
        GoalCardContent(goal = goal, onClick = onClick)
    }
}

@Composable
private fun GoalCardDeleteBackground() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(GoalsDeleteBackground)
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Remove", color = Color.White, fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}

@Composable
private fun GoalCardContent(goal: Goal, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White)
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp, horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(goal.color),
            contentAlignment = Alignment.Center
        ) {
            Text(goal.emoji, fontSize = 20.sp)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(goal.name, fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = GoalsTextPrimary)
            Text(
                text = "${formatMinutes(goal.weeklyMinutes)} this week",
                fontFamily = Inter,
                fontSize = 12.5.sp,
                color = GoalsTextMuted
            )
        }
        Text("›", fontSize = 18.sp, color = GoalsChevron)
    }
}
