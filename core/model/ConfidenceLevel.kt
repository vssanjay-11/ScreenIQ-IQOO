package com.screeniq.core.model

/**
 * Coarse-grained confidence classification for intent understanding and action planning.
 */
enum class ConfidenceLevel {
    HIGH,
    MEDIUM,
    LOW,
    UNKNOWN;

    companion object {
        fun fromScore(score: Float): ConfidenceLevel = when {
            score >= 0.75f -> HIGH
            score >= 0.45f -> MEDIUM
            score > 0.0f -> LOW
            else -> UNKNOWN
        }
    }
}
