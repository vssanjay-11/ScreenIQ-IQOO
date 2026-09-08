package com.screeniq.core.contracts

import com.screeniq.core.model.ContentClassification
import com.screeniq.core.model.ScreenContent

/**
 * Master interface for Agent 5: AI Content Understanding and Classification.
 * Converts extracted ScreenContent into structured ContentClassification.
 */
interface ContentClassifier {
    /**
     * Classifies screen content into a high-level intent category with confidence,
     * extracted entities, and domain-specific structured fields.
     */
    suspend fun classify(content: ScreenContent): Result<ContentClassification>
}
