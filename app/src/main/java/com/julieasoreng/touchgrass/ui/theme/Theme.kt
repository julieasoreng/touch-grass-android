package com.julieasoreng.touchgrass.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val BloomColorScheme = lightColorScheme(
    primary = Lavender,
    onPrimary = White,
    secondary = SageGreen,
    onSecondary = White,
    background = CreamBackground,
    onBackground = CharcoalText,
    surface = White,
    onSurface = CharcoalText,
    surfaceVariant = LavenderMuted,
    onSurfaceVariant = CharcoalText
)

@Composable
fun BloomTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = BloomColorScheme,
        typography = BloomTypography,
        content = content
    )
}
