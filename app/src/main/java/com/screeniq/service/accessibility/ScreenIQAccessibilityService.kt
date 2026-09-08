package com.screeniq.service.accessibility

import android.accessibilityservice.AccessibilityGestureEvent
import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.screeniq.core.contracts.GestureTriggerEvent
import com.screeniq.gesture.GestureTriggerManager

/**
 * Android AccessibilityService responsible for observing system-wide multi-finger
 * gestures and dispatching trigger events to ScreenIQ.
 *
 * Strict Architectural Boundary:
 * - Responsible ONLY for gesture detection and trigger dispatch.
 * - Does NOT capture screenshots directly.
 * - Does NOT perform OCR, AI inference, or UI display.
 */
class ScreenIQAccessibilityService : AccessibilityService() {

    private val gestureManager = GestureTriggerManager.getInstance()

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.i(TAG, "ScreenIQ AccessibilityService connected.")

        configureServiceInfo()
        gestureManager.updateServiceConnectionState(true)
    }

    private fun configureServiceInfo() {
        val currentInfo = serviceInfo ?: AccessibilityServiceInfo()

        var updatedFlags = currentInfo.flags or
                AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS

        // Multi-finger gesture observation requires FLAG_REQUEST_MULTI_FINGER_GESTURES on Android 12+ (API 31+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            updatedFlags = updatedFlags or AccessibilityServiceInfo.FLAG_REQUEST_MULTI_FINGER_GESTURES
        }

        // FLAG_REQUEST_TOUCH_EXPLORATION_MODE is required for system-level gesture capture
        updatedFlags = updatedFlags or AccessibilityServiceInfo.FLAG_REQUEST_TOUCH_EXPLORATION_MODE

        currentInfo.flags = updatedFlags
        currentInfo.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC

        try {
            serviceInfo = currentInfo
            Log.d(TAG, "AccessibilityServiceInfo successfully updated with multi-finger gesture flags.")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to dynamically configure serviceInfo: ${e.message}")
        }
    }

    /**
     * Modern gesture detection API introduced in Android 11 (API 30) and expanded with
     * multi-finger gestures in Android 12 (API 31).
     */
    override fun onGesture(gestureEvent: AccessibilityGestureEvent): Boolean {
        val gestureId = gestureEvent.gestureId
        Log.d(TAG, "AccessibilityGestureEvent received: gestureId=$gestureId")

        return handleGestureId(gestureId)
    }

    /**
     * Legacy gesture callback for older Android API levels.
     */
    @Deprecated("Deprecated in Java")
    override fun onGesture(gestureId: Int): Boolean {
        Log.d(TAG, "Legacy onGesture received: gestureId=$gestureId")
        return handleGestureId(gestureId)
    }

    private fun handleGestureId(gestureId: Int): Boolean {
        val isTargetGesture = when {
            // Android 12+ (API 31+) GESTURE_4_FINGER_SWIPE_UP is constant 37
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && gestureId == AccessibilityService.GESTURE_4_FINGER_SWIPE_UP -> true
            // Support direct constant check (value 37 corresponds to GESTURE_4_FINGER_SWIPE_UP)
            gestureId == 37 -> true
            // Also recognize 4-finger swipes in other directions as intentional ScreenIQ gestures if enabled
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && (
                gestureId == AccessibilityService.GESTURE_4_FINGER_SWIPE_DOWN ||
                gestureId == AccessibilityService.GESTURE_4_FINGER_SWIPE_LEFT ||
                gestureId == AccessibilityService.GESTURE_4_FINGER_SWIPE_RIGHT
            ) -> {
                Log.i(TAG, "Detected alternative 4-finger swipe direction (id=$gestureId). Dispatching trigger.")
                true
            }
            else -> false
        }

        if (isTargetGesture) {
            Log.i(TAG, "Detected Four-Finger Swipe gesture! Dispatching ScreenIQ trigger event.")
            val event = GestureTriggerEvent(
                pointerCount = 4,
                timestampMs = System.currentTimeMillis()
            )
            val accepted = gestureManager.notifyTrigger(event)
            Log.d(TAG, "Trigger event dispatch status: accepted=$accepted")
            return true
        }

        return super.onGesture(gestureId)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Accessibility events are not consumed here to preserve zero overhead on system UI.
    }

    override fun onInterrupt() {
        Log.w(TAG, "ScreenIQ AccessibilityService interrupted.")
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        Log.i(TAG, "ScreenIQ AccessibilityService destroyed.")
        gestureManager.updateServiceConnectionState(false)
    }

    companion object {
        private const val TAG = "ScreenIQ:A11yService"

        @Volatile
        var instance: ScreenIQAccessibilityService? = null
            private set

        val captureBridge: com.screeniq.capture.accessibility.AccessibilityCaptureBridge =
            object : com.screeniq.capture.accessibility.AccessibilityCaptureBridge {
                override val isServiceConnected: Boolean
                    get() = instance != null

                override val currentForegroundPackage: String?
                    get() = try {
                        instance?.rootInActiveWindow?.packageName?.toString()
                    } catch (_: Exception) {
                        null
                    }

                override fun takeScreenshot(
                    displayId: Int,
                    executor: java.util.concurrent.Executor,
                    callback: AccessibilityService.TakeScreenshotCallback
                ) {
                    val s = instance
                    if (s != null && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                        s.takeScreenshot(displayId, executor, callback)
                    } else {
                        callback.onFailure(AccessibilityService.ERROR_TAKE_SCREENSHOT_NO_ACCESSIBILITY_ACCESS)
                    }
                }
            }
    }
}
