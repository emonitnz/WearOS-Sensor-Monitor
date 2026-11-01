package com.example.myapplication.components

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.example.myapplication.ui.theme.DarkCardBackground
import com.example.myapplication.ui.theme.DarkCardBorder
import com.example.myapplication.ui.theme.HeartRateRed

/**
 * Mobile dashboard Heart Rate card.
 * Visualizes live BPM from Wear OS smartwatch, dynamic training zone, pulsing heart icon,
 * and a smooth bezier trend line chart matching watch app styling.
 */
@Composable
fun MobileHeartRateCard(
    heartRate: Double,
    history: List<Double>,
    modifier: Modifier = Modifier
) {
    // Pulse animation timed to live heart rate
    val transition = rememberInfiniteTransition(label = "MobileHeartPulse")
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
        label = "PulseScale"
    )

    // Training zone indicator
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
            .clip(RoundedCornerShape(24.dp))
            .background(DarkCardBackground)
            .border(1.dp, DarkCardBorder, RoundedCornerShape(24.dp))
            .padding(18.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Card Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Canvas(
                        modifier = Modifier
                            .size(20.dp)
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

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "HEART RATE",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.sp
                    )
                }

                // Zone Badge
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(zoneColor.copy(alpha = 0.18f))
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = zoneText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = zoneColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Large BPM Display
            Row(
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = if (heartRate > 0) heartRate.toInt().toString() else "--",
                    fontSize = 48.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = HeartRateRed,
                    lineHeight = 48.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "BPM",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = HeartRateRed.copy(alpha = 0.8f),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Live Trend Sparkline Chart
            if (history.size >= 2) {
                MobileHeartRateChart(
                    history = history,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                )
            } else {
                Text(
                    text = "Awaiting heart rate telemetry from watch...",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            }
        }
    }
}

/**
 * Mobile Canvas chart drawing smooth bezier curve for recent heart rate samples.
 */
@Composable
private fun MobileHeartRateChart(
    history: List<Double>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        val minBpm = (history.minOrNull() ?: 60.0).coerceAtMost(50.0)
        val maxBpm = (history.maxOrNull() ?: 120.0).coerceAtLeast(minBpm + 20.0)
        val range = maxBpm - minBpm

        val points = history.mapIndexed { index, bpm ->
            val x = (index.toFloat() / (history.size - 1).coerceAtLeast(1)) * width
            val normalizedY = ((bpm - minBpm) / range).toFloat()
            val y = height - (normalizedY * (height - 12f)) - 6f
            Offset(x, y)
        }

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

        val fillPath = Path().apply {
            addPath(strokePath)
            lineTo(width, height)
            lineTo(0f, height)
            close()
        }

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    HeartRateRed.copy(alpha = 0.35f),
                    Color.Transparent
                )
            )
        )

        drawPath(
            path = strokePath,
            color = HeartRateRed,
            style = Stroke(width = 3.dp.toPx())
        )

        points.lastOrNull()?.let { last ->
            drawCircle(
                color = Color.White,
                radius = 4.dp.toPx(),
                center = last
            )
            drawCircle(
                color = HeartRateRed,
                radius = 2.5.dp.toPx(),
                center = last
            )
        }
    }
}
