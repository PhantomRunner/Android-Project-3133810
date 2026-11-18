package com.example.cyberpunktcg.sensors

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AmbientLightPolicyTest {
    @Test
    fun anInitialReadingBetweenThresholdsLeavesTheThemeToTheSystem() {
        val state = AmbientLightPolicy().update(60f)
        assertEquals(60f, state.lux)
        assertNull(state.automaticDarkTheme)
        assertFalse(state.isLowLight)
    }

    @Test
    fun themeKeepsItsPreviousChoiceBetweenFortyAndEightyLux() {
        val policy = AmbientLightPolicy()
        assertEquals(true, policy.update(40f).automaticDarkTheme)
        assertEquals(true, policy.update(79f).automaticDarkTheme)
        assertEquals(false, policy.update(80f).automaticDarkTheme)
        assertEquals(false, policy.update(41f).automaticDarkTheme)
        assertEquals(true, policy.update(39f).automaticDarkTheme)
    }

    @Test
    fun lowLightWarningHasSeparateEntryAndExitThresholds() {
        val policy = AmbientLightPolicy()
        assertFalse(policy.update(50f).isLowLight)
        assertTrue(policy.update(49f).isLowLight)
        assertTrue(policy.update(70f).isLowLight)
        assertFalse(policy.update(80f).isLowLight)
        assertFalse(policy.update(50f).isLowLight)
    }

    @Test
    fun zeroLuxIsAValidDarkReading() {
        val state = AmbientLightPolicy().update(0f)
        assertTrue(state.available)
        assertEquals(0f, state.lux)
        assertEquals(true, state.automaticDarkTheme)
        assertTrue(state.isLowLight)
    }

    @Test
    fun invalidReadingsDoNotReplaceTheLastValidState() {
        val policy = AmbientLightPolicy()
        val previous = policy.update(120f)
        listOf(-1f, Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY).forEach { lux ->
            assertEquals(previous, policy.update(lux))
        }
    }
}
