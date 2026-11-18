package com.example.cyberpunktcg.sensors

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShakeDetectorTest {
    @Test
    fun restingInDifferentOrientationsDoesNotTriggerAShake() {
        val detector = ShakeDetector()
        assertFalse(detector.onAcceleration(0f, 0f, 9.80665f, 0))
        assertFalse(detector.onAcceleration(9.80665f, 0f, 0f, 100_000_000))
        assertFalse(detector.onAcceleration(0f, -9.80665f, 0f, 200_000_000))
    }

    @Test
    fun aHighReadingOnSubscriptionIsNotReplayedAsAShake() {
        val detector = ShakeDetector()
        assertFalse(detector.onAcceleration(30f, 0f, 0f, 0))
        assertFalse(detector.onAcceleration(0f, 0f, 9.80665f, 10_000_000))
        assertTrue(detector.onAcceleration(30f, 0f, 0f, 20_000_000))
    }

    @Test
    fun aHeldHighReadingDoesNotRepeatAfterTheCooldown() {
        val detector = ShakeDetector()
        detector.onAcceleration(0f, 0f, 9.80665f, 0)
        assertTrue(detector.onAcceleration(30f, 0f, 0f, 100_000_000))
        assertFalse(detector.onAcceleration(30f, 0f, 0f, 1_500_000_000))
        detector.onAcceleration(0f, 0f, 9.80665f, 1_600_000_000)
        assertTrue(detector.onAcceleration(30f, 0f, 0f, 1_700_000_000))
    }

    @Test
    fun aSecondMotionNeedsOneSecondSinceTheLastAcceptedShake() {
        val detector = ShakeDetector()
        detector.onAcceleration(0f, 0f, 9.80665f, 0)
        assertTrue(detector.onAcceleration(30f, 0f, 0f, 100_000_000))
        detector.onAcceleration(0f, 0f, 9.80665f, 200_000_000)
        assertFalse(detector.onAcceleration(30f, 0f, 0f, 900_000_000))
        detector.onAcceleration(0f, 0f, 9.80665f, 1_000_000_000)
        assertTrue(detector.onAcceleration(30f, 0f, 0f, 1_100_000_000))
    }

    @Test
    fun invalidSamplesDoNotTriggerOrResetTheDetector() {
        val detector = ShakeDetector()
        detector.onAcceleration(0f, 0f, 9.80665f, 0)
        assertFalse(detector.onAcceleration(Float.NaN, 0f, 0f, 10))
        assertFalse(detector.onAcceleration(Float.POSITIVE_INFINITY, 0f, 0f, 20))
        assertFalse(detector.onAcceleration(30f, 0f, 0f, -1))
        assertTrue(detector.onAcceleration(30f, 0f, 0f, 30))
    }
}
