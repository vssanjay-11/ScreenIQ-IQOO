package com.screeniq.confidence

import com.screeniq.core.model.ActionSafetyLevel
import com.screeniq.core.model.ActionType

/**
 * Validates ScreenIQ's safety policy invariants:
 * 1. AI must NEVER automatically purchase, transfer money, send messages, delete data, or publish content.
 * 2. Risky actions (calls, SMS) must always require explicit confirmation.
 * 3. High confidence read-only actions may be SAFE_AUTO.
 * 4. Medium / Low confidence actions require confirmation.
 */
class SafetyPolicyTest {

    fun testForbiddenAutoOperations() {
        // 1. Purchasing / Checkout
        val purchaseEval = SafetyPolicy.evaluateSafety(
            actionType = ActionType.OPEN_BROWSER,
            confidenceScore = 0.99f,
            payload = mapOf("purchase" to "true", "checkout_url" to "https://store.example/pay")
        )
        assert(purchaseEval.isRisky) { "Purchasing must be marked risky" }
        assert(purchaseEval.requiresConfirmation) { "Purchasing must strictly require confirmation" }
        assert(purchaseEval.safetyLevel == ActionSafetyLevel.DANGEROUS) { "Purchasing must be DANGEROUS" }

        // 2. Money transfer / UPI
        val upiEval = SafetyPolicy.evaluateSafety(
            actionType = ActionType.INSPECT_QR,
            confidenceScore = 1.0f,
            payload = mapOf("qr_content" to "upi://pay?pa=merchant@upi&pn=Store&am=500")
        )
        assert(upiEval.isRisky) { "Money transfer must be marked risky" }
        assert(upiEval.requiresConfirmation) { "Money transfer must require confirmation" }
        assert(upiEval.safetyLevel == ActionSafetyLevel.DANGEROUS) { "Payment must be DANGEROUS" }

        // 3. Direct message sending
        val smsEval = SafetyPolicy.evaluateSafety(
            actionType = ActionType.SEND_SMS,
            confidenceScore = 0.95f,
            payload = mapOf("phoneNumber" to "+15551234567", "auto_send" to "true")
        )
        assert(smsEval.requiresConfirmation) { "Sending messages must require confirmation" }
        assert(smsEval.safetyLevel == ActionSafetyLevel.DANGEROUS) { "Sending messages must be DANGEROUS" }

        // 4. Deleting data
        val deleteEval = SafetyPolicy.evaluateSafety(
            actionType = ActionType.CREATE_TASK,
            confidenceScore = 0.90f,
            payload = mapOf("delete_data" to "true")
        )
        assert(deleteEval.requiresConfirmation) { "Data deletion must require confirmation" }
        assert(deleteEval.safetyLevel == ActionSafetyLevel.DANGEROUS) { "Deletion must be DANGEROUS" }

        // 5. Publishing content
        val publishEval = SafetyPolicy.evaluateSafety(
            actionType = ActionType.OPEN_BROWSER,
            confidenceScore = 0.90f,
            payload = mapOf("publish_content" to "true")
        )
        assert(publishEval.requiresConfirmation) { "Publishing must require confirmation" }
        assert(publishEval.safetyLevel == ActionSafetyLevel.DANGEROUS) { "Publishing must be DANGEROUS" }
    }

    fun testHighConfidenceReadOnlyIsSafeAuto() {
        val mapEval = SafetyPolicy.evaluateSafety(
            actionType = ActionType.OPEN_MAPS,
            confidenceScore = 0.95f,
            payload = mapOf("query" to "Anna University")
        )
        assert(!mapEval.requiresConfirmation) { "High confidence map view can be SAFE_AUTO" }
        assert(mapEval.safetyLevel == ActionSafetyLevel.SAFE_AUTO)
    }

    fun testDataMutationRequiresConfirmation() {
        val calendarEval = SafetyPolicy.evaluateSafety(
            actionType = ActionType.ADD_TO_CALENDAR,
            confidenceScore = 0.98f,
            payload = mapOf("title" to "AI Workshop")
        )
        assert(calendarEval.requiresConfirmation) { "Adding calendar event must require confirmation" }
        assert(calendarEval.safetyLevel == ActionSafetyLevel.REQUIRE_CONFIRMATION)
    }
}
