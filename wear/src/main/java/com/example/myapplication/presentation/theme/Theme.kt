package com.example.myapplication.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme

// --- Fitness & Wellness Theme Palette ---
// High-contrast neon accents against OLED deep blacks ensure the smartwatch display
// remains crystal clear even under bright outdoor sunlight during a run.

// Crimson red chosen specifically for heart rate metrics to match classic fitness watch displays
val HeartRateRed = Color(0xFFFF2D55)
val HeartRateContainer = Color(0x28FF2D55)

// Bright cyan accent for accelerometer and motion tracking components
val MotionCyan = Color(0xFF00E5FF)
val MotionCyanContainer = Color(0x2000E5FF)

// Distinct color coding for X, Y, and Z axes to make 3D motion directional reading effortless
val AxisXColor = Color(0xFFFF9500) // Amber for lateral X movement
val AxisYColor = Color(0xFF30D158) // Emerald green for vertical Y movement
val AxisZColor = Color(0xFF0A84FF) // Royal blue for depth Z movement

// Dark card surfaces tuned for circular smartwatch screens
val DarkCardBackground = Color(0xFF16181C)
val DarkCardBorder = Color(0xFF262930)

// Wear Compose Material 3 color palette configuration
private val FitnessColorScheme = ColorScheme(
    primary = HeartRateRed,
    primaryContainer = HeartRateContainer,
    secondary = MotionCyan,
    secondaryContainer = MotionCyanContainer,
    background = Color.Black,
    onBackground = Color.White,
    surfaceContainer = DarkCardBackground,
    onSurface = Color.White,
    onSurfaceVariant = Color(0xFF9EA3B0)
)

/**
 * Main theme wrapper for the Wear OS app.
 * Applies our high-visibility fitness color scheme across all Wear Compose Material 3 UI elements.
 */
@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = FitnessColorScheme,
        content = content
    )
}
