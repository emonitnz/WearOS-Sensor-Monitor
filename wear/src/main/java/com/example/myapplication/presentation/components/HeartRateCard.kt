package com.example.myapplication.presentation.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.example.myapplication.presentation.theme.DarkCardBackground
import com.example.myapplication.presentation.theme.DarkCardBorder
import com.example.myapplication.presentation.theme.HeartRateRed

/**
 * Main Heart Rate card composable designed for round smartwatch screens.
 * Visualizes live BPM, dynamic training zone badge, a pulse animation synchronized to heart speed,
 * and a real-time sparkline trend graph showing recent heart rate samples.
 */
@Composable
fun HeartRateCard(
    heartRate: Double,
    history: List<Double>,
    modifier: Modifier = Modifier
) {
    // Dynamically calculate the heart pulse animation timing based on real BPM.
    // At higher heart rates, the pulse speeds up naturally to reflect cardiac activity.
    val transition = rememberInfiniteTransition(label = "HeartPulseTransition")
    val pulseScale by transition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (heartRate > 0) (60000 / heartRate.coerceIn(40.0, 200.0)).toInt() else 1000,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "HeartPulseScale"
    )

    // Automatically determine workout intensity zones based on standard heart rate bands
    val (zoneText, zoneColor) = when {
        heartRate <= 0.0 -> "MEASURING" to Color(0xFF9EA3B0)
        heartRate < 100.0 -> "RESTING" to Color(0xFF30D158)
        heartRate < 140.0 -> "FAT BURN" to Color(0xFFFFD60A)
        heartRate < 170.0 -> "CARDIO" to Color(0xFFFF9500)
        else -> "PEAK" to HeartRateRed
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(DarkCardBackground)
            .border(1.dp, DarkCardBorder, RoundedCornerShape(26.dp))
            .padding(14.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Header bar: Pulsing heart icon drawn on Canvas alongside section label
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Vector heart shape rendered directly on Canvas to guarantee smooth scaling without missing assets
                Canvas(
                    modifier = Modifier
                        .size(18.dp)
                        .scale(if (heartRate > 0) pulseScale else 1f)
                ) {
                    val w = size.width
                    val h = size.height
                    val path = Path().apply {
                        moveTo(w * 0.5f, h * 0.8f)
                        cubicTo(w * 0.1f, h * 0.55f, 0f, h * 0.35f, w * 0.25f, h * 0.15f)
                        cubicTo(w * 0.45f, 0f, w * 0.5f, h * 0.25f, w * 0.5f, h * 0.25f)
                        cubicTo(w * 0.5f, h * 0.25f, w * 0.55f, 0f, w * 0.75f, h * 0.15f)
                        cubicTo(w * 1.0f, h * 0.35f, w * 0.9f, h * 0.55f, w * 0.5f, h * 0.8f)
                        close()
                    }
                    drawPath(path = path, color = HeartRateRed)
                }

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = "HEART RATE",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Large, prominent BPM reading display with fallback string when waiting for initial sensor value
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (heartRate > 0) heartRate.toInt().toString() else "--",
                    fontSize = 38.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = HeartRateRed,
                    lineHeight = 38.sp
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "BPM",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = HeartRateRed.copy(alpha = 0.8f),
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }

            // Colored pill badge reflecting the current heart rate zone
            Box(
                modifier = Modifier
                    .padding(top = 2.dp, bottom = 8.dp)
                    .clip(CircleShape)
                    .background(zoneColor.copy(alpha = 0.18f))
                    .padding(horizontal = 10.dp, vertical = 2.dp)
            ) {
                Text(
                    text = zoneText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = zoneColor,
                    textAlign = TextAlign.Center
                )
            }

            // Render live sparkline chart once we have at least 2 real heart rate samples
            if (history.size >= 2) {
                HeartRateTrendChart(
                    history = history,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                )
            } else {
                Text(
                    text = "Collecting HR samples...",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }
        }
    }
}

/**
 * Custom Canvas chart that draws a smooth, cubic bezier line representing actual recent heart rate readings.
 * Includes a soft gradient shadow underneath the line for a polished Garmin/Fitbit look.
 */
@Composable
private fun HeartRateTrendChart(
    history: List<Double>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        // Calculate dynamic Y-axis bounds based on minimum and maximum BPM values in history
        val minBpm = (history.minOrNull() ?: 60.0).coerceAtMost(50.0)
        val maxBpm = (history.maxOrNull() ?: 120.0).coerceAtLeast(minBpm + 20.0)
        val range = maxBpm - minBpm

        // Map raw BPM data into canvas pixel coordinates
        val points = history.mapIndexed { index, bpm ->
            val x = (index.toFloat() / (history.size - 1).coerceAtLeast(1)) * width
            val normalizedY = ((bpm - minBpm) / range).toFloat()
            val y = height - (normalizedY * (height - 8f)) - 4f
            Offset(x, y)
        }

        // Generate smooth cubic bezier curves between data points
        val strokePath = Path().apply {
            points.forEachIndexed { i, pt ->
                if (i == 0) moveTo(pt.x, pt.y)
                else {
                    val prev = points[i - 1]
                    val controlX1 = prev.x + ((pt.x - prev.x) / 2f)
                    val controlX2 = prev.x + ((pt.x - prev.x) / 2f)
                    cubicTo(controlX1, prev.y, controlX2, pt.y, pt.x, pt.y)
                }
            }
        }

        // Close the path at the bottom to create a solid area for gradient filling
        val fillPath = Path().apply {
            addPath(strokePath)
            lineTo(width, height)
            lineTo(0f, height)
            close()
        }

        // Soft vertical gradient under the trend line
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    HeartRateRed.copy(alpha = 0.35f),
                    Color.Transparent
                )
            )
        )

        // Draw primary Crimson trend line
        drawPath(
            path = strokePath,
            color = HeartRateRed,
            style = Stroke(width = 2.5.dp.toPx())
        )

        // Highlight the newest data point with a white/crimson dot
        points.lastOrNull()?.let { last ->
            drawCircle(
                color = Color.White,
                radius = 3.dp.toPx(),
                center = last
            )
            drawCircle(
                color = HeartRateRed,
                radius = 1.8.dp.toPx(),
                center = last
            )
        }
    }
}
