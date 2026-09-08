package com.screeniq.capture.testing

import android.graphics.Bitmap
import com.screeniq.capture.error.CaptureError
import com.screeniq.capture.error.CaptureException
import com.screeniq.core.contracts.ScreenCaptureEngine
import com.screeniq.core.model.ScreenCaptureResult
import java.util.UUID

/**
 * Test fake implementing [ScreenCaptureEngine] for deterministic unit testing
 * and for downstream agents (e.g. Agent 4 OCR, Agent 10 Integration).
 */
class FakeScreenCaptureEngine : ScreenCaptureEngine {

    var simulatedResult: Result<ScreenCaptureResult>? = null
    var captureInvocationCount: Int = 0
        private set

    /**
     * Configures the fake to return a successful mock [ScreenCaptureResult].
     */
    fun enqueueSuccess(
        width: Int = 1080,
        height: Int = 2400,
        sourcePackage: String? = "com.example.testapp",
        mockBitmap: Bitmap? = null
    ) {
        val bitmap = mockBitmap ?: try {
            Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        } catch (_: Throwable) {
            // In unit tests where android.graphics.Bitmap is mocked or not loaded
            null
        }

        simulatedResult = Result.success(
            ScreenCaptureResult(
                captureId = UUID.randomUUID().toString(),
                bitmap = bitmap ?: Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888),
                width = width,
                height = height,
                timestampMs = System.currentTimeMillis(),
                sourcePackage = sourcePackage
            )
        )
    }

    /**
     * Configures the fake to return a structured [CaptureError] failure.
     */
    fun enqueueFailure(error: CaptureError) {
        simulatedResult = Result.failure(CaptureException(error))
    }

    override suspend fun captureCurrentScreen(): Result<ScreenCaptureResult> {
        captureInvocationCount++
        return simulatedResult ?: Result.failure(
            CaptureException(CaptureError.ServiceUnavailable)
        )
    }

    fun reset() {
        simulatedResult = null
        captureInvocationCount = 0
    }
}
