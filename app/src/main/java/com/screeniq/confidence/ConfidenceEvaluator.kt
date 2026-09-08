package com.screeniq.confidence

import com.screeniq.core.model.ActionConfidence
import com.screeniq.core.model.ActionType

/**
 * Evaluates contextual confidence for specific proposed actions based on
 * entity completeness, OCR signal quality, and classification score.
 */
object ConfidenceEvaluator {

    /**
     * Evaluates action-level confidence, computing a calibrated score and explanation reason.
     */
    fun evaluateActionConfidence(
        actionType: ActionType,
        baseClassificationScore: Float,
        payload: Map<String, String>,
        requiredParamKeys: List<String> = emptyList(),
        optionalParamKeys: List<String> = emptyList()
    ): ActionConfidence {
        var score = baseClassificationScore.coerceIn(0.0f, 1.0f)
        val reasons = mutableListOf<String>()

        // Check required parameters
        val missingRequired = requiredParamKeys.filter { payload[it].isNullOrBlank() }
        if (missingRequired.isNotEmpty()) {
            val penalty = (missingRequired.size * 0.25f).coerceAtMost(0.50f)
            score = (score - penalty).coerceAtLeast(0.10f)
            reasons.add("Missing required parameters: ${missingRequired.joinToString(", ")}")
        } else if (requiredParamKeys.isNotEmpty()) {
            reasons.add("All essential parameters resolved (${requiredParamKeys.joinToString(", ")})")
        }

        // Check optional parameters bonus
        val presentOptional = optionalParamKeys.filter { !payload[it].isNullOrBlank() }
        if (presentOptional.isNotEmpty() && missingRequired.isEmpty()) {
            val bonus = (presentOptional.size * 0.05f).coerceAtMost(0.10f)
            score = (score + bonus).coerceAtMost(1.0f)
            reasons.add("Enriched with context (${presentOptional.joinToString(", ")})")
        }

        // Action-specific confidence rules
        when (actionType) {
            ActionType.ADD_TO_CALENDAR -> {
                val hasDate = !payload["startDate"].isNullOrBlank() || !payload["date"].isNullOrBlank()
                val hasTime = !payload["startTime"].isNullOrBlank() || !payload["time"].isNullOrBlank()
                if (!hasDate) {
                    score = score.coerceAtMost(0.60f)
                    reasons.add("No explicit date identified")
                } else if (!hasTime) {
                    reasons.add("Date identified, time unspecified (defaults to all-day)")
                }
            }
            ActionType.OPEN_MAPS -> {
                val hasAddressOrQuery = !payload["query"].isNullOrBlank() ||
                        !payload["address"].isNullOrBlank() ||
                        (!payload["latitude"].isNullOrBlank() && !payload["longitude"].isNullOrBlank())
                if (!hasAddressOrQuery) {
                    score = score.coerceAtMost(0.40f)
                    reasons.add("No address, place name, or coordinates found")
                }
            }
            ActionType.DIAL_PHONE -> {
                val phone = payload["phoneNumber"] ?: ""
                val digitsOnly = phone.filter { it.isDigit() }
                if (digitsOnly.length < 7) {
                    score = score.coerceAtMost(0.45f)
                    reasons.add("Phone number appears truncated or invalid ($phone)")
                }
            }
            ActionType.COMPOSE_EMAIL -> {
                val email = payload["recipient"] ?: ""
                if (!email.contains("@") || !email.contains(".")) {
                    score = score.coerceAtMost(0.40f)
                    reasons.add("Email address syntax invalid ($email)")
                }
            }
            ActionType.OPEN_BROWSER -> {
                val url = payload["url"] ?: ""
                if (!url.startsWith("http://") && !url.startsWith("https://") && !url.contains(".")) {
                    score = score.coerceAtMost(0.40f)
                    reasons.add("URL destination incomplete ($url)")
                }
            }
            ActionType.INSPECT_QR -> {
                val qr = payload["qr_content"] ?: ""
                if (qr.isBlank()) {
                    score = score.coerceAtMost(0.30f)
                    reasons.add("QR code payload is empty")
                }
            }
            ActionType.ASK_USER_CUSTOM -> {
                // Low confidence fallback
                score = score.coerceAtMost(0.50f)
                reasons.add("Fallback: General or low confidence screen content")
            }
            else -> {
                // Keep calculated score
            }
        }

        val finalScore = (Math.round(score * 100.0f) / 100.0f).coerceIn(0.0f, 1.0f)
        val finalReason = if (reasons.isNotEmpty()) {
            reasons.joinToString("; ")
        } else {
            "Classification confidence: ${(finalScore * 100).toInt()}%"
        }

        return ActionConfidence(
            score = finalScore,
            reason = finalReason
        )
    }
}
