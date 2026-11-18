package com.example.cyberpunktcg.ui.theme

import com.example.cyberpunktcg.data.model.ThemeMode
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeModeTest {
    @Test
    fun manualModesOverrideBothTheSensorAndSystemTheme() {
        assertFalse(ThemeMode.LIGHT.resolveDarkTheme(true, true))
        assertTrue(ThemeMode.DARK.resolveDarkTheme(false, false))
    }

    @Test
    fun autoUsesTheSensorWhenAThemeDecisionIsAvailable() {
        assertTrue(ThemeMode.AUTO.resolveDarkTheme(true, false))
        assertFalse(ThemeMode.AUTO.resolveDarkTheme(false, true))
    }

    @Test
    fun autoFallsBackToTheSystemUntilAReadingIsAvailable() {
        assertTrue(ThemeMode.AUTO.resolveDarkTheme(null, true))
        assertFalse(ThemeMode.AUTO.resolveDarkTheme(null, false))
    }
}
