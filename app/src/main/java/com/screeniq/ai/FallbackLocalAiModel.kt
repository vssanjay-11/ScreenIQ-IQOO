package com.screeniq.ai

import com.screeniq.core.model.ContentCategory

/**
 * Lightweight default/fallback implementation of [LocalAiModel].
 * Ensures the MVP remains completely functional without downloading large LLM weights,
 * while allowing mock/test injection or seamless swapping when on-device weights are added.
 */
class FallbackLocalAiModel(
    override val isAvailable: Boolean = false,
    private val mockResponses: Map<String, LocalModelInferenceResult> = emptyMap()
) : LocalAiModel {

    override suspend fun inferClassification(
        rawText: String,
        candidateCategories: List<ContentCategory>
    ): LocalModelInferenceResult? {
        if (!isAvailable) return null

        // Check mock responses for deterministic testing if configured
        for ((trigger, response) in mockResponses) {
            if (rawText.contains(trigger, ignoreCase = true)) {
                return response
            }
        }

        return null
    }
}
