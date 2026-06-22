package com.checkin.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CheckInColorScheme = darkColorScheme(
    primary = PrimaryGreen,
    onPrimary = Color.White,
    primaryContainer = PrimaryGreenVariant,
    onPrimaryContainer = Color.White,
    secondary = PrimaryGreenVariant,
    onSecondary = Color.White,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    error = RedEmergency,
    onError = Color.White,
    outline = BorderColor,
    outlineVariant = BorderColor
)

@Composable
fun CheckInTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CheckInColorScheme,
        typography = CheckInTypography,
        content = content
    )
}
