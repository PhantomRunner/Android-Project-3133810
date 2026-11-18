package com.example.cyberpunktcg.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.test.platform.app.InstrumentationRegistry
import com.example.cyberpunktcg.data.model.ThemeMode
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.UUID

class ThemePreferencesTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val name = "theme-test-${UUID.randomUUID()}"
    private lateinit var storage: SharedPreferences

    @Before
    fun openPreferences() {
        storage = context.getSharedPreferences(name, Context.MODE_PRIVATE)
    }

    @After
    fun deletePreferences() {
        context.deleteSharedPreferences(name)
    }

    @Test
    fun aNewInstallKeepsDarkAsTheDefault() {
        assertEquals(ThemeMode.DARK, ThemePreferences(storage).themeMode())
    }

    @Test
    fun existingLightAndDarkChoicesAreReadWithoutResettingThem() {
        storage.edit().putBoolean("dark_theme", false).commit()
        assertEquals(ThemeMode.LIGHT, ThemePreferences(storage).themeMode())
        storage.edit().putBoolean("dark_theme", true).commit()
        assertEquals(ThemeMode.DARK, ThemePreferences(storage).themeMode())
    }

    @Test
    fun autoSurvivesReopeningAndTakesPrecedenceOverTheLegacyChoice() {
        storage.edit().putBoolean("dark_theme", false).commit()
        ThemePreferences(storage).saveThemeMode(ThemeMode.AUTO)
        assertEquals(ThemeMode.AUTO, ThemePreferences(storage).themeMode())
    }
}
