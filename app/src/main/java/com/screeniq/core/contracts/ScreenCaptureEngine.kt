package com.screeniq.core.contracts

import com.screeniq.core.model.ScreenCaptureResult

/**
 * Agent 3: Screen Capture Engine
 *
 * Interface contract governing in-memory capture of the active screen surface.
 * Defined in ARCHITECTURE.md §4.
 */
interface ScreenCaptureEngine {
    /**
     * Captures the currently visible screen content into memory.
     *
     * @return [Result] containing [ScreenCaptureResult] on success,
     *         or a failure containing a structured [CaptureException].
     */
    suspend fun captureCurrentScreen(): Result<ScreenCaptureResult>
}
