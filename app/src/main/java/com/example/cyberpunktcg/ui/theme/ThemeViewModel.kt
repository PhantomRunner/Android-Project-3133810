package com.example.cyberpunktcg.ui.theme

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.example.cyberpunktcg.data.local.ThemePreferences
import com.example.cyberpunktcg.data.model.ThemeMode

// Loads the saved theme choice and exposes it as state for the current Activity.
internal class ThemeViewModel(application: Application) : AndroidViewModel(application) {
    private val preferences = ThemePreferences(application)

    // Compose recomposes the parts of the UI that read this value when it changes.
    var mode by mutableStateOf(preferences.themeMode())
        private set

    fun changeMode(mode: ThemeMode) {
        if (this.mode == mode) return

        preferences.saveThemeMode(mode)
        this.mode = mode
    }
}
