package com.screeniq.ocr.engine

import android.graphics.Bitmap
import com.screeniq.core.model.TextBlock

/**
 * Pluggable visual text recognizer.
 * Supports production ML Kit Android Vision implementation or in-memory test recognizer.
 */
interface TextRecognizerDelegate {
    suspend fun recognize(bitmap: Bitmap?): List<TextBlock>
}
