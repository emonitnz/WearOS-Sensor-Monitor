package com.example.myapplication.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Dark color scheme tuned specifically for a modern health & fitness dashboard
private val FitnessDarkColorScheme = darkColorScheme(
    primary = HeartRateRed,
    primaryContainer = HeartRateContainer,
    secondary = MotionCyan,
    secondaryContainer = MotionCyanContainer,
    background = DarkBackground,
    surface = DarkCardBackground,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = Color(0xFF9EA3B0)
)

/**
 * Custom Material 3 theme wrapper for the mobile health monitor app.
 * Configures dark OLED background and vibrant fitness accent colors.
 */
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = FitnessDarkColorScheme,
        content = content
    )
}
