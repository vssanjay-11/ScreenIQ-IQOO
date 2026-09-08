package com.screeniq.capture.accessibility

import android.accessibilityservice.AccessibilityService
import java.util.concurrent.Executor

/**
 * Interface bridging the Screen Capture Subsystem to the host [AccessibilityService].
 *
 * This abstraction decouples Agent 3 from the concrete lifecycle of Agent 2's
 * AccessibilityService implementation and allows unit testing without a physical device.
 */
interface AccessibilityCaptureBridge {
    /**
     * True if the underlying AccessibilityService is actively running and bound.
     */
    val isServiceConnected: Boolean

    /**
     * Package name of the active foreground app if available from accessibility window hierarchy.
     */
    val currentForegroundPackage: String?

    /**
     * Delegate for [AccessibilityService.takeScreenshot].
     */
    fun takeScreenshot(
        displayId: Int,
        executor: Executor,
        callback: AccessibilityService.TakeScreenshotCallback
    )
}
