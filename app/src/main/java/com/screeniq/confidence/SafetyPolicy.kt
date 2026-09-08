package com.screeniq.confidence

import com.screeniq.core.model.ActionSafetyLevel
import com.screeniq.core.model.ActionType

/**
 * Result of a safety policy evaluation for an action proposal.
 */
data class SafetyEvaluation(
    val safetyLevel: ActionSafetyLevel,
    val requiresConfirmation: Boolean,
    val isRisky: Boolean,
    val policyReason: String,
    val isForbiddenFromAutoExecution: Boolean
)

/**
 * Enforces ScreenIQ's core safety and autonomy policies.
 *
 * Core Invariant:
 * The AI must NEVER automatically execute risky actions:
 * - Purchases or checkout
 * - Money transfers / payment flows
 * - Sending messages or emails (drafts are permitted, outbound sending is not)
 * - Deleting or overwriting data
 * - Publishing content to social or web platforms
 */
object SafetyPolicy {

    /**
     * Set of actions that are intrinsically classified as DANGEROUS or sensitive.
     * These MUST NEVER run automatically regardless of confidence score.
     */
    private val INTRINSICALLY_RISKY_ACTIONS = setOf(
        ActionType.DIAL_PHONE,
        ActionType.SEND_SMS
    )

    /**
     * Actions that alter personal information or schedule (calendar, tasks, contacts).
     * These always require confirmation to prevent unwanted personal data mutations.
     */
    private val DATA_MUTATION_ACTIONS = setOf(
        ActionType.ADD_TO_CALENDAR,
        ActionType.CREATE_TASK,
        ActionType.SET_REMINDER,
        ActionType.SAVE_CONTACT,
        ActionType.COMPOSE_EMAIL // Compose opens draft composer; does not auto-send
    )

    /**
     * Keys in payloads that indicate risky financial or transactional operations.
     */
    private val FINANCIAL_OR_TRANSACTION_PAYLOAD_KEYS = setOf(
        "is_payment",
        "payment_upi",
        "amount",
        "upi_id",
        "checkout_url",
        "purchase",
        "transfer_money"
    )

    /**
     * Keys in payloads indicating content deletion or publishing.
     */
    private val DELETION_OR_PUBLISH_PAYLOAD_KEYS = setOf(
        "delete_data",
        "remove_item",
        "publish_content",
        "send_message_direct"
    )

    /**
     * Evaluates the safety level, confirmation requirement, and autonomy policy for an action.
     */
    fun evaluateSafety(
        actionType: ActionType,
        confidenceScore: Float,
        payload: Map<String, String> = emptyMap()
    ): SafetyEvaluation {
        val tier = ConfidenceTier.fromScore(confidenceScore)

        // 1. Check for strictly forbidden automatic operations
        val hasFinancialKeywords = payload.keys.any { it.lowercase() in FINANCIAL_OR_TRANSACTION_PAYLOAD_KEYS } ||
                payload.values.any { it.lowercase().contains("upi://pay") || it.lowercase().contains("paytm") }

        val hasDeletionOrPublishKeywords = payload.keys.any { it.lowercase() in DELETION_OR_PUBLISH_PAYLOAD_KEYS }

        val isDirectMessageSending = actionType == ActionType.SEND_SMS &&
                (payload["auto_send"]?.toBoolean() == true || payload["direct"] == "true")

        if (hasFinancialKeywords || hasDeletionOrPublishKeywords || isDirectMessageSending) {
            return SafetyEvaluation(
                safetyLevel = ActionSafetyLevel.DANGEROUS,
                requiresConfirmation = true,
                isRisky = true,
                policyReason = "Blocked by strict safety policy: Financial transactions, message sending, data deletion, or publishing must never be automated.",
                isForbiddenFromAutoExecution = true
            )
        }

        // 2. Check for intrinsically risky actions (calls, sms)
        if (actionType in INTRINSICALLY_RISKY_ACTIONS) {
            return SafetyEvaluation(
                safetyLevel = ActionSafetyLevel.DANGEROUS,
                requiresConfirmation = true,
                isRisky = true,
                policyReason = "Outbound communication requires explicit user confirmation.",
                isForbiddenFromAutoExecution = true
            )
        }

        // 3. Check for data mutation actions (calendar, tasks, contacts, email draft)
        if (actionType in DATA_MUTATION_ACTIONS) {
            return SafetyEvaluation(
                safetyLevel = ActionSafetyLevel.REQUIRE_CONFIRMATION,
                requiresConfirmation = true,
                isRisky = false,
                policyReason = "Action modifies personal data or drafts; requires user confirmation.",
                isForbiddenFromAutoExecution = true
            )
        }

        // 4. For QR inspection: if QR payload contains payment/financial data, classify as DANGEROUS
        if (actionType == ActionType.INSPECT_QR) {
            val qrContent = payload["qr_content"] ?: payload["raw_data"] ?: ""
            if (qrContent.startsWith("upi://") || qrContent.contains("paytm", ignoreCase = true)) {
                return SafetyEvaluation(
                    safetyLevel = ActionSafetyLevel.DANGEROUS,
                    requiresConfirmation = true,
                    isRisky = true,
                    policyReason = "QR code contains financial/payment request. User confirmation mandatory.",
                    isForbiddenFromAutoExecution = true
                )
            }
        }

        // 5. Read-only / Safe actions (Maps, Browser, Clipboard, Document Summary, Ask User)
        // High confidence read-only actions can be SAFE_AUTO
        return when (tier) {
            ConfidenceTier.HIGH -> {
                SafetyEvaluation(
                    safetyLevel = ActionSafetyLevel.SAFE_AUTO,
                    requiresConfirmation = false,
                    isRisky = false,
                    policyReason = "Safe read-only action with high confidence; eligible for automatic proposal or direct presentation.",
                    isForbiddenFromAutoExecution = false
                )
            }
            ConfidenceTier.MEDIUM -> {
                SafetyEvaluation(
                    safetyLevel = ActionSafetyLevel.REQUIRE_CONFIRMATION,
                    requiresConfirmation = true,
                    isRisky = false,
                    policyReason = "Medium confidence requires user confirmation before proceeding.",
                    isForbiddenFromAutoExecution = false
                )
            }
            ConfidenceTier.LOW -> {
                SafetyEvaluation(
                    safetyLevel = ActionSafetyLevel.REQUIRE_CONFIRMATION,
                    requiresConfirmation = true,
                    isRisky = false,
                    policyReason = "Low confidence requires prompting the user for intent.",
                    isForbiddenFromAutoExecution = true
                )
            }
        }
    }
}
