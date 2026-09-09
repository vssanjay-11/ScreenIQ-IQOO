package com.screeniq.gesture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TwoFingerSwipeDetectorTest {

    private lateinit var detector: TwoFingerSwipeDetector

    @Before
    fun setUp() {
        detector = TwoFingerSwipeDetector(
            requiredPointerCount = 2,
            minSwipeDistancePx = 100f,
            maxSwipeDurationMs = 600L,
            maxPerpendicularDriftRatio = 0.6f
        )
    }

    @Test
    fun testValidTwoFingerSwipeDown() {
        val t0 = 1000L
        detector.onPointerDown(pointerId = 0, x = 200f, y = 300f, timeMs = t0)
        detector.onPointerDown(pointerId = 1, x = 400f, y = 300f, timeMs = t0)

        assertEquals(2, detector.activePointerCount())

        val t1 = 1200L
        detector.onPointerMove(pointerId = 0, x = 210f, y = 550f, timeMs = t1)
        detector.onPointerMove(pointerId = 1, x = 410f, y = 560f, timeMs = t1)

        val result = detector.onPointerUp(pointerId = 0, timeMs = t1)

        assertTrue("Two-finger swipe down must be detected", result.isGestureDetected)
        assertEquals(SwipeDirection.DOWN, result.direction)
        assertEquals(2, result.pointerCount)
        assertTrue("Delta Y should be positive (downward)", result.averageDeltaY > 200f)
    }

    @Test
    fun testValidTwoFingerSwipeUp() {
        val t0 = 1000L
        detector.onPointerDown(pointerId = 0, x = 200f, y = 700f, timeMs = t0)
        detector.onPointerDown(pointerId = 1, x = 400f, y = 700f, timeMs = t0)

        val t1 = 1250L
        detector.onPointerMove(pointerId = 0, x = 205f, y = 400f, timeMs = t1)
        detector.onPointerMove(pointerId = 1, x = 395f, y = 410f, timeMs = t1)

        val result = detector.onPointerUp(pointerId = 1, timeMs = t1)

        assertTrue("Two-finger swipe up must be detected", result.isGestureDetected)
        assertEquals(SwipeDirection.UP, result.direction)
        assertTrue("Delta Y should be negative (upward)", result.averageDeltaY < -200f)
    }

    @Test
    fun testRejectsSingleFingerMovement() {
        val t0 = 1000L
        detector.onPointerDown(pointerId = 0, x = 200f, y = 300f, timeMs = t0)

        detector.onPointerMove(pointerId = 0, x = 200f, y = 600f, timeMs = 1100L)

        val result = detector.onPointerUp(pointerId = 0, timeMs = 1100L)

        assertFalse("Single finger swipe must not trigger two-finger detector", result.isGestureDetected)
        assertEquals(1, result.pointerCount)
    }

    @Test
    fun testRejectsInsufficientDistance() {
        val t0 = 1000L
        detector.onPointerDown(pointerId = 0, x = 200f, y = 300f, timeMs = t0)
        detector.onPointerDown(pointerId = 1, x = 400f, y = 300f, timeMs = t0)

        // Only move 30 pixels (threshold is 100)
        detector.onPointerMove(pointerId = 0, x = 200f, y = 330f, timeMs = 1100L)
        detector.onPointerMove(pointerId = 1, x = 400f, y = 330f, timeMs = 1100L)

        val result = detector.onPointerUp(pointerId = 0, timeMs = 1100L)

        assertFalse("Movement below distance threshold must be rejected", result.isGestureDetected)
        assertEquals(SwipeDirection.INSUFFICIENT_MOVEMENT, result.direction)
    }

    @Test
    fun testResetClearsTrackingState() {
        detector.onPointerDown(0, 100f, 100f)
        detector.onPointerDown(1, 200f, 100f)
        assertEquals(2, detector.activePointerCount())

        detector.reset()
        assertEquals(0, detector.activePointerCount())
    }
}
