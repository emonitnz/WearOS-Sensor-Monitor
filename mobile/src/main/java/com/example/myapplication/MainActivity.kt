package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    DashboardScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun DashboardScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        HeartRateDashboard()

        Spacer(modifier = Modifier.height(32.dp))
        HorizontalDivider(thickness = 1.dp, color = Color.LightGray)
        Spacer(modifier = Modifier.height(32.dp))

        AccelerometerDashboard()
    }
}

@Composable
fun HeartRateDashboard() {
    val currentHr by SensorDataRepository.currentHeartRate.collectAsState()
    val hrHistory by SensorDataRepository.heartRateHistory.collectAsState()

    Text(
        text = "Heart Rate",
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        color = Color.Red
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = "Current: ${currentHr.toInt()} BPM (5-reading avg)",
        fontSize = 18.sp,
        color = MaterialTheme.colorScheme.onSurface
    )

    Spacer(modifier = Modifier.height(16.dp))

    SensorChart(
        data = hrHistory,
        minVal = 40.0,
        maxVal = 200.0,
        color = Color.Red,
        yAxisLabel = "Heart Rate (BPM)",
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
    )
}

@Composable
fun AccelerometerDashboard() {
    val currentMag by SensorDataRepository.currentMagnitude.collectAsState()
    val magHistory by SensorDataRepository.magnitudeHistory.collectAsState()

    Text(
        text = "Acceleration Magnitude",
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF4CAF50) // Material Green
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = "Current: %.2f m/s² (10-reading avg)".format(currentMag),
        fontSize = 18.sp,
        color = MaterialTheme.colorScheme.onSurface
    )

    Spacer(modifier = Modifier.height(16.dp))

    SensorChart(
        data = magHistory,
        minVal = 0.0,
        maxVal = 30.0,
        color = Color(0xFF4CAF50),
        yAxisLabel = "Acceleration (m/s²)",
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
    )
}

@Composable
fun SensorChart(
    data: List<Double>,
    minVal: Double,
    maxVal: Double,
    color: Color,
    yAxisLabel: String,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(
        fontSize = 10.sp,
        color = Color.DarkGray
    )
    val titleStyle = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = Color.Black
    )

    val leftPadding = 60.dp
    val bottomPadding = 35.dp
    val topPadding = 10.dp
    val rightPadding = 10.dp

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        val leftPaddingPx = leftPadding.toPx()
        val bottomPaddingPx = bottomPadding.toPx()
        val topPaddingPx = topPadding.toPx()
        val rightPaddingPx = rightPadding.toPx()

        val chartWidth = width - leftPaddingPx - rightPaddingPx
        val chartHeight = height - bottomPaddingPx - topPaddingPx

        val gridColor = Color.LightGray
        val axisColor = Color.Black

        // 1. Draw Axes
        drawLine(
            color = axisColor,
            start = Offset(leftPaddingPx, topPaddingPx),
            end = Offset(leftPaddingPx, height - bottomPaddingPx),
            strokeWidth = 2f
        )
        drawLine(
            color = axisColor,
            start = Offset(leftPaddingPx, height - bottomPaddingPx),
            end = Offset(width - rightPaddingPx, height - bottomPaddingPx),
            strokeWidth = 2f
        )

        // 2. Draw Y-axis Labels and Ticks
        val yStepCount = 4
        for (i in 0..yStepCount) {
            val value = minVal + (maxVal - minVal) * i / yStepCount
            val yPos = (height - bottomPaddingPx) - (i.toFloat() / yStepCount * chartHeight)

            drawLine(
                color = axisColor,
                start = Offset(leftPaddingPx - 8f, yPos),
                end = Offset(leftPaddingPx, yPos),
                strokeWidth = 2f
            )

            drawLine(
                color = gridColor,
                start = Offset(leftPaddingPx, yPos),
                end = Offset(width - rightPaddingPx, yPos),
                strokeWidth = 1f
            )

            drawText(
                textMeasurer = textMeasurer,
                text = value.toInt().toString(),
                style = labelStyle,
                topLeft = Offset(leftPaddingPx - 35f, yPos - 15f)
            )
        }

        // Y-Axis Title
        val titleLayoutResult = textMeasurer.measure(yAxisLabel, titleStyle)
        val titleWidth = titleLayoutResult.size.width
        val titleHeight = titleLayoutResult.size.height

        drawContext.canvas.nativeCanvas.save()
        drawContext.canvas.nativeCanvas.rotate(-90f, 15f, height / 2)
        drawText(
            textMeasurer = textMeasurer,
            text = yAxisLabel,
            style = titleStyle,
            topLeft = Offset(15f - (titleWidth / 2f), (height / 2) - (titleHeight / 2f))
        )
        drawContext.canvas.nativeCanvas.restore()

        // 3. Draw X-axis Labels and Ticks
        if (data.isNotEmpty()) {
            val xStepCount = 5
            val dataStep = (data.size / xStepCount).coerceAtLeast(1)
            for (i in 0..xStepCount) {
                val index = (i * dataStep).coerceAtMost(data.size - 1)
                val xPos = leftPaddingPx + (index.toFloat() / (data.size - 1).coerceAtLeast(1)) * chartWidth

                drawLine(
                    color = axisColor,
                    start = Offset(xPos, height - bottomPaddingPx),
                    end = Offset(xPos, height - bottomPaddingPx + 8f),
                    strokeWidth = 2f
                )

                drawText(
                    textMeasurer = textMeasurer,
                    text = index.toString(),
                    style = labelStyle,
                    topLeft = Offset(xPos - 10f, height - bottomPaddingPx + 10f)
                )
            }

            val xTitle = "Reading Sequence"
            val xTitleLayout = textMeasurer.measure(xTitle, titleStyle)
            drawText(
                textMeasurer = textMeasurer,
                text = xTitle,
                style = titleStyle,
                topLeft = Offset(
                    leftPaddingPx + (chartWidth / 2) - (xTitleLayout.size.width / 2),
                    height - 20.dp.toPx()
                )
            )
        }

        // 4. Draw Data Path
        if (data.size >= 2) {
            val range = maxVal - minVal
            val xStep = chartWidth / (data.size - 1)

            val path = Path().apply {
                data.forEachIndexed { index, value ->
                    val normalizedY = ((value.coerceIn(minVal, maxVal) - minVal) / range).toFloat()
                    val x = leftPaddingPx + index * xStep
                    val y = (height - bottomPaddingPx) - (normalizedY * chartHeight)

                    if (index == 0) moveTo(x, y) else lineTo(x, y)
                }
            }

            drawPath(
                path = path,
                color = color,
                style = Stroke(width = 3.dp.toPx())
            )
        }
    }
}
