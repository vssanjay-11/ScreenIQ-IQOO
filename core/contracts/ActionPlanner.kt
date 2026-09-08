package com.screeniq.core.contracts

import com.screeniq.core.model.ActionSuggestion
import com.screeniq.core.model.ContentClassification

/**
 * Agent 6: Action Planner
 * Converts a semantic ContentClassification into ranked, safe ActionSuggestions.
 */
interface ActionPlanner {
    suspend fun planActions(classification: ContentClassification): List<ActionSuggestion>
}
