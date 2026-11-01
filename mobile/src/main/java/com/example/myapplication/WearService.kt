package com.example.myapplication

import android.util.Log
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * A background service that listens for messages from the Wear OS Data Layer.
 * It automatically processes incoming accelerometer and heart rate data,
 * calculates moving averages, and updates the shared repository.
 */
class WearService : WearableListenerService() {

    // Lists to store readings for moving averages
    private val hrBuffer = mutableListOf<Double>()
    private val accBuffer = mutableListOf<Double>()

    override fun onMessageReceived(messageEvent: MessageEvent) {
        super.onMessageReceived(messageEvent)

        when (messageEvent.path) {
            "/sensor/data" -> {
                val payload = String(messageEvent.data)
                Log.d("WearService", "Received accelerometer data: $payload")

                val values = payload.split(",")
                if (values.size == 3) {
                    val x = values[0].toFloatOrNull() ?: 0f
                    val y = values[1].toFloatOrNull() ?: 0f
                    val z = values[2].toFloatOrNull() ?: 0f

                    // 1. Calculate raw magnitude (sqrt(x^2 + y^2 + z^2))
                    val rawMagnitude = sqrt(x.pow(2) + y.pow(2) + z.pow(2)).toDouble()

                    // 2. Calculate 10-reading moving average
                    accBuffer.add(rawMagnitude)
                    if (accBuffer.size > 10) {
                        accBuffer.removeAt(0)
                    }
                    val averageMagnitude = accBuffer.average()

                    // 3. Update repository
                    Log.d("WearService", "Magnitude: %.2f m/s² (Avg 10: %.2f)".format(rawMagnitude, averageMagnitude))
                    SensorDataRepository.updateMagnitude(averageMagnitude)
                }
            }

            "/heart_rate" -> {
                val payload = String(messageEvent.data)
                val rawHeartRate = payload.toDoubleOrNull() ?: 0.0

                if (rawHeartRate > 0) {
                    // Calculate 5-reading moving average
                    hrBuffer.add(rawHeartRate)
                    if (hrBuffer.size > 5) {
                        hrBuffer.removeAt(0)
                    }
                    val averageHr = hrBuffer.average()

                    Log.d("WearService", "Received heart rate: $rawHeartRate BPM (Avg 5: %.1f)".format(averageHr))
                    SensorDataRepository.updateHeartRate(averageHr)
                }
            }
        }
    }
}
