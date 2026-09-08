package com.screeniq.planner

import com.screeniq.confidence.ConfidenceEvaluator
import com.screeniq.confidence.SafetyPolicy
import com.screeniq.core.model.ActionConfidence
import com.screeniq.core.model.ActionSafetyLevel
import com.screeniq.core.model.ActionSuggestion
import com.screeniq.core.model.ActionType
import java.util.UUID

/**
 * Fluent builder for ActionSuggestion objects, enforcing safety policies,
 * confidence scoring, and parameter contract validation.
 */
class ActionProposalBuilder(
    private val actionType: ActionType,
    private val baseScore: Float
) {
    private var actionId: String = "action_${UUID.randomUUID().toString().take(8)}"
    private var title: String = ""
    private var description: String = ""
    private val payload = mutableMapOf<String, String>()
    private var isPrimary: Boolean = false
    private val requiredKeys = mutableListOf<String>()
    private val optionalKeys = mutableListOf<String>()

    fun actionId(id: String) = apply { this.actionId = id }
    fun title(title: String) = apply { this.title = title }
    fun description(description: String) = apply { this.description = description }
    fun isPrimary(isPrimary: Boolean) = apply { this.isPrimary = isPrimary }

    fun addParam(key: String, value: String?) = apply {
        if (!value.isNullOrBlank()) {
            this.payload[key] = value.trim()
        }
    }

    fun addParams(params: Map<String, String?>) = apply {
        params.forEach { (k, v) -> addParam(k, v) }
    }

    fun requireKeys(vararg keys: String) = apply {
        this.requiredKeys.addAll(keys)
    }

    fun optionalKeys(vararg keys: String) = apply {
        this.optionalKeys.addAll(keys)
    }

    fun build(): ActionSuggestion {
        // 1. Evaluate calibrated action confidence
        val actionConfidence: ActionConfidence = ConfidenceEvaluator.evaluateActionConfidence(
            actionType = actionType,
            baseClassificationScore = baseScore,
            payload = payload,
            requiredParamKeys = requiredKeys,
            optionalParamKeys = optionalKeys
        )

        // 2. Evaluate safety policy (strictly enforces auto-action prohibitions)
        val safetyEvaluation = SafetyPolicy.evaluateSafety(
            actionType = actionType,
            confidenceScore = actionConfidence.score,
            payload = payload
        )

        return ActionSuggestion(
            actionId = actionId,
            type = actionType,
            title = title.ifBlank { actionType.name.replace('_', ' ') },
            description = description.ifBlank { safetyEvaluation.policyReason },
            payload = payload.toMap(),
            confidence = actionConfidence,
            safetyLevel = safetyEvaluation.safetyLevel,
            isPrimary = isPrimary,
            requiresConfirmation = safetyEvaluation.requiresConfirmation
        )
    }
}
