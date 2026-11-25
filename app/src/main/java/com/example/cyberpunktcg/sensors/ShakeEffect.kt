package com.example.cyberpunktcg.sensors

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.Flow

@Composable
internal fun ShakeEffect(events: Flow<Unit>, enabled: Boolean, onShake: () -> Unit) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    // Use the latest action after recomposition without restarting the sensor collection.
    val currentAction by rememberUpdatedState(onShake)
    LaunchedEffect(events, lifecycle, enabled) {
        if (enabled) {
            // Listen only while this screen is resumed and the shortcut is enabled.
            lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                events.collect { currentAction() }
            }
        }
    }
}
