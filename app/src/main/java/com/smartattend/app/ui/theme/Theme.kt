package com.smartattend.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val SmartBlue = Color(0xFF1769E0)
val SmartNavy = Color(0xFF123563)
val SmartBackground = Color(0xFFF5F7FB)
val SmartWarning = Color(0xFFFFB020)
val SmartSuccess = Color(0xFF16835B)
val SmartDanger = Color(0xFFB3261E)

private val SmartAttendColors = lightColorScheme(
    primary = SmartBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD8E7FF),
    onPrimaryContainer = SmartNavy,
    secondary = SmartNavy,
    onSecondary = Color.White,
    background = SmartBackground,
    surface = Color.White,
    surfaceVariant = Color(0xFFE9EEF7),
    error = SmartDanger,
)

@Composable
fun SmartAttendTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = SmartAttendColors, content = content)
}

