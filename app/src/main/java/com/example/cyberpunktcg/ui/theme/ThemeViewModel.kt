package com.example.cyberpunktcg.ui.theme

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.example.cyberpunktcg.data.local.ThemePreferences
import com.example.cyberpunktcg.data.model.ThemeMode

internal class ThemeViewModel(application: Application) : AndroidViewModel(application) {
    private val preferences = ThemePreferences(application)

    var mode by mutableStateOf(preferences.themeMode())
        private set

    fun changeMode(mode: ThemeMode) {
        if (this.mode == mode) return

        preferences.saveThemeMode(mode)
        this.mode = mode
    }
}
