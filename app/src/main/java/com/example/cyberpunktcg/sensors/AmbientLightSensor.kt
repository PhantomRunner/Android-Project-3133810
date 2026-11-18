package com.example.cyberpunktcg.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate

internal class AmbientLightSensor(context: Context) {
    private val manager = context.applicationContext.getSystemService(SensorManager::class.java)
    private val sensor = manager?.getDefaultSensor(Sensor.TYPE_LIGHT)
    val isAvailable: Boolean get() = sensor != null

    val readings = callbackFlow {
        val sensorManager = manager
        val lightSensor = sensor
        if (sensorManager == null || lightSensor == null) {
            trySend(LightSensorState(available = false))
            close()
            return@callbackFlow
        }

        val policy = AmbientLightPolicy()
        trySend(LightSensorState(available = true))
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                trySend(policy.update(event.values[0]))
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        if (!sensorManager.registerListener(listener, lightSensor, SensorManager.SENSOR_DELAY_NORMAL)) {
            trySend(LightSensorState(available = false))
            close()
            return@callbackFlow
        }
        awaitClose { sensorManager.unregisterListener(listener) }
    }.conflate()
}
