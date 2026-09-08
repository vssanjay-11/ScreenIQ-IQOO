package com.screeniq.capture

import android.graphics.Bitmap
import com.screeniq.capture.accessibility.AccessibilityScreenshotProvider
import com.screeniq.capture.error.CaptureError
import com.screeniq.capture.error.CaptureException
import com.screeniq.capture.memory.CaptureMemoryManager
import com.screeniq.core.contracts.ScreenCaptureEngine
import com.screeniq.core.model.ScreenCaptureResult

/**
 * Primary implementation of [ScreenCaptureEngine] for ScreenIQ.
 *
 * Orchestrates in-memory screen capture across available system providers:
 * 1. Primary: [AccessibilityScreenshotProvider] (API 30+ accessibility screenshot)
 * 2. Fallback: Configurable fallback provider (e.g. MediaProjection or mock provider)
 *
 * Architecture & Privacy Guarantees:
 * - Strictly in-memory: No disk IO or permanent cache.
 * - Graphic buffer safety: Hardware buffers are released immediately upon capture.
 * - Graceful failure: Converts all Android system errors into structured [CaptureError]
 *   within Kotlin's [Result.failure].
 */
class ScreenCaptureManager(
    private val accessibilityProvider: AccessibilityScreenshotProvider? = null,
    private val fallbackProvider: (suspend () -> Result<ScreenCaptureResult>)? = null,
    private val maxDimensionPx: Int? = null // Optional downscale threshold to prevent OOM
) : ScreenCaptureEngine {

    override suspend fun captureCurrentScreen(): Result<ScreenCaptureResult> {
        // Attempt capture via AccessibilityService screenshot first
        val primaryResult = accessibilityProvider?.capture()

        if (primaryResult != null && primaryResult.isSuccess) {
            return processAndScaleResult(primaryResult.getOrThrow())
        }

        // If primary provider failed or was unavailable, try fallback provider if registered
        if (fallbackProvider != null) {
            val fallbackResult = try {
                fallbackProvider.invoke()
            } catch (e: Throwable) {
                Result.failure(
                    CaptureException(
                        CaptureError.InternalError("Fallback provider failed", e)
                    )
                )
            }

            if (fallbackResult.isSuccess) {
                return processAndScaleResult(fallbackResult.getOrThrow())
            }

            // Return the fallback failure if it failed
            return fallbackResult
        }

        // If no fallback exists, return primary failure or ServiceUnavailable error
        return primaryResult ?: Result.failure(
            CaptureException(CaptureError.ServiceUnavailable)
        )
    }

    /**
     * Optional image scaling to prevent Out-Of-Memory (OOM) errors on ultra-high-resolution
     * displays (e.g. 2K/4K) before passing downstream to OCR / ML Kit.
     */
    private fun processAndScaleResult(result: ScreenCaptureResult): Result<ScreenCaptureResult> {
        val maxDim = maxDimensionPx
        if (maxDim == null || (result.width <= maxDim && result.height <= maxDim)) {
            return Result.success(result)
        }

        return try {
            val originalBitmap = result.bitmap
            val scaleFactor = maxDim.toFloat() / maxOf(result.width, result.height).toFloat()
            val targetWidth = (result.width * scaleFactor).toInt()
            val targetHeight = (result.height * scaleFactor).toInt()

            val scaledBitmap = Bitmap.createScaledBitmap(
                originalBitmap,
                targetWidth,
                targetHeight,
                true
            )

            // Safely recycle original unscaled bitmap if a new scaled instance was produced
            if (scaledBitmap !== originalBitmap) {
                CaptureMemoryManager.safeRecycle(originalBitmap)
            }

            val scaledResult = result.copy(
                bitmap = scaledBitmap,
                width = targetWidth,
                height = targetHeight
            )
            Result.success(scaledResult)
        } catch (e: Throwable) {
            // If scaling fails (e.g. transient OOM), return original unscaled result rather than failing
            Result.success(result)
        }
    }
}
