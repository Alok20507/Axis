package com.racelink.controller.core.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val RaceLinkColors = darkColorScheme(
    primary = Color(0xFFD6FF61),
    onPrimary = Color(0xFF182000),
    secondary = Color(0xFFB7C7D6),
    background = Color(0xFF090A0C),
    surface = Color(0xFF121417),
    surfaceVariant = Color(0xFF1B1E22),
    onBackground = Color(0xFFF2F4F6),
    onSurface = Color(0xFFF2F4F6),
    outline = Color(0xFF89929B),
    error = Color(0xFFFFB4AB),
)

@Composable
fun RaceLinkTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = RaceLinkColors, content = content)
}
