package com.example.assignmentthree

import android.util.Log
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * A service that listens for messages from the Wear OS Data Layer.
 * It runs in the background even if the app UI is closed.
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

                    // 1. Calculate raw magnitude
                    val rawMagnitude = sqrt(x.pow(2) + y.pow(2) + z.pow(2)).toDouble()

                    // 2. Calculate 10-reading moving average
                    accBuffer.add(rawMagnitude)
                    if (accBuffer.size > 10) {
                        accBuffer.removeAt(0) //we remove the 11th buffer data and then count average
                    }
                    val averageMagnitude = accBuffer.average()

                    // 3. Log and update repository
                    Log.d("WearService", "Magnitude: %.2f m/s² (Avg 10: %.2f)".format(rawMagnitude, averageMagnitude))
                    SensorDataRepository.updateMagnitude(averageMagnitude)
                }
            }
            "/heart_rate" -> {
                // Decode the incoming heart-rate ByteArray as a String
                val payload = String(messageEvent.data)
                val rawHeartRate = payload.toDoubleOrNull() ?: 0.0

                // Calculate 5-reading moving average
                hrBuffer.add(rawHeartRate)
                if (hrBuffer.size > 5) {
                    hrBuffer.removeAt(0)
                }
                val averageHr = hrBuffer.average()

                // Log and update repository for the UI
                Log.d("WearService", "Received heart rate: $rawHeartRate BPM (Avg 5: %.1f)".format(averageHr))
                SensorDataRepository.updateHeartRate(averageHr)
            }
        }
    }
}
