package com.screeniq.capture.accessibility

import android.accessibilityservice.AccessibilityService
import android.graphics.Bitmap
import android.os.Build
import android.view.Display
import com.screeniq.capture.error.CaptureError
import com.screeniq.capture.error.CaptureException
import com.screeniq.core.model.ScreenCaptureResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import java.util.UUID
import kotlin.coroutines.resume

/**
 * Screen capture provider utilizing Android 11+ (API 30+) [AccessibilityService.takeScreenshot].
 *
 * Key guarantees:
 * - In-memory only: captures directly to transient memory buffer.
 * - Hardware buffer lifecycle: wrapped bitmap copied to software ARGB_8888 and
 *   native HardwareBuffer closed immediately to prevent graphic memory leaks.
 * - Structured failure handling: maps Android error codes to [CaptureError].
 */
class AccessibilityScreenshotProvider(
    private val bridge: AccessibilityCaptureBridge,
    private val defaultDisplayId: Int = Display.DEFAULT_DISPLAY,
    private val timeoutMs: Long = 4000L
) {

    /**
     * Executes in-memory screenshot capture using the active Accessibility bridge.
     */
    suspend fun capture(): Result<ScreenCaptureResult> {
        // 1. Verify OS version capability (Android 11 / API 30+)
        if (Build.VERSION.SDK_INT in 1 until Build.VERSION_CODES.R) {
            return Result.failure(
                CaptureException(
                    CaptureError.UnsupportedEnvironment(
                        "AccessibilityService.takeScreenshot requires Android 11+ (API 30+). Current API: ${Build.VERSION.SDK_INT}"
                    )
                )
            )
        }

        // 2. Verify service availability
        if (!bridge.isServiceConnected) {
            return Result.failure(
                CaptureException(CaptureError.ServiceUnavailable)
            )
        }

        return try {
            withTimeout(timeoutMs) {
                suspendCancellableCoroutine { continuation ->
                    val executor = Dispatchers.Default.asExecutor()

                    val callback = object : AccessibilityService.TakeScreenshotCallback {
                        override fun onSuccess(screenshotResult: AccessibilityService.ScreenshotResult) {
                            try {
                                val hwBuffer = screenshotResult.hardwareBuffer
                                val colorSpace = screenshotResult.colorSpace
                                val timestamp = screenshotResult.timestamp

                                // Wrap the HardwareBuffer into a Bitmap
                                val hwBitmap = Bitmap.wrapHardwareBuffer(hwBuffer, colorSpace)
                                if (hwBitmap == null) {
                                    hwBuffer.close()
                                    continuation.resume(
                                        Result.failure(
                                            CaptureException(
                                                CaptureError.InternalError("Failed to wrap HardwareBuffer into Bitmap")
                                            )
                                        )
                                    )
                                    return
                                }

                                // Create a software-accessible copy (ARGB_8888) so downstream analyzers (e.g. ML Kit)
                                // can inspect pixels directly without HardwareBitmap restrictions
                                val softwareBitmap = try {
                                    hwBitmap.copy(Bitmap.Config.ARGB_8888, false)
                                } finally {
                                    // Always close the native HardwareBuffer and recycle the hardware wrapper
                                    hwBuffer.close()
                                    hwBitmap.recycle()
                                }

                                if (softwareBitmap == null) {
                                    continuation.resume(
                                        Result.failure(
                                            CaptureException(
                                                CaptureError.InternalError("Failed to copy hardware bitmap to ARGB_8888")
                                            )
                                        )
                                    )
                                    return
                                }

                                val result = ScreenCaptureResult(
                                    captureId = UUID.randomUUID().toString(),
                                    bitmap = softwareBitmap,
                                    width = softwareBitmap.width,
                                    height = softwareBitmap.height,
                                    timestampMs = if (timestamp > 0) timestamp else System.currentTimeMillis(),
                                    sourcePackage = bridge.currentForegroundPackage
                                )

                                continuation.resume(Result.success(result))
                            } catch (e: Throwable) {
                                continuation.resume(
                                    Result.failure(
                                        CaptureException(
                                            CaptureError.InternalError("Exception processing screenshot buffer", e)
                                        )
                                    )
                                )
                            }
                        }

                        override fun onFailure(errorCode: Int) {
                            val error = mapErrorCode(errorCode, defaultDisplayId)
                            continuation.resume(Result.failure(CaptureException(error)))
                        }
                    }

                    try {
                        bridge.takeScreenshot(defaultDisplayId, executor, callback)
                    } catch (e: Throwable) {
                        continuation.resume(
                            Result.failure(
                                CaptureException(
                                    CaptureError.InternalError("takeScreenshot dispatch failed", e)
                                )
                            )
                        )
                    }
                }
            }
        } catch (_: kotlinx.coroutines.TimeoutCancellationException) {
            Result.failure(CaptureException(CaptureError.Timeout(timeoutMs)))
        } catch (e: Throwable) {
            Result.failure(
                CaptureException(
                    CaptureError.InternalError("Unexpected capture error", e)
                )
            )
        }
    }

    private fun mapErrorCode(errorCode: Int, displayId: Int): CaptureError {
        return when (errorCode) {
            AccessibilityService.ERROR_TAKE_SCREENSHOT_INTERNAL_ERROR ->
                CaptureError.InternalError("Internal Android system error (code 1)")

            AccessibilityService.ERROR_TAKE_SCREENSHOT_NO_ACCESSIBILITY_ACCESS ->
                CaptureError.ServiceUnavailable

            3 /* ERROR_TAKE_SCREENSHOT_INTERVAL */ ->
                CaptureError.RateLimited

            AccessibilityService.ERROR_TAKE_SCREENSHOT_INVALID_DISPLAY ->
                CaptureError.InvalidDisplay(displayId)

            AccessibilityService.ERROR_TAKE_SCREENSHOT_INVALID_WINDOW ->
                CaptureError.SecureWindowDetected

            else ->
                CaptureError.InternalError("Unknown screenshot failure code: $errorCode")
        }
    }
}
