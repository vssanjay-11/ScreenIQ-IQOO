package com.screeniq.core.common

/**
 * Shared system constants and configuration keys.
 */
object ScreenIqConstants {
    const val APP_NAME = "ScreenIQ"
    const val PACKAGE_NAME = "com.screeniq"
    
    // Performance budgets
    const val MAX_PIPELINE_LATENCY_MS = 1500L
    const val SCREEN_CAPTURE_TIMEOUT_MS = 4000L
    
    // Confidence thresholds
    const val HIGH_CONFIDENCE_THRESHOLD = 0.75f
    const val MEDIUM_CONFIDENCE_THRESHOLD = 0.45f
    
    // Gesture defaults
    const val REQUIRED_POINTER_COUNT = 4
}
