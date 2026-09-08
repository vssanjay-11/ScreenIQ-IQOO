package com.screeniq.privacy.test

import com.screeniq.history.model.ActionResultSummary
import com.screeniq.history.model.HistoryEntry
import com.screeniq.privacy.PrivacyGuard

/**
 * Unit test verifying privacy sanitization and safety guardrails.
 */
class PrivacyGuardTest {

    fun testSensitiveTextSanitization() {
        // Credit card masking
        val ccText = "Payment for order 4111 2222 3333 4444 completed"
        val ccSanitized = PrivacyGuard.sanitizeSummary(ccText)
        assert(!ccSanitized.contains("4111 2222 3333 4444")) { "Credit card not masked!" }
        assert(ccSanitized.contains("••••-••••-••••-4444")) { "Masked format incorrect" }

        // OTP masking
        val otpText = "Your verification OTP: 849201"
        val otpSanitized = PrivacyGuard.sanitizeSummary(otpText)
        assert(!otpSanitized.contains("849201")) { "OTP was not masked!" }
        assert(otpSanitized.contains("••••")) { "Masked OTP pattern missing" }
    }

    fun testNoScreenshotGuard() {
        val entry = HistoryEntry(
            contentType = "EVENT",
            summary = "Meeting at 10 AM",
            actionSelected = "Add to Calendar",
            actionResult = ActionResultSummary.success("Added"),
            confidence = 0.95f,
            isScreenshotStored = true // Simulating inadvertent attempt
        )

        val sanitized = PrivacyGuard.sanitizeForStorage(entry)
        assert(!sanitized.isScreenshotStored) { "PrivacyGuard failed to disable screenshot storage flag" }
    }
}
