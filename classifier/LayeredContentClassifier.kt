package com.screeniq.classifier

import com.screeniq.ai.FallbackLocalAiModel
import com.screeniq.ai.LocalAiModel
import com.screeniq.classifier.deterministic.DeterministicExtractor
import com.screeniq.classifier.heuristic.HeuristicClassifier
import com.screeniq.classifier.structured.StructuredFieldExtractor
import com.screeniq.core.contracts.ContentClassifier
import com.screeniq.core.model.ContentCategory
import com.screeniq.core.model.ContentClassification
import com.screeniq.core.model.DetectedEntity
import com.screeniq.core.model.ScreenContent

/**
 * Main classification engine for ScreenIQ implementing the 4-layer AI strategy:
 *
 * 1. Deterministic Extraction (Regex, structural rules)
 * 2. Lightweight Local Classification (Heuristic scoring matrix, zero latency)
 * 3. Optional Local LLM/SLM Model (MediaPipe/Gemma/ONNX plug-in behind [LocalAiModel])
 * 4. Safe UNKNOWN Fallback
 *
 * Fully decoupled from cloud AI, respecting device battery and privacy.
 */
class LayeredContentClassifier(
    private val deterministicExtractor: DeterministicExtractor = DeterministicExtractor(),
    private val heuristicClassifier: HeuristicClassifier = HeuristicClassifier(),
    private val structuredFieldExtractor: StructuredFieldExtractor = StructuredFieldExtractor(),
    private val localAiModel: LocalAiModel = FallbackLocalAiModel()
) : ContentClassifier {

    override suspend fun classify(content: ScreenContent): Result<ContentClassification> {
        return runCatching {
            val rawText = content.rawFullText.trim()

            // 1. Stage 1: Deterministic Extraction
            val extractedEntities = deterministicExtractor.extract(rawText)
            val combinedEntities = (content.detectedEntities + extractedEntities).distinctBy {
                "${it.type}-${it.rawValue}"
            }

            // 2. Stage 2: Lightweight Local Heuristic Scoring
            val heuristicResult = heuristicClassifier.classify(rawText, combinedEntities)

            var finalCategory = heuristicResult.primaryCategory
            var finalConfidence = heuristicResult.confidence
            var finalReasoning = heuristicResult.reasoning
            val secondaryCategories = heuristicResult.secondaryCategories.toMutableList()

            // 3. Stage 3: Optional Local LLM / On-Device SLM refinement
            // Triggered if heuristic confidence is low/ambiguous (<0.75) and local model is available
            if (finalConfidence < 0.75f && localAiModel.isAvailable) {
                val localAiResult = localAiModel.inferClassification(
                    rawText = rawText,
                    candidateCategories = ContentCategory.values().toList()
                )

                if (localAiResult != null && localAiResult.confidence > finalConfidence) {
                    if (finalCategory != localAiResult.suggestedCategory && !secondaryCategories.contains(finalCategory)) {
                        secondaryCategories.add(0, finalCategory)
                    }
                    finalCategory = localAiResult.suggestedCategory
                    finalConfidence = localAiResult.confidence
                    finalReasoning = "Refined by on-device local AI: ${localAiResult.reasoning}"
                }
            }

            // 4. Stage 4: Structured Fields & Safe Fallback
            val structuredFields = structuredFieldExtractor.extractFields(
                category = finalCategory,
                rawText = rawText,
                entities = combinedEntities
            )

            // Generate concise human-readable summary
            val summary = generateSummary(finalCategory, structuredFields, rawText)

            ContentClassification(
                captureId = content.captureId,
                primaryCategory = finalCategory,
                secondaryCategories = secondaryCategories,
                confidenceScore = finalConfidence,
                extractedEntities = combinedEntities,
                structuredFields = structuredFields,
                summary = summary,
                reasoningSummary = finalReasoning
            )
        }
    }

    private fun generateSummary(
        category: ContentCategory,
        fields: Map<String, String>,
        rawText: String
    ): String {
        return when (category) {
            ContentCategory.EVENT -> {
                val title = fields["title"] ?: "Event"
                val date = fields["date"]
                val time = fields["time"]
                val loc = fields["location"]
                listOfNotNull(title, date?.takeIf { it.isNotBlank() }, time?.takeIf { it.isNotBlank() }, loc?.takeIf { it.isNotBlank() })
                    .joinToString(" • ")
            }
            ContentCategory.PRODUCT -> {
                val title = fields["title"] ?: "Product"
                val price = fields["price"]
                listOfNotNull(title, price?.takeIf { it.isNotBlank() }).joinToString(" - ")
            }
            ContentCategory.LOCATION -> {
                fields["address"] ?: "Location details"
            }
            ContentCategory.PHONE -> {
                "Call ${fields["phoneNumber"]}"
            }
            ContentCategory.EMAIL -> {
                "Email ${fields["emailAddress"]}"
            }
            ContentCategory.URL -> {
                "Open ${fields["url"]}"
            }
            ContentCategory.CONTACT -> {
                val name = fields["name"] ?: "Contact"
                val org = fields["organization"]
                listOfNotNull(name, org?.takeIf { it.isNotBlank() }).joinToString(" - ")
            }
            ContentCategory.TASK -> {
                "Task: ${fields["title"]}"
            }
            ContentCategory.DOCUMENT -> {
                "Document: ${fields["title"]}"
            }
            ContentCategory.QR_CODE -> {
                "QR Code: ${fields["qrType"]}"
            }
            ContentCategory.IMAGE -> "Visual image on screen"
            ContentCategory.TEXT -> rawText.take(60)
            ContentCategory.UNKNOWN -> "Unrecognized screen content"
        }
    }
}
