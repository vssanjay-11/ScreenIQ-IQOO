package com.screeniq.settings.model

/**
 * Defines how long local action history records are kept on-device.
 */
enum class HistoryRetention(
    val displayName: String,
    val durationMs: Long
) {
    RETENTION_24_HOURS(
        displayName = "24 Hours",
        durationMs = 24 * 60 * 60 * 1000L
    ),

    RETENTION_7_DAYS(
        displayName = "7 Days",
        durationMs = 7 * 24 * 60 * 60 * 1000L
    ),

    RETENTION_30_DAYS(
        displayName = "30 Days",
        durationMs = 30 * 24 * 60 * 60 * 1000L
    ),

    INDEFINITE(
        displayName = "Keep Indefinitely",
        durationMs = -1L
    );

    val isAutoPurgeEnabled: Boolean
        get() = durationMs > 0

    fun calculateCutoffMs(currentTimeMs: Long = System.currentTimeMillis()): Long? {
        return if (isAutoPurgeEnabled) currentTimeMs - durationMs else null
    }
}
