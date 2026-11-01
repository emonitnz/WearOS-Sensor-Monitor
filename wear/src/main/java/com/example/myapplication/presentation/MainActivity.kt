package com.example.myapplication.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.health.services.client.HealthServices
import androidx.health.services.client.MeasureCallback
import androidx.health.services.client.data.DataPointContainer
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.DataTypeAvailability
import androidx.health.services.client.data.DeltaDataType
import androidx.lifecycle.lifecycleScope
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices
import androidx.wear.compose.ui.tooling.preview.WearPreviewFontScales
import com.example.myapplication.presentation.theme.MyApplicationTheme
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.guava.await as awaitFuture
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await as awaitTask

/**
 * Main activity for the Wear OS app.
 *
 * This activity collects real-time accelerometer and heart rate sensor data,
 * displays it on the watch face, and streams the data to the connected phone
 * via the Wearable Data Layer API.
 *
 * It is optimized for active tracking (e.g. while running) by:
 * 1. Keeping the screen on during measurement sessions.
 * 2. Throttling update rates to balance smooth visuals with battery efficiency.
 * 3. Gracefully falling back between Health Services and standard SensorManager for heart rate tracking.
 */
class MainActivity : ComponentActivity(), SensorEventListener {

    // --- Sensor Management ---
    // Handles general hardware sensors (like the Accelerometer and fallback Heart Rate)
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var heartRateSensorFallback: Sensor? = null

    // Health Services MeasureClient (the primary, recommended API on Wear OS for Heart Rate)
    private val measureClient by lazy { HealthServices.getClient(this).measureClient }
    private var heartRateCallback: MeasureCallback? = null
    private var isHealthServicesActive = false

    // --- Wearable Data Layer Communication ---
    // Used to send sensor readings directly to paired devices (e.g., handheld phone)
    private val messageClient by lazy { Wearable.getMessageClient(this) }
    private val nodeClient by lazy { Wearable.getNodeClient(this) }

    // --- Throttling & Performance Control ---
    // We update UI and dispatch messages at most every 200ms (5 Hz).
    // This prevents flooding the Bluetooth queue and draining the battery during a run.
    private var lastUpdate: Long = 0
    private val updateIntervalMillis: Long = 200

    // --- Jetpack Compose State ---
    // Compose will automatically re-render the UI whenever these state values change.
    private var x by mutableFloatStateOf(0f)
    private var y by mutableFloatStateOf(0f)
    private var z by mutableFloatStateOf(0f)
    private var heartRate by mutableDoubleStateOf(0.0)

    // --- Runtime Permissions ---
    // Android 15+ (API 36+) introduced granular health permissions, while earlier versions use BODY_SENSORS.
    private val heartRatePermission = if (Build.VERSION.SDK_INT >= 36) {
        "android.permission.health.READ_HEART_RATE"
    } else {
        Manifest.permission.BODY_SENSORS
    }

