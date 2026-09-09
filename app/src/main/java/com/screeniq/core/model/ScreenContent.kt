package com.screeniq.core.model

/**
 * Lightweight rectangular bounding box abstraction representing screen coordinates.
 * Compatible with Android Rect without requiring android.graphics in JVM unit tests.
 */
data class ScreenRect(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
) {
    val width: Int get() = right - left
    val height: Int get() = bottom - top
}

/**
 * Structured textual and spatial data extracted from the screen frame by Agent 4 (OCR).
 */
data class ScreenContent(
    val captureId: String,
    val rawFullText: String,
    val textBlocks: List<TextBlock> = emptyList(),
    val detectedEntities: List<DetectedEntity> = emptyList(),
    val extractionDurationMs: Long = 0L
)

/**
 * Discrete block of text with spatial coordinates and OCR confidence.
 */
data class TextBlock(
    val text: String,
    val boundingBox: ScreenRect? = null,
    val confidence: Float = 1.0f
)

/**
 * Entity detected during OCR or deterministic extraction.
 */
data class DetectedEntity(
    val type: EntityType,
    val rawValue: String,
    val normalizedValue: String? = null,
    val boundingBox: ScreenRect? = null,
    val confidence: Float = 1.0f,
    val metadata: Map<String, Any> = emptyMap()
)

fun android.graphics.Rect.toScreenRect(): ScreenRect = ScreenRect(left, top, right, bottom)
fun ScreenRect.toAndroidRect(): android.graphics.Rect = android.graphics.Rect(left, top, right, bottom)

/**
 * Fine-grained entity types recognized on screen.
 */
enum class EntityType {
    DATE_TIME,
    LOCATION_ADDRESS,
    PHONE_NUMBER,
    EMAIL,
    URL,
    PRODUCT_INFO,
    TASK_TODO,
    DOCUMENT_SNIPPET,
    QR_BARCODE,
    NUMERIC_FINANCIAL,
    CONTACT_NAME,
    UNKNOWN
}
