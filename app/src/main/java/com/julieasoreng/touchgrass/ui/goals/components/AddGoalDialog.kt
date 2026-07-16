package com.julieasoreng.touchgrass.ui.goals.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.julieasoreng.touchgrass.data.goals.ACTIVITY_COLOR_OPTIONS
import com.julieasoreng.touchgrass.data.goals.ACTIVITY_ICON_OPTIONS
import com.julieasoreng.touchgrass.data.goals.ActivityIcon
import com.julieasoreng.touchgrass.ui.theme.GoalsPurple
import com.julieasoreng.touchgrass.ui.theme.GoalsTextMuted
import com.julieasoreng.touchgrass.ui.theme.GoalsTextPrimary
import com.julieasoreng.touchgrass.ui.theme.Quicksand

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddGoalDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, icon: ActivityIcon, color: Color) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedIcon by remember { mutableStateOf(ACTIVITY_ICON_OPTIONS.first()) }
    var selectedColor by remember { mutableStateOf(ACTIVITY_COLOR_OPTIONS.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add a new goal", fontFamily = Quicksand, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("e.g. Meditate") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Icon", fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = GoalsTextMuted)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ACTIVITY_ICON_OPTIONS.forEach { icon ->
                        val selected = icon == selectedIcon
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (selected) GoalsPurple.copy(alpha = 0.2f) else Color.Transparent)
                                .clickable { selectedIcon = icon },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(icon.drawableRes),
                                contentDescription = null,
                                tint = GoalsTextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
                Text("Color", fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = GoalsTextMuted)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ACTIVITY_COLOR_OPTIONS.forEach { color ->
                        val selected = color == selectedColor
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(color)
                                .then(
                                    if (selected) {
                                        Modifier.border(2.dp, GoalsPurple, CircleShape)
                                    } else {
                                        Modifier
                                    }
                                )
                                .clickable { selectedColor = color }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name, selectedIcon, selectedColor) }, enabled = name.isNotBlank()) {
                Text("Add", fontWeight = FontWeight.Bold, color = GoalsPurple)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
