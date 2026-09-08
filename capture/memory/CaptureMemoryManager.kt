package com.screeniq.capture.memory

import android.graphics.Bitmap
import com.screeniq.core.model.ScreenCaptureResult

/**
 * Utility for managing in-memory lifecycle of captured screen frames.
 *
 * Enforces ScreenIQ Privacy & Memory Guarantees:
 * 1. Bitmaps reside strictly in transient RAM.
 * 2. No disk caching or persistent storage of pixels.
 * 3. Safe recycling prevents native graphic buffer and Out-Of-Memory (OOM) leaks.
 */
object CaptureMemoryManager {

    /**
     * Safely recycles the [Bitmap] associated with a [ScreenCaptureResult]
     * if it has not already been recycled.
     */
    fun recycle(result: ScreenCaptureResult?) {
        result?.bitmap?.let { bitmap ->
            safeRecycle(bitmap)
        }
    }

    /**
     * Safely recycles a [Bitmap], guarding against null and already-recycled states.
     */
    fun safeRecycle(bitmap: Bitmap?) {
        if (bitmap != null && !bitmap.isRecycled) {
            try {
                bitmap.recycle()
            } catch (_: Throwable) {
                // Ignore any native recycling race conditions
            }
        }
    }

    /**
     * Scoped execution helper that ensures the captured screen bitmap
     * is unconditionally recycled after [block] finishes execution.
     *
     * Example usage by downstream consumers (e.g. Agent 4 OCR):
     * ```
     * CaptureMemoryManager.use(captureResult) { result ->
     *     ocrEngine.process(result.bitmap)
     * }
     * ```
     */
    inline fun <R> use(result: ScreenCaptureResult, block: (ScreenCaptureResult) -> R): R {
        return try {
            block(result)
        } finally {
            recycle(result)
        }
    }
}
