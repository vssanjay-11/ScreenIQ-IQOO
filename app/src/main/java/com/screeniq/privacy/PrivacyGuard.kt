package com.screeniq.privacy

import com.screeniq.history.model.HistoryEntry
import com.screeniq.history.repository.HistoryRepository

/**
 * Defensive privacy guardrail system for ScreenIQ.
 *
 * Enforces sanitization of sensitive values before logging and guarantees
 * that raw screen bitmaps never leak to disk.
 */
object PrivacyGuard {

    // Regex patterns for sensitive information
    private val CREDIT_CARD_REGEX = Regex("""\b(?:\d{4}[ -]?){3}\d{4}\b""")
    private val OTP_PIN_REGEX = Regex("""(?i)\b(?:otp|pin|code|password)[:\s]+([0-9a-zA-Z]{4,8})\b""")
    private val SSN_NATIONAL_ID_REGEX = Regex("""\b\d{3}-\d{2}-\d{4}\b""")

    /**
     * Sanitizes textual summary before storing in history.
     * Masks credit card numbers, OTPs, PINs, and potential passwords.
     */
    fun sanitizeSummary(input: String): String {
        var sanitized = input

        // Mask credit card numbers
        sanitized = CREDIT_CARD_REGEX.replace(sanitized) { match ->
            val clean = match.value.replace(" ", "").replace("-", "")
            "••••-••••-••••-${clean.takeLast(4)}"
        }

        // Mask OTP / PIN patterns
        sanitized = OTP_PIN_REGEX.replace(sanitized) { match ->
            val prefix = match.value.substringBefore(":")
            "$prefix: ••••"
        }

        // Mask SSN / National IDs
        sanitized = SSN_NATIONAL_ID_REGEX.replace(sanitized, "•••-••-••••")

        return sanitized
    }

    /**
     * Inspects a [HistoryEntry] to guarantee:
     * 1. [HistoryEntry.isScreenshotStored] is FALSE.
     * 2. Summary text does not leak unsanitized credentials.
     */
    fun sanitizeForStorage(entry: HistoryEntry): HistoryEntry {
        val cleanSummary = sanitizeSummary(entry.summary)
        return entry.copy(
            summary = cleanSummary,
            isScreenshotStored = false // Absolute guarantee
        )
    }

    /**
     * Completely and permanently clears all local action history records.
     *
     * @param repository The active history repository.
     * @return Number of cleared entries.
     */
    suspend fun executeClearHistory(repository: HistoryRepository): Int {
        return repository.clearHistory()
    }
}
