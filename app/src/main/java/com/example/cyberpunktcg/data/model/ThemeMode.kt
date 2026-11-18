package com.example.cyberpunktcg.data.model

internal enum class ThemeMode {
    LIGHT, DARK, AUTO;

    fun resolveDarkTheme(automaticDarkTheme: Boolean?, systemDarkTheme: Boolean): Boolean = when (this) {
        LIGHT -> false
        DARK -> true
        AUTO -> automaticDarkTheme ?: systemDarkTheme
    }
}
