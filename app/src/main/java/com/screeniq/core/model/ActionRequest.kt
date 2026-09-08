package com.screeniq.core.model

/**
 * Approved command sent to the Android Execution Subsystem.
 */
data class ActionRequest(
    val actionId: String,
    val type: ActionType,
    val parameters: Map<String, String>,
    val confirmedByUser: Boolean,
    val timestampMs: Long = System.currentTimeMillis()
)

/**
 * Result emitted following native intent execution.
 */
data class ActionResult(
    val actionId: String,
    val success: Boolean,
    val executedIntentSummary: String,
    val errorMessage: String? = null,
    val timestampMs: Long = System.currentTimeMillis()
)
