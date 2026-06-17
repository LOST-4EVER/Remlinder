package com.remlinder.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = TimerAccent,
    onPrimary = Color.White,
    primaryContainer = TimerAccent.copy(alpha = 0.15f),
    onPrimaryContainer = TimerAccent,
    secondary = WormAccent,
    onSecondary = Color.Black,
    tertiary = GlowPink,
    surface = SurfaceDark,
    surfaceVariant = SurfaceDarkAlt,
    surfaceContainerHigh = SurfaceContainerDark,
    surfaceContainerHighest = SurfaceDarkElevated,
    onSurface = Color.White,
    onSurfaceVariant = Color.White.copy(alpha = 0.7f),
    background = SurfaceDark,
    onBackground = Color.White,
    error = AlarmRed,
    onError = Color.White,
    errorContainer = AlarmRed.copy(alpha = 0.15f),
    onErrorContainer = AlarmRed,
    outline = CardBorder,
    outlineVariant = CardBorder.copy(alpha = 0.5f)
)

private val LightColorScheme = lightColorScheme(
    primary = TimerAccent,
    onPrimary = Color.White,
    primaryContainer = TimerAccent.copy(alpha = 0.12f),
    onPrimaryContainer = TimerAccentDim,
    secondary = WormAccent,
    onSecondary = Color.Black,
    tertiary = GlowPink,
    surface = SurfaceLight,
    surfaceVariant = SurfaceLightAlt,
    surfaceContainerHigh = Color.White,
    surfaceContainerHighest = Color.White,
    onSurface = Color.Black,
    onSurfaceVariant = Color.Black.copy(alpha = 0.6f),
    background = SurfaceLight,
    onBackground = Color.Black,
    error = AlarmRed,
    onError = Color.White,
    errorContainer = AlarmRed.copy(alpha = 0.12f),
    onErrorContainer = AlarmRed,
    outline = Color(0xFFD0D0E0),
    outlineVariant = Color(0xFFE0E0EE)
)

@Composable
fun RemlinderTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.surface.toArgb()
            window.navigationBarColor = colorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
