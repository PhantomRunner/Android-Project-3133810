package com.example.cyberpunktcg.data.model

internal enum class ThemeMode {
    LIGHT, DARK, AUTO;

    // Auto follows the system when the light sensor has no dark or light choice yet.
    fun resolveDarkTheme(automaticDarkTheme: Boolean?, systemDarkTheme: Boolean): Boolean = when (this) {
        LIGHT -> false
        DARK -> true
        AUTO -> automaticDarkTheme ?: systemDarkTheme
    }
}
