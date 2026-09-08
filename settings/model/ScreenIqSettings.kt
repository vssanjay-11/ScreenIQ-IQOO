package com.screeniq.settings.model

/**
 * Privacy controls and safety toggles for ScreenIQ.
 */
data class PrivacyControls(
    val allowScreenProcessing: Boolean = true,
    val storeScreenshots: Boolean = false, // Must be FALSE by default
    val maskSensitiveInput: Boolean = true, // Hide OTPs, passwords, card numbers
    val allowDiagnosticsTelemetry: Boolean = false // No external cloud telemetry
)

/**
 * Complete user settings for ScreenIQ.
 * All settings are stored locally; no cloud/account required.
 */
data class ScreenIqSettings(
    val isScreenIqEnabled: Boolean = true,
    val isGestureEnabled: Boolean = true,
    val confirmationBehavior: ConfirmationBehavior = ConfirmationBehavior.CONFIRM_SENSITIVE_ONLY,
    val privacy: PrivacyControls = PrivacyControls(),
    val historyRetention: HistoryRetention = HistoryRetention.RETENTION_7_DAYS,
    val preferLocalAi: Boolean = true
) {
    companion object {
        val DEFAULT = ScreenIqSettings()
    }
}
