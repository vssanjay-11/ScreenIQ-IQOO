package com.screeniq.planner.strategies

import com.screeniq.core.model.ActionSuggestion
import com.screeniq.core.model.ActionType
import com.screeniq.core.model.ContentClassification
import com.screeniq.core.model.EntityType
import com.screeniq.planner.ActionParameters
import com.screeniq.planner.ActionProposalBuilder
import com.screeniq.planner.CategoryActionStrategy

class WebLinkActionStrategy : CategoryActionStrategy {

    override fun plan(classification: ContentClassification): List<ActionSuggestion> {
        val suggestions = mutableListOf<ActionSuggestion>()
        val baseScore = classification.confidenceScore

        val urlEntity = classification.extractedEntities.firstOrNull { it.type == EntityType.URL }
        val rawUrl = urlEntity?.normalizedValue ?: urlEntity?.rawValue ?: classification.summary ?: ""
        val formattedUrl = if (!rawUrl.startsWith("http://") && !rawUrl.startsWith("https://")) {
            "https://$rawUrl"
        } else {
            rawUrl
        }

        // 1. Primary: Open in Browser
        val browserSuggestion = ActionProposalBuilder(ActionType.OPEN_BROWSER, baseScore)
            .title("Open Link in Browser")
            .description("Visit $formattedUrl")
            .isPrimary(true)
            .addParam(ActionParameters.URL, formattedUrl)
            .requireKeys(ActionParameters.URL)
            .build()
        suggestions.add(browserSuggestion)

        // 2. Secondary: Copy Link
        val copySuggestion = ActionProposalBuilder(ActionType.COPY_TO_CLIPBOARD, baseScore)
            .title("Copy Link")
            .description("Copy \"$formattedUrl\" to clipboard.")
            .isPrimary(false)
            .addParam(ActionParameters.TEXT, formattedUrl)
            .requireKeys(ActionParameters.TEXT)
            .build()
        suggestions.add(copySuggestion)

        return suggestions
    }
}
