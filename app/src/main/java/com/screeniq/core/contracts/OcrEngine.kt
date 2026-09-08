package com.screeniq.core.contracts

import com.screeniq.core.model.ScreenCaptureResult
import com.screeniq.core.model.ScreenContent

/**
 * Agent 4: OCR Engine Contract
 * Transforms an in-memory ScreenCaptureResult into structured ScreenContent.
 */
interface OcrEngine {
    suspend fun extractContent(captureResult: ScreenCaptureResult): Result<ScreenContent>
}
