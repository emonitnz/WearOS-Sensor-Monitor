package com.example.myapplication

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * A singleton repository to share real-time sensor data between the background
 * WearService and the Compose Dashboard UI.
 */
object SensorDataRepository {
    // Current heart rate (moving average)
    private val _currentHeartRate = MutableStateFlow(0.0)
    val currentHeartRate: StateFlow<Double> = _currentHeartRate.asStateFlow()

    // History of heart rate values for the chart
    private val _heartRateHistory = MutableStateFlow<List<Double>>(emptyList())
    val heartRateHistory: StateFlow<List<Double>> = _heartRateHistory.asStateFlow()

    // Acceleration Magnitude Data
    private val _currentMagnitude = MutableStateFlow(0.0)
    val currentMagnitude: StateFlow<Double> = _currentMagnitude.asStateFlow()

    private val _magnitudeHistory = MutableStateFlow<List<Double>>(emptyList())
    val magnitudeHistory: StateFlow<List<Double>> = _magnitudeHistory.asStateFlow()

    // Max number of points to keep in history chart
    private const val MAX_HISTORY_SIZE = 50

    /**
     * Updates the repository with a new heart rate value.
     */
    fun updateHeartRate(newValue: Double) {
        _currentHeartRate.value = newValue

        val currentHistory = _heartRateHistory.value.toMutableList()
        currentHistory.add(newValue)
        if (currentHistory.size > MAX_HISTORY_SIZE) {
            currentHistory.removeAt(0)
        }
        _heartRateHistory.value = currentHistory
    }

    /**
     * Updates the repository with a new magnitude value.
     */
    fun updateMagnitude(newValue: Double) {
        _currentMagnitude.value = newValue

        val currentHistory = _magnitudeHistory.value.toMutableList()
        currentHistory.add(newValue)
        if (currentHistory.size > MAX_HISTORY_SIZE) {
            currentHistory.removeAt(0)
        }
        _magnitudeHistory.value = currentHistory
    }
}
