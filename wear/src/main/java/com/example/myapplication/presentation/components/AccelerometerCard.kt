package com.example.myapplication.presentation.components

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.example.myapplication.presentation.theme.AxisXColor
import com.example.myapplication.presentation.theme.AxisYColor
import com.example.myapplication.presentation.theme.AxisZColor
import com.example.myapplication.presentation.theme.DarkCardBackground
import com.example.myapplication.presentation.theme.DarkCardBorder
import com.example.myapplication.presentation.theme.MotionCyan
import kotlin.math.sqrt

/**
 * Modern graphical component for Accelerometer motion tracking on circular Wear OS display.
 * Displays overall motion magnitude along with bi-directional center-zero level bars for X, Y, and Z axes.
 */
@Composable
fun AccelerometerCard(
    x: Float,
    y: Float,
    z: Float,
    modifier: Modifier = Modifier
) {
    // Calculate 3D motion magnitude: sqrt(x² + y² + z²). Standard earth gravity at rest is ~9.81 m/s².
    val magnitude = sqrt((x * x) + (y * y) + (z * z))

    // Categorize motion intensity into user-friendly fitness labels
    val (motionText, motionColor) = when {
        magnitude < 11.5f -> "REST" to Color(0xFF30D158)
        magnitude < 18.0f -> "MODERATE" to Color(0xFFFFD60A)
        else -> "INTENSE" to MotionCyan
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
            // Section Title Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Motion cyan glowing indicator dot
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(MotionCyan)
                )

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = "MOTION & ACCEL",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Overall Magnitude Banner showing total acceleration
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF22252B))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Column {
                    Text(
                        text = "MAGNITUDE",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF9EA3B0)
                    )
                    Text(
                        text = "%.2f m/s²".format(magnitude),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }

                // Small activity intensity tag
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(motionColor.copy(alpha = 0.18f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = motionText,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = motionColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Bi-directional level bars for X, Y, and Z accelerometer axes
            AxisLevelBar(axisLabel = "X", value = x, color = AxisXColor)
            Spacer(modifier = Modifier.height(6.dp))
            AxisLevelBar(axisLabel = "Y", value = y, color = AxisYColor)
            Spacer(modifier = Modifier.height(6.dp))
            AxisLevelBar(axisLabel = "Z", value = z, color = AxisZColor)
        }
    }
}

/**
 * Custom Canvas meter that draws a bi-directional progress bar with 0 in the center.
 * Maps values from -15.0 m/s² to +15.0 m/s², filling left for negative readings and right for positive.
 */
@Composable
private fun AxisLevelBar(
    axisLabel: String,
    value: Float,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Single character axis identifier
        Text(
            text = axisLabel,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier.width(16.dp)
        )

        // Center-zero bi-directional level bar Canvas
        Canvas(
            modifier = Modifier
                .weight(1f)
                .height(12.dp)
                .padding(horizontal = 4.dp)
        ) {
            val w = size.width
            val h = size.height
            val centerX = w / 2f

            // Track background
            drawRoundRect(
                color = Color(0xFF23262D),
                size = size,
                cornerRadius = CornerRadius(h / 2f, h / 2f)
            )

            // Center zero line
            drawLine(
                color = Color(0xFF424752),
                start = Offset(centerX, 0f),
                end = Offset(centerX, h),
                strokeWidth = 1.5.dp.toPx()
            )

            // Clamp sensor values to ±15 m/s² for visual meter normalization
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

        // Formatted signed float string (e.g., +9.8 m/s²)
        Text(
            text = "%+.1f".format(value),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.width(36.dp)
        )
    }
}
