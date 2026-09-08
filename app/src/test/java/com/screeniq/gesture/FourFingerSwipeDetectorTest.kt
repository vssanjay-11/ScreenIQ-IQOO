package com.screeniq.gesture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FourFingerSwipeDetectorTest {

    private lateinit var detector: FourFingerSwipeDetector

    @Before
    fun setUp() {
        detector = FourFingerSwipeDetector(
            requiredPointerCount = 4,
            minSwipeDistancePx = 150f,
            maxSwipeDurationMs = 800L,
            maxPerpendicularDriftRatio = 0.6f
        )
    }

    @Test
    fun testValidFourFingerSwipeUpDetected() {
        val startTime = 1000L
        val endTime = 1200L // 200ms duration

        // 4 pointers touch down
        detector.onPointerDown(0, 100f, 600f, startTime)
        detector.onPointerDown(1, 200f, 610f, startTime)
        detector.onPointerDown(2, 300f, 605f, startTime)
        detector.onPointerDown(3, 400f, 620f, startTime)

        assertEquals(4, detector.activePointerCount())

        // 4 pointers move up by 250px (y decreases)
        detector.onPointerMove(0, 105f, 350f, endTime)
        detector.onPointerMove(1, 195f, 360f, endTime)
        detector.onPointerMove(2, 302f, 355f, endTime)
        detector.onPointerMove(3, 398f, 370f, endTime)

        val result = detector.onPointerUp(0, endTime)

        assertTrue("Should detect 4-finger swipe up", result.isGestureDetected)
        assertEquals(SwipeDirection.UP, result.direction)
        assertEquals(4, result.pointerCount)
        assertTrue("Displacement should be negative (upward)", result.averageDeltaY < -150f)
    }

    @Test
    fun testThreeFingersRejected() {
        val startTime = 1000L
        val endTime = 1200L

        // Only 3 pointers touch down
        detector.onPointerDown(0, 100f, 600f, startTime)
        detector.onPointerDown(1, 200f, 610f, startTime)
        detector.onPointerDown(2, 300f, 605f, startTime)

        detector.onPointerMove(0, 100f, 300f, endTime)
        detector.onPointerMove(1, 200f, 300f, endTime)
        detector.onPointerMove(2, 300f, 300f, endTime)

        val result = detector.onPointerUp(0, endTime)

        assertFalse("3 fingers should not trigger 4-finger detector", result.isGestureDetected)
        assertEquals(3, result.pointerCount)
    }

    @Test
    fun testInsufficientDisplacementRejected() {
        val startTime = 1000L
        val endTime = 1200L

        // 4 pointers touch down
        detector.onPointerDown(0, 100f, 600f, startTime)
        detector.onPointerDown(1, 200f, 600f, startTime)
        detector.onPointerDown(2, 300f, 600f, startTime)
        detector.onPointerDown(3, 400f, 600f, startTime)

        // Only small upward movement: 50px (< 150px threshold)
        detector.onPointerMove(0, 100f, 550f, endTime)
        detector.onPointerMove(1, 200f, 550f, endTime)
        detector.onPointerMove(2, 300f, 550f, endTime)
        detector.onPointerMove(3, 400f, 550f, endTime)

        val result = detector.onPointerUp(0, endTime)

        assertFalse("Small movement should be rejected", result.isGestureDetected)
        assertEquals(SwipeDirection.INSUFFICIENT_MOVEMENT, result.direction)
    }

    @Test
    fun testSwipeDownDetected() {
        val startTime = 1000L
        val endTime = 1250L

        detector.onPointerDown(0, 100f, 200f, startTime)
        detector.onPointerDown(1, 200f, 200f, startTime)
        detector.onPointerDown(2, 300f, 200f, startTime)
        detector.onPointerDown(3, 400f, 200f, startTime)

        // Move downward by 200px (y increases)
        detector.onPointerMove(0, 100f, 400f, endTime)
        detector.onPointerMove(1, 200f, 400f, endTime)
        detector.onPointerMove(2, 300f, 400f, endTime)
        detector.onPointerMove(3, 400f, 400f, endTime)

        val result = detector.onPointerUp(0, endTime)

        assertTrue(result.isGestureDetected)
        assertEquals(SwipeDirection.DOWN, result.direction)
        assertTrue(result.averageDeltaY > 150f)
    }

    @Test
    fun testExcessivePerpendicularDriftRejected() {
        val startTime = 1000L
        val endTime = 1200L

        detector.onPointerDown(0, 100f, 600f, startTime)
        detector.onPointerDown(1, 200f, 600f, startTime)
        detector.onPointerDown(2, 300f, 600f, startTime)
        detector.onPointerDown(3, 400f, 600f, startTime)

        // Moves upward by 200px, but drifts horizontally by 180px (> 60% of 200px)
        detector.onPointerMove(0, 280f, 400f, endTime)
        detector.onPointerMove(1, 380f, 400f, endTime)
        detector.onPointerMove(2, 480f, 400f, endTime)
        detector.onPointerMove(3, 580f, 400f, endTime)

        val result = detector.onPointerUp(0, endTime)

        assertFalse("Excessive diagonal drift should be rejected", result.isGestureDetected)
    }

    @Test
    fun testSlowDragExceedingMaxDurationRejected() {
        val startTime = 1000L
        val endTime = 2200L // 1200ms duration (> 800ms)

        detector.onPointerDown(0, 100f, 600f, startTime)
        detector.onPointerDown(1, 200f, 600f, startTime)
        detector.onPointerDown(2, 300f, 600f, startTime)
        detector.onPointerDown(3, 400f, 600f, startTime)

        detector.onPointerMove(0, 100f, 350f, endTime)
        detector.onPointerMove(1, 200f, 350f, endTime)
        detector.onPointerMove(2, 300f, 350f, endTime)
        detector.onPointerMove(3, 400f, 350f, endTime)

        val result = detector.onPointerUp(0, endTime)

        assertFalse("Slow drag exceeding max duration should be rejected", result.isGestureDetected)
    }
}
