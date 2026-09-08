package com.screeniq.planner

import com.screeniq.core.model.ActionSuggestion
import com.screeniq.core.model.ContentClassification

/**
 * Strategy interface for converting a specific ContentClassification category
 * into a tailored, prioritized list of ActionSuggestions.
 */
interface CategoryActionStrategy {
    fun plan(classification: ContentClassification): List<ActionSuggestion>
}
