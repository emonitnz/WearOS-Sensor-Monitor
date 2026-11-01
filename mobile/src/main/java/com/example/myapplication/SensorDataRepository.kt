package com.example.myapplication

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Singleton repository sharing live smartwatch telemetry between the background
 * WearService and the mobile Compose Dashboard UI.
 */
object SensorDataRepository {

    // --- Heart Rate Telemetry ---
    private val _currentHeartRate = MutableStateFlow(0.0)
    val currentHeartRate: StateFlow<Double> = _currentHeartRate.asStateFlow()

    private val _heartRateHistory = MutableStateFlow<List<Double>>(emptyList())
    val heartRateHistory: StateFlow<List<Double>> = _heartRateHistory.asStateFlow()

    // --- Accelerometer Raw Axes & Magnitude Telemetry ---
    private val _accX = MutableStateFlow(0f)
    val accX: StateFlow<Float> = _accX.asStateFlow()

    private val _accY = MutableStateFlow(0f)
    val accY: StateFlow<Float> = _accY.asStateFlow()

    private val _accZ = MutableStateFlow(0f)
    val accZ: StateFlow<Float> = _accZ.asStateFlow()

    private val _currentMagnitude = MutableStateFlow(0.0)
    val currentMagnitude: StateFlow<Double> = _currentMagnitude.asStateFlow()

    private val _magnitudeHistory = MutableStateFlow<List<Double>>(emptyList())
    val magnitudeHistory: StateFlow<List<Double>> = _magnitudeHistory.asStateFlow()

    // --- Connection Status Tracking ---
    private val _lastReceivedTimestamp = MutableStateFlow(0L)
    val lastReceivedTimestamp: StateFlow<Long> = _lastReceivedTimestamp.asStateFlow()

    // Keep up to 50 sample points for smooth graph rendering
    private const val MAX_HISTORY_SIZE = 50

    /**
     * Called when a new heart rate sample arrives from the smartwatch.
     */
    fun updateHeartRate(newValue: Double) {
        _currentHeartRate.value = newValue
        _lastReceivedTimestamp.value = System.currentTimeMillis()

        val history = _heartRateHistory.value.toMutableList()
        history.add(newValue)
        if (history.size > MAX_HISTORY_SIZE) {
            history.removeAt(0)
        }
        _heartRateHistory.value = history
    }

    /**
     * Called when new accelerometer readings arrive from the smartwatch.
     */
    fun updateAccelerometer(x: Float, y: Float, z: Float, magnitude: Double) {
        _accX.value = x
        _accY.value = y
        _accZ.value = z
        _currentMagnitude.value = magnitude
        _lastReceivedTimestamp.value = System.currentTimeMillis()

        val history = _magnitudeHistory.value.toMutableList()
        history.add(magnitude)
        if (history.size > MAX_HISTORY_SIZE) {
            history.removeAt(0)
        }
        _magnitudeHistory.value = history
    }
}
