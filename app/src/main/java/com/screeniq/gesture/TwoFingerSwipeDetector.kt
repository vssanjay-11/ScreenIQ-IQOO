package com.screeniq.gesture

/**
 * Algorithmic detector specifically tuned for 2-finger swipe gestures.
 *
 * Tracks touch pointer coordinates across ACTION_POINTER_DOWN, ACTION_MOVE,
 * and ACTION_POINTER_UP events.
 *
 * Evaluates:
 * 1. Target pointer count (default: 2 fingers).
 * 2. Primary axis displacement (vertical down/up vs horizontal left/right).
 * 3. Threshold distance in pixels (default: 120px).
 * 4. Maximum gesture duration to qualify as a swift swipe rather than a slow drag (default: 750ms).
 */
class TwoFingerSwipeDetector(
    val requiredPointerCount: Int = 2,
    val minSwipeDistancePx: Float = 120f,
    val maxSwipeDurationMs: Long = 750L,
    val maxPerpendicularDriftRatio: Float = 0.65f
) {
    private val startPoints = mutableMapOf<Int, TouchPoint>()
    private val currentPoints = mutableMapOf<Int, TouchPoint>()
    private var gestureStartTimeMs: Long = 0L

    /**
     * Call when a pointer goes down (ACTION_DOWN or ACTION_POINTER_DOWN).
     */
    fun onPointerDown(pointerId: Int, x: Float, y: Float, timeMs: Long = System.currentTimeMillis()) {
        val point = TouchPoint(x, y, timeMs)
        startPoints[pointerId] = point
        currentPoints[pointerId] = point

        if (startPoints.size == requiredPointerCount) {
            gestureStartTimeMs = timeMs
        }
    }

    /**
     * Call when pointers move (ACTION_MOVE).
     */
    fun onPointerMove(pointerId: Int, x: Float, y: Float, timeMs: Long = System.currentTimeMillis()) {
        if (startPoints.containsKey(pointerId)) {
            currentPoints[pointerId] = TouchPoint(x, y, timeMs)
        }
    }

    /**
     * Call when a pointer is lifted (ACTION_POINTER_UP or ACTION_UP).
     *
     * Evaluates whether the released gesture matches the target 2-finger swipe.
     */
    fun onPointerUp(pointerId: Int, timeMs: Long = System.currentTimeMillis()): GestureDetectionResult {
        val result = evaluateGesture(timeMs)

        // Clear tracking for the lifted pointer
        startPoints.remove(pointerId)
        currentPoints.remove(pointerId)
        if (startPoints.isEmpty()) {
            gestureStartTimeMs = 0L
        }

        return result
    }

    /**
     * Resets any active tracking state (e.g., on ACTION_CANCEL).
     */
    fun reset() {
        startPoints.clear()
        currentPoints.clear()
        gestureStartTimeMs = 0L
    }

    /**
     * Returns current active tracked pointer count.
     */
    fun activePointerCount(): Int = startPoints.size

    /**
     * Analyzes accumulated touch coordinates for all active pointers.
     */
    fun evaluateGesture(currentTimeMs: Long = System.currentTimeMillis()): GestureDetectionResult {
        val pointerCount = startPoints.size
        if (pointerCount != requiredPointerCount) {
            return GestureDetectionResult(
                isGestureDetected = false,
                direction = SwipeDirection.INSUFFICIENT_MOVEMENT,
                pointerCount = pointerCount,
                averageDeltaX = 0f,
                averageDeltaY = 0f,
                durationMs = 0L
            )
        }

        val duration = if (gestureStartTimeMs > 0) currentTimeMs - gestureStartTimeMs else 0L
        if (duration > maxSwipeDurationMs || duration < 30L) {
            return GestureDetectionResult(
                isGestureDetected = false,
                direction = SwipeDirection.INSUFFICIENT_MOVEMENT,
                pointerCount = pointerCount,
                averageDeltaX = 0f,
                averageDeltaY = 0f,
                durationMs = duration
            )
        }

        var totalDeltaX = 0f
        var totalDeltaY = 0f

        for ((id, start) in startPoints) {
            val curr = currentPoints[id] ?: start
            totalDeltaX += (curr.x - start.x)
            totalDeltaY += (curr.y - start.y)
        }

        val avgDeltaX = totalDeltaX / pointerCount
        val avgDeltaY = totalDeltaY / pointerCount
        val absDeltaX = Math.abs(avgDeltaX)
        val absDeltaY = Math.abs(avgDeltaY)

        val direction: SwipeDirection
        val isDetected: Boolean

        if (absDeltaY > absDeltaX) {
            // Primarily vertical motion
            val isPerpendicularDriftAcceptable = absDeltaX <= (absDeltaY * maxPerpendicularDriftRatio)
            if (absDeltaY >= minSwipeDistancePx && isPerpendicularDriftAcceptable) {
                direction = if (avgDeltaY < 0) SwipeDirection.UP else SwipeDirection.DOWN
                isDetected = true
            } else {
                direction = SwipeDirection.INSUFFICIENT_MOVEMENT
                isDetected = false
            }
        } else {
            // Primarily horizontal motion
            val isPerpendicularDriftAcceptable = absDeltaY <= (absDeltaX * maxPerpendicularDriftRatio)
            if (absDeltaX >= minSwipeDistancePx && isPerpendicularDriftAcceptable) {
                direction = if (avgDeltaX < 0) SwipeDirection.LEFT else SwipeDirection.RIGHT
                isDetected = true
            } else {
                direction = SwipeDirection.INSUFFICIENT_MOVEMENT
                isDetected = false
            }
        }

        return GestureDetectionResult(
            isGestureDetected = isDetected,
            direction = direction,
            pointerCount = pointerCount,
            averageDeltaX = avgDeltaX,
            averageDeltaY = avgDeltaY,
            durationMs = duration
        )
    }
}
