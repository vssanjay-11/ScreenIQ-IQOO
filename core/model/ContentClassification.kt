package com.screeniq.core.model

/**
 * Semantic interpretation of what the user is looking at and likely intents.
 * Produced by Agent 5 (Classifier), consumed by Agent 6 (Planner).
 */
data class ContentClassification(
    val captureId: String,
    val primaryCategory: ContentCategory,
    val secondaryCategories: List<ContentCategory> = emptyList(),
    val confidenceScore: Float, // 0.0f to 1.0f
    val extractedEntities: List<DetectedEntity> = emptyList(),
    val structuredFields: Map<String, String> = emptyMap(),
    val summary: String? = null,
    val reasoningSummary: String? = null
) {
    // Convenience properties matching Agent 5 specification
    val type: ContentCategory get() = primaryCategory
    val confidence: Float get() = confidenceScore
    val confidenceLevel: ConfidenceLevel get() = ConfidenceLevel.fromScore(confidenceScore)
}

/**
 * Supported content categories / content types recognized by ScreenIQ.
 */
enum class ContentCategory {
    EVENT,
    PRODUCT,
    LOCATION,
    PHONE,
    EMAIL,
    URL,
    CONTACT,
    TASK,
    DOCUMENT,
    QR_CODE,
    IMAGE,
    TEXT,
    UNKNOWN
}

/**
 * ContentType alias for interoperability across modules.
 */
typealias ContentType = ContentCategory
