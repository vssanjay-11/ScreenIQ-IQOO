package com.screeniq.ai

import com.screeniq.core.model.ContentCategory

/**
 * Result of on-device LLM/SLM/vision model inference.
 */
data class LocalModelInferenceResult(
    val suggestedCategory: ContentCategory,
    val confidence: Float,
    val reasoning: String,
    val structuredFields: Map<String, String> = emptyMap()
)

/**
 * Interface for optional on-device AI inference (e.g. MediaPipe GenAI, Gemma 2B, ONNX, llama.cpp).
 * Decouples the classification pipeline from any single local model engine.
 */
interface LocalAiModel {
    /**
     * Whether the local model weights and runtime are loaded and ready for inference.
     */
    val isAvailable: Boolean

    /**
     * Runs prompt-based or embedding-based semantic classification over the extracted text.
     */
    suspend fun inferClassification(
        rawText: String,
        candidateCategories: List<ContentCategory>
    ): LocalModelInferenceResult?
}
