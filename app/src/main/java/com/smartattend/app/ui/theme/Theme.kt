package com.smartattend.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.smartattend.app.core.model.ThemePreference

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

private val SmartAttendDarkColors = darkColorScheme(
    primary = Color(0xFF9FC3FF),
    onPrimary = Color(0xFF00325F),
    primaryContainer = Color(0xFF004881),
    secondary = Color(0xFFB9C7E6),
    background = Color(0xFF10131A),
    surface = Color(0xFF181C24),
)

@Composable
fun SmartAttendTheme(themePreference: ThemePreference = ThemePreference.SYSTEM, content: @Composable () -> Unit) {
    val systemDark = androidx.compose.foundation.isSystemInDarkTheme()
    val dark = themePreference == ThemePreference.DARK || (themePreference == ThemePreference.SYSTEM && systemDark)
    MaterialTheme(colorScheme = if (dark) SmartAttendDarkColors else SmartAttendColors, content = content)
}

