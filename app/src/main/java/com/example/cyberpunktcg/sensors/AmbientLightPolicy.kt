package com.example.cyberpunktcg.sensors

internal data class LightSensorState(
    val available: Boolean,
    val lux: Float? = null,
    val automaticDarkTheme: Boolean? = null,
    val isLowLight: Boolean = false,
)

// Turns lux readings into two decisions, one for Auto theme and one for the camera warning.
internal class AmbientLightPolicy {
    private var state = LightSensorState(available = true)

    fun update(lux: Float): LightSensorState {
        if (!lux.isFinite() || lux < 0f) return state

        // Separate entry/exit thresholds keep a noisy reading from flickering the UI.
        val darkTheme = when {
            lux <= 40f -> true
            lux >= 80f -> false
            else -> state.automaticDarkTheme
        }
        val lowLight = when {
            lux < 50f -> true
            lux >= 80f -> false
            else -> state.isLowLight
        }
        state = LightSensorState(true, lux, darkTheme, lowLight)
        return state
    }
}
