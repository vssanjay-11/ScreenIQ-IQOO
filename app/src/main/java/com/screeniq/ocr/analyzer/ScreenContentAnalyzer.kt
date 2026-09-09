package com.screeniq.ocr.analyzer

import com.screeniq.core.contracts.OcrEngine
import com.screeniq.core.model.ScreenCaptureResult
import com.screeniq.core.model.ScreenContent
import com.screeniq.core.model.TextBlock
import com.screeniq.ocr.clustering.SpatialTextClusterer
import com.screeniq.ocr.engine.MlKitTextRecognizerDelegate
import com.screeniq.ocr.engine.TextRecognizerDelegate
import com.screeniq.ocr.entity.EntityPatternExtractor

/**
 * ScreenContentAnalyzer
 * 
 * Takes ScreenCaptureResult and extracts useful structured information.
 * - Extracts raw visible text
 * - Clusters text blocks with geometric bounding boxes
 * - Extracts phone numbers, email addresses, URLs, dates, times, addresses, currencies/prices
 * - Identifies obvious structured information (events, topics)
 * 
 * Strict Privacy:
 * - Operates entirely in memory.
 * - Does not save raw bitmap to disk.
 * - Avoids keeping lingering references to the input bitmap.
 * - Does NOT decide the final user action or invoke LLM reasoning.
 */
class ScreenContentAnalyzer(
    private val textRecognizer: TextRecognizerDelegate = MlKitTextRecognizerDelegate(),
    private val patternExtractor: EntityPatternExtractor = EntityPatternExtractor(),
    private val clusterer: SpatialTextClusterer = SpatialTextClusterer()
) : OcrEngine {

    override suspend fun extractContent(captureResult: ScreenCaptureResult): Result<ScreenContent> {
        val startTime = System.currentTimeMillis()

        return runCatching {
            val bitmap = captureResult.bitmap

            // 1. Extract visual text blocks from image using OCR
            val recognizedBlocks = textRecognizer.recognize(bitmap)

            // Guard against empty / recycled / corrupted input frames
            if (recognizedBlocks.isEmpty() && (bitmap == null || bitmap.width <= 0 || bitmap.height <= 0)) {
                return@runCatching ScreenContent(
                    captureId = captureResult.captureId,
                    rawFullText = "",
                    textBlocks = emptyList(),
                    detectedEntities = emptyList(),
                    extractionDurationMs = System.currentTimeMillis() - startTime
                )
            }

            // 2. Cluster spatial blocks if appropriate for coherent paragraph/header boundaries
            val clusteredBlocks = if (recognizedBlocks.size > 1) {
                clusterer.clusterBlocks(recognizedBlocks)
            } else {
                recognizedBlocks
            }

            // 3. Assemble raw full text
            val rawFullText = clusteredBlocks.joinToString("\n\n") { it.text.trim() }

            // 4. Extract structured entities across full text and individual blocks
            val globalEntities = patternExtractor.extractEntities(rawFullText)

            val blockEntities = clusteredBlocks.flatMap { block ->
                patternExtractor.extractEntities(block.text, block.boundingBox)
            }

            // Combine and de-duplicate
            val allEntities = (globalEntities + blockEntities).distinctBy {
                "${it.type}_${it.rawValue.trim().lowercase()}"
            }

            val durationMs = System.currentTimeMillis() - startTime

            ScreenContent(
                captureId = captureResult.captureId,
                rawFullText = rawFullText,
                textBlocks = clusteredBlocks,
                detectedEntities = allEntities,
                extractionDurationMs = durationMs
            )
        }
    }
}
