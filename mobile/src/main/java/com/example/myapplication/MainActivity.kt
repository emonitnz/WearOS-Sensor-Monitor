package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.myapplication.components.ConnectionHeader
import com.example.myapplication.components.MobileAccelerometerCard
import com.example.myapplication.components.MobileHeartRateCard
import com.example.myapplication.ui.theme.DarkBackground
import com.example.myapplication.ui.theme.MyApplicationTheme

/**
 * Main handheld activity displaying live fitness telemetry streamed from the paired Wear OS smartwatch.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = DarkBackground
                ) { innerPadding ->
                    DashboardScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

/**
 * Primary mobile scrollable dashboard.
 * Combines the smartwatch connection status header, the large heart rate card,
 * and the motion/accelerometer telemetry card into a clean layout.
 */
@Composable
fun DashboardScreen(modifier: Modifier = Modifier) {
    // Collect live telemetry StateFlows from SensorDataRepository
    val currentHr by SensorDataRepository.currentHeartRate.collectAsState()
    val hrHistory by SensorDataRepository.heartRateHistory.collectAsState()

    val accX by SensorDataRepository.accX.collectAsState()
    val accY by SensorDataRepository.accY.collectAsState()
    val accZ by SensorDataRepository.accZ.collectAsState()

    val currentMag by SensorDataRepository.currentMagnitude.collectAsState()
    val magHistory by SensorDataRepository.magnitudeHistory.collectAsState()

    val lastReceived by SensorDataRepository.lastReceivedTimestamp.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Smartwatch connection status header
        ConnectionHeader(
            lastReceivedTimestamp = lastReceived
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Heart Rate telemetry section
        MobileHeartRateCard(
            heartRate = currentHr,
            history = hrHistory
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Motion & Accelerometer telemetry section
        MobileAccelerometerCard(
            x = accX,
            y = accY,
            z = accZ,
            magnitude = currentMag,
            history = magHistory
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}
