package com.example.cyberpunktcg.sensors

import kotlin.math.sqrt

internal class ShakeDetector {
    private var armed = false
    private var lastShakeNanos: Long? = null

    fun onAcceleration(x: Float, y: Float, z: Float, timestampNanos: Long): Boolean {
        if (!x.isFinite() || !y.isFinite() || !z.isFinite() || timestampNanos < 0) return false
        val gravity = sqrt(x.toDouble() * x + y.toDouble() * y + z.toDouble() * z) / 9.80665
        if (gravity < 1.3) armed = true
        if (gravity < 2.7 || !armed) return false

        // A held high value is one motion, even after the cooldown has elapsed.
        armed = false
        val previous = lastShakeNanos
        if (previous != null && timestampNanos - previous < 1_000_000_000L) return false
        lastShakeNanos = timestampNanos
        return true
    }
}
