package com.screeniq.capture.trigger

import com.screeniq.core.contracts.GestureTriggerEvent
import com.screeniq.core.contracts.GestureTriggerListener
import com.screeniq.core.contracts.ScreenCaptureEngine
import com.screeniq.core.model.ScreenCaptureResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Coordinates the end-to-end trigger-to-capture pipeline:
 *
 * Preferred Flow:
 * [Gesture Trigger] (Agent 2)
 *       │
 *       ▼
 * [ScreenCaptureEngine] (Agent 3)
 *       │
 *       ▼
 * [ScreenCaptureResult] (In-memory DTO)
 *       │
 *       ▼
 * [Downstream Content Analyzer / OCR] (Agent 4)
 */
class GestureCaptureCoordinator(
    private val gestureListener: GestureTriggerListener,
    private val captureEngine: ScreenCaptureEngine,
    private val onCaptureResult: suspend (Result<ScreenCaptureResult>) -> Unit
) {

    private var activeJob: Job? = null

    /**
     * Starts listening for multi-touch gesture events and triggers screen capture upon receipt.
     */
    fun start(scope: CoroutineScope) {
        if (activeJob?.isActive == true) return

        gestureListener.enableDetection()

        activeJob = scope.launch {
            try {
                gestureListener.gestureEvents.collectLatest { event ->
                    onGestureReceived(event)
                }
            } catch (_: CancellationException) {
                // Normal job cancellation
            }
        }
    }

    /**
     * Directly processes a gesture event and executes screen capture.
     */
    suspend fun onGestureReceived(event: GestureTriggerEvent) {
        // Execute in-memory screen capture
        val result = captureEngine.captureCurrentScreen()

        // Forward capture result (or structured failure) to downstream analyzer
        onCaptureResult(result)
    }

    /**
     * Programmatic trigger for testing or alternate triggers (e.g. shortcut or quick settings tile).
     */
    suspend fun triggerCaptureDirectly(): Result<ScreenCaptureResult> {
        val result = captureEngine.captureCurrentScreen()
        onCaptureResult(result)
        return result
    }

    /**
     * Stops listening and disables gesture detection.
     */
    fun stop() {
        activeJob?.cancel()
        activeJob = null
        gestureListener.disableDetection()
    }
}
