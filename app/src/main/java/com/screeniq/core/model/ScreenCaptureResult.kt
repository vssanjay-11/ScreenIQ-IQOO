package com.screeniq.core.model

import android.graphics.Bitmap

/**
 * Encapsulates the in-memory visual frame captured from the display.
 * Strict privacy rule: Bitmap must be processed in-memory and recycled after extraction.
 */
data class ScreenCaptureResult(
    val captureId: String,
    val bitmap: Bitmap? = null,
    val width: Int,
    val height: Int,
    val timestampMs: Long,
    val sourcePackage: String? = null
)
