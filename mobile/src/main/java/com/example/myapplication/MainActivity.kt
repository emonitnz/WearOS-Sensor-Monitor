package com.example.myapplication

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable

/**
 * Mobile Activity that receives live sensor data (Accelerometer and Heart Rate)
 * streamed from the paired Wear OS smartwatch via the Wearable Message API.
 */
class MainActivity : ComponentActivity(), MessageClient.OnMessageReceivedListener {

    private var x by mutableFloatStateOf(0f)
    private var y by mutableFloatStateOf(0f)
    private var z by mutableFloatStateOf(0f)
    private var heartRate by mutableDoubleStateOf(0.0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MobileSensorScreen(
                        x = x,
                        y = y,
                        z = z,
                        heartRate = heartRate
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Register listener for incoming Wear OS messages
        Wearable.getMessageClient(this).addListener(this)
    }

    override fun onPause() {
        super.onPause()
        // Remove listener when app is paused
        Wearable.getMessageClient(this).removeListener(this)
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        when (messageEvent.path) {
            "/sensor/data" -> {
                val message = String(messageEvent.data)
                val parts = message.split(",")
                if (parts.size == 3) {
                    try {
                        x = parts[0].toFloat()
                        y = parts[1].toFloat()
                        z = parts[2].toFloat()
                        Log.d("MobileDataLayer", "Received accelerometer: X=$x, Y=$y, Z=$z")
                    } catch (e: Exception) {
                        Log.e("MobileDataLayer", "Error parsing accelerometer data", e)
                    }
                }
            }

            "/heart_rate" -> {
                val message = String(messageEvent.data)
                try {
                    heartRate = message.toDouble()
                    Log.d("MobileDataLayer", "Received heart rate: $heartRate BPM")
                } catch (e: Exception) {
                    Log.e("MobileDataLayer", "Error parsing heart rate data", e)
                }
            }
        }
    }
}

@Composable
fun MobileSensorScreen(x: Float, y: Float, z: Float, heartRate: Double) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Wear OS Sensor Receiver",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Heart Rate: ${heartRate.toInt()} BPM",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Accelerometer Data",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Acc X: %.2f".format(x), style = MaterialTheme.typography.bodyLarge)
        Text(text = "Acc Y: %.2f".format(y), style = MaterialTheme.typography.bodyLarge)
        Text(text = "Acc Z: %.2f".format(z), style = MaterialTheme.typography.bodyLarge)
    }
}
