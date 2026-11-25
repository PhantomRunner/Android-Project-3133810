package com.example.cyberpunktcg.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.cyberpunktcg.data.model.ThemeMode

// SharedPreferences stores the chosen theme so it can be restored after the app closes.
internal class ThemePreferences(private val preferences: SharedPreferences) {
    constructor(context: Context) : this(context.getSharedPreferences("theme_preferences", Context.MODE_PRIVATE))

    fun themeMode(): ThemeMode {
        // Older installs may still have the Boolean dark theme setting.
        val savedMode = preferences.getString("theme_mode", null)
        return ThemeMode.entries.firstOrNull { it.name == savedMode }
            ?: if (preferences.getBoolean("dark_theme", true)) ThemeMode.DARK else ThemeMode.LIGHT
    }

    fun saveThemeMode(mode: ThemeMode) {
        // apply() updates memory immediately and writes to disk asynchronously.
        preferences.edit().putString("theme_mode", mode.name).apply()
    }
}
