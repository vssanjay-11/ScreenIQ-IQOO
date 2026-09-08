package com.screeniq.settings.model

/**
 * Governs when ScreenIQ prompts the user before executing an action.
 */
enum class ConfirmationBehavior(
    val displayName: String,
    val description: String
) {
    /**
     * Shows confirmation popup for all actions regardless of safety level.
     */
    ALWAYS_ASK(
        displayName = "Always Ask",
        description = "Show confirmation dialog before executing any action."
    ),

    /**
     * Auto-suggests safe viewing actions (Maps, Browser links); requires explicit
     * user tap for calendar events, messaging, calls, or task creation.
     */
    CONFIRM_SENSITIVE_ONLY(
        displayName = "Confirm Sensitive Only",
        description = "Require confirmation for calendar, messaging, and calls; auto-prepare safe previews."
    ),

    /**
     * Executes safe read-only actions automatically when confidence is high;
     * confirms only dangerous operations (outbound calls, payments).
     */
    AUTO_EXECUTE_SAFE(
        displayName = "Auto-Execute Safe Actions",
        description = "Automatically trigger safe actions (e.g., open maps for addresses) at >90% confidence."
    )
}
