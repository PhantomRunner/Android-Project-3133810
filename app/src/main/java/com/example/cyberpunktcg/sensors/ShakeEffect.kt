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
    val currentAction by rememberUpdatedState(onShake)
    LaunchedEffect(events, lifecycle, enabled) {
        if (enabled) {
            lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                events.collect { currentAction() }
            }
        }
    }
}
