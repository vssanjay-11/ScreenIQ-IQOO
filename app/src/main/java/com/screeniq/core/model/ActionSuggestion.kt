package com.screeniq.core.model

/**
 * A concrete proposal presented to the user or prepared for execution.
 */
data class ActionSuggestion(
    val actionId: String,
    val type: ActionType,
    val title: String,
    val description: String,
    val payload: Map<String, String>, // Key-value arguments for execution (e.g. title, date, geo)
    val confidence: ActionConfidence,
    val safetyLevel: ActionSafetyLevel,
    val isPrimary: Boolean = false,
    val requiresConfirmation: Boolean = safetyLevel != ActionSafetyLevel.SAFE_AUTO
)

data class ActionConfidence(
    val score: Float, // 0.0 to 1.0
    val reason: String
) {
    val level: ConfidenceLevel get() = ConfidenceLevel.fromScore(score)
}

enum class ActionSafetyLevel {
    SAFE_AUTO,             // Non-destructive read/view (e.g. open map, view browser)
    REQUIRE_CONFIRMATION,  // Alters personal data (e.g. add calendar event, create task)
    DANGEROUS              // Sensitive (e.g. place phone call, send message, payment)
}

enum class ActionType {
    ADD_TO_CALENDAR,
    OPEN_MAPS,
    DIAL_PHONE,
    SEND_SMS,
    COMPOSE_EMAIL,
    OPEN_BROWSER,
    CREATE_TASK,
    COPY_TO_CLIPBOARD,
    SUMMARIZE_DOCUMENT,
    INSPECT_QR,
    ASK_USER_CUSTOM,
    SAVE_CONTACT,
    SEARCH_PRODUCT,
    SET_REMINDER
}
