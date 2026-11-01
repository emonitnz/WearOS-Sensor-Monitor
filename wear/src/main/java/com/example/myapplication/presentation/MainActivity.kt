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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.health.services.client.HealthServices
import androidx.health.services.client.MeasureCallback
import androidx.health.services.client.data.DataPointContainer
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.DataTypeAvailability
import androidx.health.services.client.data.DeltaDataType
import androidx.lifecycle.lifecycleScope
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices
import androidx.wear.compose.ui.tooling.preview.WearPreviewFontScales
import com.example.myapplication.presentation.components.AccelerometerCard
import com.example.myapplication.presentation.components.HeartRateCard
import com.example.myapplication.presentation.theme.MyApplicationTheme
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.guava.await as awaitFuture
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await as awaitTask

/**
 * Primary activity for our Wear OS smartwatch application.
 *
 * This class handles hardware sensor lifecycle management (accelerometer and heart rate),
 * manages state recomposition, updates our circular Compose dashboard, and streams sensor
 * telemetry to handheld phone devices over the Wearable Data Layer API.
 */
class MainActivity : ComponentActivity(), SensorEventListener {

    // Sensor instances for hardware acceleration and fallback heart rate
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var heartRateSensorFallback: Sensor? = null

    // Health Services MeasureClient is Google's recommended high-precision heart rate API for Wear OS 3+
    private val measureClient by lazy { HealthServices.getClient(this).measureClient }
    private var heartRateCallback: MeasureCallback? = null
    private var isHealthServicesActive = false

    // Wearable Data Layer clients for transmitting readings to paired phones
    private val messageClient by lazy { Wearable.getMessageClient(this) }
    private val nodeClient by lazy { Wearable.getNodeClient(this) }

    // Throttle sensor dispatching to 200ms (5 Hz) to maintain UI smoothness and conserve watch battery during runs
    private var lastUpdate: Long = 0
    private val updateIntervalMillis: Long = 200

    // Live Compose state variables holding accelerometer readings and heart rate
    private var x by mutableFloatStateOf(0f)
    private var y by mutableFloatStateOf(0f)
    private var z by mutableFloatStateOf(0f)
    private var heartRate by mutableDoubleStateOf(0.0)

    // In-memory sample buffer keeping the last 30 heart rate points for the live trend graph
    private val heartRateHistory = mutableStateListOf<Double>()

    // Use granular Health Permissions for Android 15+ (API 36) and standard BODY_SENSORS for earlier Android versions
    private val heartRatePermission = if (Build.VERSION.SDK_INT >= 36) {
        "android.permission.health.READ_HEART_RATE"
    } else {
        Manifest.permission.BODY_SENSORS
    }