    // Handles the permission prompt callback
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.d("Sensors", "Permission granted! Starting heart rate sensor...")
            startHeartRateMeasurement()
        } else {
            Log.w("Sensors", "Body sensor permission was denied by the user.")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen as required by modern Wear OS guidelines
        installSplashScreen()
        super.onCreate(savedInstanceState)

        // Prevent the watch screen from sleeping during active exercise monitoring
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Grab system service for hardware sensors
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        heartRateSensorFallback = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)

        setContent {
            WearApp(
                x = x,
                y = y,
                z = z,
                heartRate = heartRate
            )
        }
    }

    override fun onResume() {
        super.onResume()

        // Keep the screen on while actively monitoring sensors during runs
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Register accelerometer updates (normal delay provides a solid 20-50Hz stream, which we throttle to 5Hz)
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }

        // Check or request required body sensor permission before listening to heart rate
        if (ContextCompat.checkSelfPermission(this, heartRatePermission) == PackageManager.PERMISSION_GRANTED) {
            startHeartRateMeasurement()
        } else {
            requestPermissionLauncher.launch(heartRatePermission)
        }
    }

    override fun onPause() {
        super.onPause()

        // Unregister SensorManager listeners to preserve battery when activity is paused
        sensorManager.unregisterListener(this)

        // Unregister Health Services MeasureClient callback
        heartRateCallback?.let { callback ->
            if (isHealthServicesActive) {
                measureClient.unregisterMeasureCallbackAsync(DataType.HEART_RATE_BPM, callback)
                isHealthServicesActive = false
            }
        }

        // Clear screen flag so device can sleep normally when out of focus
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    /**
     * Starts monitoring heart rate.
     * We try Health Services first (recommended for Wear OS 3+).
     * If Health Services isn't supported (like in standard Android emulators),
     * we fall back to standard SensorManager (Sensor.TYPE_HEART_RATE).
     */
    private fun startHeartRateMeasurement() {
        lifecycleScope.launch {
            try {
                // Query capabilities to see if Health Services supports heart rate
                val capabilities = measureClient.getCapabilitiesAsync().awaitFuture()
                val supportsHeartRate = DataType.HEART_RATE_BPM in capabilities.supportedDataTypesMeasure

                if (supportsHeartRate) {
                    heartRateCallback = object : MeasureCallback {
                        override fun onDataReceived(data: DataPointContainer) {
                            val heartRateData = data.getData(DataType.HEART_RATE_BPM)
                            if (heartRateData.isNotEmpty()) {
                                heartRate = heartRateData.last().value
                                Log.d("HeartRate", "New Heart Rate via Health Services: ${heartRate.toInt()} BPM")
                                sendHeartRateData(heartRate)
                            }
                        }

                        override fun onAvailabilityChanged(
                            dataType: DeltaDataType<*, *>,
                            availability: androidx.health.services.client.data.Availability
                        ) {
                            if (availability is DataTypeAvailability) {
                                Log.d("HeartRate", "Health Services availability: $availability")
                            }
                        }
                    }

                    measureClient.registerMeasureCallback(DataType.HEART_RATE_BPM, heartRateCallback!!)
                    isHealthServicesActive = true
                    Log.d("HeartRate", "Health Services measure callback successfully registered.")
                } else {
                    Log.w("HeartRate", "Health Services heart rate not available, falling back to SensorManager...")
                    startFallbackHeartRateSensor()
                }
            } catch (e: Exception) {
                Log.e("HeartRate", "Health Services init failed or not available on device. Using SensorManager fallback.", e)
                startFallbackHeartRateSensor()
            }
        }
    }

    /**
     * Fallback method using traditional SensorManager for heart rate.
     * Useful on Wear OS emulators or older Wear hardware.
     */
    private fun startFallbackHeartRateSensor() {
        heartRateSensorFallback?.let { sensor ->
            val registered = sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
            Log.d("HeartRate", "SensorManager heart rate registered: $registered")
        } ?: Log.w("HeartRate", "No heart rate sensor hardware found on this device.")
    }

    /**
     * Called whenever a sensor reported by SensorManager produces new data.
     */
    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return

        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                val currentTime = System.currentTimeMillis()

                // Throttle updates to avoid overloading UI recomposition and Bluetooth link
                if (currentTime - lastUpdate >= updateIntervalMillis) {
                    x = event.values[0]
                    y = event.values[1]
                    z = event.values[2]

                    Log.d("Sensors", "Acc: X=%.2f, Y=%.2f, Z=%.2f".format(x, y, z))
                    sendSensorData(x, y, z)
                    lastUpdate = currentTime
                }
            }

            Sensor.TYPE_HEART_RATE -> {
                if (event.values.isNotEmpty()) {
                    val rawBpm = event.values[0].toDouble()
                    if (rawBpm > 0) {
                        heartRate = rawBpm
                        Log.d("Sensors", "Heart Rate via SensorManager: ${heartRate.toInt()} BPM")
                        sendHeartRateData(heartRate)
                    }
                }
            }
        }
    }

    /**
     * Sends accelerometer readings (X, Y, Z) to all connected Wearable nodes (such as the handheld phone).
     */
    private fun sendSensorData(x: Float, y: Float, z: Float) {
        val payload = "$x,$y,$z".toByteArray()

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val nodes = nodeClient.connectedNodes.awaitTask()
                for (node in nodes) {
                    messageClient.sendMessage(node.id, "/sensor/data", payload).awaitTask()
                    Log.d("DataLayer", "Sent accelerometer to node ${node.displayName}")
                }
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: Exception) {
                Log.e("DataLayer", "Failed to send accelerometer payload", e)
            }
        }
    }

    /**
     * Sends current heart rate (BPM) to all connected Wearable nodes.
     */
    private fun sendHeartRateData(heartRate: Double) {
        val payload = heartRate.toString().toByteArray()

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val nodes = nodeClient.connectedNodes.awaitTask()
                for (node in nodes) {
                    messageClient.sendMessage(node.id, "/heart_rate", payload).awaitTask()
                    Log.d("DataLayer", "Sent heart rate ($heartRate) to node ${node.displayName}")
                }
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: Exception) {
                Log.e("DataLayer", "Failed to send heart rate payload", e)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed for basic sampling
    }
}

/**
 * Main UI for the Wear OS application built with Wear Compose Material 3.
 */
@Composable
fun WearApp(x: Float, y: Float, z: Float, heartRate: Double) {
    MyApplicationTheme {
        AppScaffold {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Sensor Monitor",
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Heart Rate: ${heartRate.toInt()} BPM",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Acc X: %.2f".format(x),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Acc Y: %.2f".format(y),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Acc Z: %.2f".format(z),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@WearPreviewDevices
@WearPreviewFontScales
@Composable
fun DefaultPreview() {
    WearApp(x = 1.23f, y = 4.56f, z = 9.81f, heartRate = 72.0)
}
