package com.example.cyberpunktcg.sensors

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

internal class SensorsViewModel(application: Application) : AndroidViewModel(application) {
    private val lightSensor = AmbientLightSensor(application)

    // Auto theme and camera share one listener. The last subscriber releases it immediately.
    val light = lightSensor.readings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 0, replayExpirationMillis = 0),
        initialValue = LightSensorState(available = lightSensor.isAvailable),
    )
}