    // Handles user interaction when asking for body sensor permissions
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.d("Sensors", "Permission granted! Initializing heart rate tracking...")
            startHeartRateMeasurement()
        } else {
            Log.w("Sensors", "Body sensor permission denied by user.")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        // Keep watch screen lit during active workout sessions so sensors don't go to sleep mid-run
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Obtain System SensorManager and register hardware sensors
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        heartRateSensorFallback = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)

        setContent {
            WearApp(
                x = x,
                y = y,
                z = z,
                heartRate = heartRate,
                heartRateHistory = heartRateHistory
            )
        }
    }

    override fun onResume() {
        super.onResume()

        // Ensure screen remains awake while viewing live tracking
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Listen for accelerometer motion updates
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }

        // Check required runtime permissions before activating heart rate sensors
        if (ContextCompat.checkSelfPermission(this, heartRatePermission) == PackageManager.PERMISSION_GRANTED) {
            startHeartRateMeasurement()
        } else {
            requestPermissionLauncher.launch(heartRatePermission)
        }
    }

    override fun onPause() {
        super.onPause()

        // Unregister accelerometer sensor listener to prevent battery drain in background
        sensorManager.unregisterListener(this)

        // Safely unregister Health Services heart rate measure callback
        heartRateCallback?.let { callback ->
            if (isHealthServicesActive) {
                measureClient.unregisterMeasureCallbackAsync(DataType.HEART_RATE_BPM, callback)
                isHealthServicesActive = false
            }
        }

        // Allow watch screen to turn off normally when app is no longer in focus
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    // Appends new heart rate readings into our history buffer for sparkline rendering, trimming to max 30 points
    private fun addHeartRateSample(sample: Double) {
        if (sample > 0) {
            heartRateHistory.add(sample)
            if (heartRateHistory.size > 30) {
                heartRateHistory.removeAt(0)
            }
        }
    }

    /**
     * Initializes heart rate monitoring. First attempts Health Services MeasureClient.
     * If Health Services is unavailable (such as on Android Wear OS emulators), seamlessly
     * falls back to standard SensorManager.
     */
    private fun startHeartRateMeasurement() {
        lifecycleScope.launch {
            try {
                val capabilities = measureClient.getCapabilitiesAsync().awaitFuture()
                val supportsHeartRate = DataType.HEART_RATE_BPM in capabilities.supportedDataTypesMeasure

                if (supportsHeartRate) {
                    heartRateCallback = object : MeasureCallback {
                        override fun onDataReceived(data: DataPointContainer) {
                            val heartRateData = data.getData(DataType.HEART_RATE_BPM)
                            if (heartRateData.isNotEmpty()) {
                                heartRate = heartRateData.last().value
                                addHeartRateSample(heartRate)
                                Log.d("HeartRate", "Health Services HR update: ${heartRate.toInt()} BPM")
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
                    Log.d("HeartRate", "Health Services heart rate registered successfully.")
                } else {
                    Log.w("HeartRate", "Health Services heart rate not available. Switching to SensorManager fallback.")
                    startFallbackHeartRateSensor()
                }
            } catch (e: Exception) {
                Log.e("HeartRate", "Health Services init exception. Reverting to SensorManager fallback.", e)
                startFallbackHeartRateSensor()
            }
        }
    }

    // Fallback registration using traditional SensorManager for heart rate on hardware/emulators without Health Services
    private fun startFallbackHeartRateSensor() {
        heartRateSensorFallback?.let { sensor ->
            val registered = sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
            Log.d("HeartRate", "SensorManager heart rate registered: $registered")
        } ?: Log.w("HeartRate", "No hardware heart rate sensor found on device.")
    }

    // Triggered whenever standard hardware sensors (Accelerometer or fallback Heart Rate) report new data
    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return

        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                val currentTime = System.currentTimeMillis()

                // Throttle UI updates and Bluetooth message dispatches to 200ms intervals
                if ((currentTime - lastUpdate) >= updateIntervalMillis) {
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
                        addHeartRateSample(rawBpm)
                        Log.d("Sensors", "SensorManager heart rate update: ${heartRate.toInt()} BPM")
                        sendHeartRateData(heartRate)
                    }
                }
            }
        }
    }

    // Transmits 3D accelerometer payload over Wearable Message API to connected handheld devices
    private fun sendSensorData(x: Float, y: Float, z: Float) {
        val payload = "$x,$y,$z".toByteArray()

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val nodes = nodeClient.connectedNodes.awaitTask()
                for (node in nodes) {
                    messageClient.sendMessage(node.id, "/sensor/data", payload).awaitTask()
                    Log.d("DataLayer", "Sent accelerometer payload to node ${node.displayName}")
                }
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: Exception) {
                Log.e("DataLayer", "Failed to dispatch accelerometer payload", e)
            }
        }
    }

    // Transmits current heart rate reading over Wearable Message API to connected handheld devices
    private fun sendHeartRateData(heartRate: Double) {
        val payload = heartRate.toString().toByteArray()

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val nodes = nodeClient.connectedNodes.awaitTask()
                for (node in nodes) {
                    messageClient.sendMessage(node.id, "/heart_rate", payload).awaitTask()
                    Log.d("DataLayer", "Sent heart rate payload ($heartRate BPM) to node ${node.displayName}")
                }
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: Exception) {
                Log.e("DataLayer", "Failed to dispatch heart rate payload", e)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not used for standard sampling
    }
}

/**
 * Main circular Wear OS Compose UI layout.
 * Uses TransformingLazyColumn and ScreenScaffold to curve content naturally along round display bezels.
 */
@Composable
fun WearApp(
    x: Float,
    y: Float,
    z: Float,
    heartRate: Double,
    heartRateHistory: List<Double>
) {
    MyApplicationTheme {
        AppScaffold {
            val listState = rememberTransformingLazyColumnState()
            val transformationSpec = rememberTransformationSpec()

            ScreenScaffold(
                scrollState = listState
            ) { contentPadding ->
                TransformingLazyColumn(
                    contentPadding = contentPadding,
                    state = listState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Header title bar at the top of the circular screen
                    item {
                        ListHeader(
                            modifier = Modifier
                                .fillMaxWidth()
                                .transformedHeight(this, transformationSpec),
                            transformation = SurfaceTransformation(transformationSpec)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF30D158))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "FITNESS MONITOR",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                    }

                    // Section 1: Heart Rate Card with live BPM, pulse animation, and trend chart
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .transformedHeight(this, transformationSpec),
                            contentAlignment = Alignment.Center
                        ) {
                            HeartRateCard(
                                heartRate = heartRate,
                                history = heartRateHistory
                            )
                        }
                    }

                    // Section 2: Motion & Accelerometer Card with bi-directional axis meters
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .transformedHeight(this, transformationSpec),
                            contentAlignment = Alignment.Center
                        ) {
                            AccelerometerCard(
                                x = x,
                                y = y,
                                z = z
                            )
                        }
                    }

                    // Footer item: Streaming connectivity status pill at the bottom of the scroll list
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp, bottom = 16.dp)
                                .transformedHeight(this, transformationSpec),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "• STREAMING TO MOBILE",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF00E5FF).copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@WearPreviewDevices
@WearPreviewFontScales
@Composable
fun DefaultPreview() {
    WearApp(
        x = 1.23f,
        y = 4.56f,
        z = 9.81f,
        heartRate = 78.0,
        heartRateHistory = listOf(72.0, 75.0, 74.0, 78.0, 80.0, 77.0, 78.0)
    )
}
