package com.julieasoreng.touchgrass.data.goals

import androidx.compose.ui.graphics.Color

data class Goal(
    val id: String,
    val name: String,
    val icon: ActivityIcon,
    val color: Color,
    val weeklyMinutes: Int
)
