package com.example.myapplication.components

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.theme.AxisXColor
import com.example.myapplication.ui.theme.AxisYColor
import com.example.myapplication.ui.theme.AxisZColor
import com.example.myapplication.ui.theme.DarkCardBackground
import com.example.myapplication.ui.theme.DarkCardBorder
import com.example.myapplication.ui.theme.DarkCardInner
import com.example.myapplication.ui.theme.MotionCyan
import kotlin.math.sqrt

/**
 * Mobile Accelerometer and Motion Telemetry Card.
 * Displays total 3D acceleration magnitude, activity intensity classification,
 * center-zero axis level meters (X, Y, Z), and a live magnitude history line graph.
 */
@Composable
fun MobileAccelerometerCard(
    x: Float,
    y: Float,
    z: Float,
    magnitude: Double,
    history: List<Double>,
    modifier: Modifier = Modifier
) {
    val (motionText, motionColor) = when {
        magnitude < 11.5 -> "REST" to Color(0xFF30D158)
        magnitude < 18.0 -> "MODERATE" to Color(0xFFFFD60A)
        else -> "INTENSE" to MotionCyan
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
            // Header Title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(MotionCyan)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "MOTION & ACCELEROMETER",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.sp
                    )
                }

                // Motion Intensity Tag
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(motionColor.copy(alpha = 0.18f))
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = motionText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = motionColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Magnitude Summary Container
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkCardInner)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Column {
                    Text(
                        text = "ACCELERATION MAGNITUDE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF9EA3B0)
                    )
                    Text(
                        text = "%.2f m/s²".format(magnitude),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }

                Text(
                    text = "10-Reading Avg",
                    fontSize = 11.sp,
                    color = MotionCyan.copy(alpha = 0.8f),
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Axis Level Bars (X, Y, Z)
            MobileAxisLevelBar(axisLabel = "X Axis", value = x, color = AxisXColor)
            Spacer(modifier = Modifier.height(8.dp))
            MobileAxisLevelBar(axisLabel = "Y Axis", value = y, color = AxisYColor)
            Spacer(modifier = Modifier.height(8.dp))
            MobileAxisLevelBar(axisLabel = "Z Axis", value = z, color = AxisZColor)

            Spacer(modifier = Modifier.height(18.dp))

            // Acceleration Magnitude History Line Chart
            if (history.size >= 2) {
                MobileMagnitudeChart(
                    history = history,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                )
            } else {
                Text(
                    text = "Awaiting accelerometer telemetry from watch...",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }
        }
    }
}

/**
 * Bi-directional level bar for individual accelerometer axis.
 * Maps values from -15 m/s² to +15 m/s² with 0 in the center.
 */
@Composable
private fun MobileAxisLevelBar(
    axisLabel: String,
    value: Float,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = axisLabel,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier.width(52.dp)
        )

        Canvas(
            modifier = Modifier
                .weight(1f)
                .height(14.dp)
                .padding(horizontal = 6.dp)
        ) {
            val w = size.width
            val h = size.height
            val centerX = w / 2f

            drawRoundRect(
                color = Color(0xFF23262D),
                size = size,
                cornerRadius = CornerRadius(h / 2f, h / 2f)
            )

            drawLine(
                color = Color(0xFF424752),
                start = Offset(centerX, 0f),
                end = Offset(centerX, h),
                strokeWidth = 1.5.dp.toPx()
            )

            val clampedVal = value.coerceIn(-15f, 15f)
            val fillFraction = clampedVal / 15f

            val fillWidth = (w / 2f) * kotlin.math.abs(fillFraction)
            val fillLeft = if (fillFraction >= 0) centerX else centerX - fillWidth

            if (fillWidth > 0f) {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(fillLeft, 1f),
                    size = Size(fillWidth, h - 2f),
                    cornerRadius = CornerRadius(h / 2f, h / 2f)
                )
            }
        }

        Text(
            text = "%+.2f".format(value),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.width(52.dp)
        )
    }
}

/**
 * Canvas chart displaying historical acceleration magnitude readings.
 */
@Composable
private fun MobileMagnitudeChart(
    history: List<Double>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        val minMag = (history.minOrNull() ?: 0.0).coerceAtMost(0.0)
        val maxMag = (history.maxOrNull() ?: 20.0).coerceAtLeast(15.0)
        val range = maxMag - minMag

        val points = history.mapIndexed { index, mag ->
            val x = (index.toFloat() / (history.size - 1).coerceAtLeast(1)) * width
            val normalizedY = ((mag - minMag) / range).toFloat()
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
                    MotionCyan.copy(alpha = 0.35f),
                    Color.Transparent
                )
            )
        )

        drawPath(
            path = strokePath,
            color = MotionCyan,
            style = Stroke(width = 3.dp.toPx())
        )

        points.lastOrNull()?.let { last ->
            drawCircle(
                color = Color.White,
                radius = 4.dp.toPx(),
                center = last
            )
            drawCircle(
                color = MotionCyan,
                radius = 2.5.dp.toPx(),
                center = last
            )
        }
    }
}
