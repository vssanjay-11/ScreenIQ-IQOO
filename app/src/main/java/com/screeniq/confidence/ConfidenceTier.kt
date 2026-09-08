package com.screeniq.confidence

/**
 * Confidence tiers governing presentation and execution behavior.
 */
enum class ConfidenceTier {
    /**
     * High Confidence (Score >= 0.85):
     * May create a safe draft or propose automatic execution (strictly for SAFE_AUTO actions).
     */
    HIGH,

    /**
     * Medium Confidence (0.50 <= Score < 0.85):
     * Requires explicit user confirmation before any action is executed.
     */
    MEDIUM,

    /**
     * Low Confidence (Score < 0.50):
     * Fallback to asking the user what they want or offering basic clipboard copy.
     */
    LOW;

    companion object {
        const val HIGH_THRESHOLD = 0.85f
        const val LOW_THRESHOLD = 0.50f

        fun fromScore(score: Float): ConfidenceTier {
            return when {
                score >= HIGH_THRESHOLD -> HIGH
                score >= LOW_THRESHOLD -> MEDIUM
                else -> LOW
            }
        }
    }
}
