package com.example.cyberpunktcg.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate

internal class AccelerometerSensor(context: Context) {
    private val manager = context.applicationContext.getSystemService(SensorManager::class.java)
    private val sensor = manager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    val isAvailable: Boolean get() = sensor != null

    val shakes = callbackFlow {
        val sensorManager = manager
        val accelerometer = sensor
        if (sensorManager == null || accelerometer == null) {
            close()
            return@callbackFlow
        }
        val detector = ShakeDetector()
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (detector.onAcceleration(event.values[0], event.values[1], event.values[2], event.timestamp)) {
                    trySend(Unit)
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        if (!sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_GAME)) {
            close()
            return@callbackFlow
        }
        awaitClose { sensorManager.unregisterListener(listener) }
    }.conflate()
}
