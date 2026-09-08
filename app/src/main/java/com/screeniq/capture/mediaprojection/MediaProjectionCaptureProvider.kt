package com.screeniq.capture.mediaprojection

import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import com.screeniq.capture.error.CaptureError
import com.screeniq.capture.error.CaptureException
import com.screeniq.core.model.ScreenCaptureResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.nio.ByteBuffer
import java.util.UUID
import kotlin.coroutines.resume

/**
 * Screen capture provider utilizing Android's [MediaProjection] API.
 * Serves as a complementary/fallback capture provider when AccessibilityService
 * screenshot is unavailable or when operating in standalone mode.
 *
 * Guarantees:
 * - Direct in-memory buffer read via [ImageReader].
 * - Immediate [Image.close] and [VirtualDisplay.release] to prevent buffer leaks.
 * - Software [Bitmap.Config.ARGB_8888] ready for downstream OCR and analysis.
 */
class MediaProjectionCaptureProvider(
    private val mediaProjection: MediaProjection,
    private val screenWidth: Int,
    private val screenHeight: Int,
    private val screenDensityDpi: Int,
    private val timeoutMs: Long = 4000L
) {

    suspend fun capture(): Result<ScreenCaptureResult> = withContext(Dispatchers.Default) {
        if (screenWidth <= 0 || screenHeight <= 0) {
            return@withContext Result.failure(
                CaptureException(
                    CaptureError.InternalError("Invalid screen dimensions: ${screenWidth}x${screenHeight}")
                )
            )
        }

        var imageReader: ImageReader? = null
        var virtualDisplay: VirtualDisplay? = null

        try {
            withTimeout(timeoutMs) {
                suspendCancellableCoroutine { continuation ->
                    imageReader = ImageReader.newInstance(
                        screenWidth,
                        screenHeight,
                        PixelFormat.RGBA_8888,
                        2
                    )

                    virtualDisplay = mediaProjection.createVirtualDisplay(
                        "ScreenIQ_Capture",
                        screenWidth,
                        screenHeight,
                        screenDensityDpi,
                        DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                        imageReader!!.surface,
                        null,
                        null
                    )

                    imageReader!!.setOnImageAvailableListener({ reader ->
                        var image: Image? = null
                        try {
                            image = reader.acquireLatestImage()
                            if (image != null && continuation.isActive) {
                                val bitmap = convertImageToBitmap(image, screenWidth, screenHeight)
                                val result = ScreenCaptureResult(
                                    captureId = UUID.randomUUID().toString(),
                                    bitmap = bitmap,
                                    width = screenWidth,
                                    height = screenHeight,
                                    timestampMs = System.currentTimeMillis()
                                )
                                continuation.resume(Result.success(result))
                            }
                        } catch (e: Throwable) {
                            if (continuation.isActive) {
                                continuation.resume(
                                    Result.failure(
                                        CaptureException(
                                            CaptureError.InternalError("Failed to convert image buffer to Bitmap", e)
                                        )
                                    )
                                )
                            }
                        } finally {
                            image?.close()
                        }
                    }, null)
                }
            }
        } catch (_: kotlinx.coroutines.TimeoutCancellationException) {
            Result.failure(CaptureException(CaptureError.Timeout(timeoutMs)))
        } catch (e: Throwable) {
            Result.failure(
                CaptureException(
                    CaptureError.InternalError("MediaProjection capture failure", e)
                )
            )
        } finally {
            try {
                virtualDisplay?.release()
                imageReader?.close()
            } catch (_: Throwable) {
                // Ignore cleanup exceptions
            }
        }
    }

    private fun convertImageToBitmap(image: Image, width: Int, height: Int): Bitmap {
        val plane = image.planes[0]
        val buffer: ByteBuffer = plane.buffer
        val pixelStride = plane.pixelStride
        val rowStride = plane.rowStride
        val rowPadding = rowStride - pixelStride * width

        val bitmap = Bitmap.createBitmap(
            width + rowPadding / pixelStride,
            height,
            Bitmap.Config.ARGB_8888
        )
        bitmap.copyPixelsFromBuffer(buffer)

        // If row padding caused the bitmap to be wider than the target display, crop it
        return if (rowPadding == 0) {
            bitmap
        } else {
            val cropped = Bitmap.createBitmap(bitmap, 0, 0, width, height)
            bitmap.recycle()
            cropped
        }
    }
}
