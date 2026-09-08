package com.screeniq.history.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Privacy-compliant local record of actions executed by ScreenIQ.
 *
 * Stored fields strictly adhere to the ScreenIQ specification:
 * - timestamp (e.g., 09:42)
 * - content type (EVENT, LOCATION, COMMUNICATION, etc.)
 * - summary (e.g., "AI Workshop", "IIT Madras")
 * - action selected (e.g., "Added to Calendar", "Opened in Maps")
 * - action result (e.g., "Added to Calendar ✓" / error info)
 * - confidence (e.g., 0.98f)
 *
 * CRITICAL PRIVACY GUARANTEE:
 * Full screenshots are NEVER stored by default. [isScreenshotStored] must remain false.
 */
data class HistoryEntry(
    val id: String = UUID.randomUUID().toString(),
    val timestampMs: Long = System.currentTimeMillis(),
    val contentType: String,
    val summary: String,
    val actionSelected: String,
    val actionResult: ActionResultSummary,
    val confidence: Float, // 0.0f to 1.0f
    val isScreenshotStored: Boolean = false,
    val sourcePackage: String? = null
) {
    /**
     * Formats timestamp into short HH:mm time string (e.g., "09:42").
     */
    val formattedTime: String
        get() = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestampMs))

    /**
     * Formats timestamp into full date-time string (e.g., "2026-09-08 09:42:15").
     */
    val formattedDateTime: String
        get() = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(timestampMs))

    /**
     * Confidence formatted as integer percentage (e.g., 95%).
     */
    val confidencePercentage: Int
        get() = (confidence.coerceIn(0f, 1f) * 100).toInt()

    /**
     * Clean one-line display string suitable for lists or demo views.
     * Example: "09:42 | EVENT | AI Workshop | Added to Calendar ✓ (98%)"
     */
    val displaySummary: String
        get() = "$formattedTime | $contentType | $summary | ${actionResult.displayStatus} (${confidencePercentage}%)"
}

/**
 * Result details for a completed action.
 */
data class ActionResultSummary(
    val wasSuccessful: Boolean,
    val statusMessage: String,
    val errorMessage: String? = null
) {
    val displayStatus: String
        get() = if (wasSuccessful) "$statusMessage ✓" else "$statusMessage ✗"

    companion object {
        fun success(message: String) = ActionResultSummary(
            wasSuccessful = true,
            statusMessage = message
        )

        fun failure(message: String, error: String? = null) = ActionResultSummary(
            wasSuccessful = false,
            statusMessage = message,
            errorMessage = error
        )
    }
}
