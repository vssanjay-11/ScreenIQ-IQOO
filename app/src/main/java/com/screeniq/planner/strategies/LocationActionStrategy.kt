package com.screeniq.planner.strategies

import com.screeniq.core.model.ActionSuggestion
import com.screeniq.core.model.ActionType
import com.screeniq.core.model.ContentClassification
import com.screeniq.core.model.EntityType
import com.screeniq.planner.ActionParameters
import com.screeniq.planner.ActionProposalBuilder
import com.screeniq.planner.CategoryActionStrategy

class LocationActionStrategy : CategoryActionStrategy {

    override fun plan(classification: ContentClassification): List<ActionSuggestion> {
        val suggestions = mutableListOf<ActionSuggestion>()
        val baseScore = classification.confidenceScore

        val locationEntity = classification.extractedEntities.firstOrNull { it.type == EntityType.LOCATION_ADDRESS }
        val addressText = locationEntity?.normalizedValue
            ?: locationEntity?.rawValue
            ?: classification.summary
            ?: "Detected Location"

        // 1. Primary: Open Maps
        val mapSuggestion = ActionProposalBuilder(ActionType.OPEN_MAPS, baseScore)
            .title("Open in Maps")
            .description("Search or navigate to $addressText")
            .isPrimary(true)
            .addParam(ActionParameters.MAP_QUERY, addressText)
            .addParam(ActionParameters.MAP_ADDRESS, addressText)
            .requireKeys(ActionParameters.MAP_QUERY)
            .build()
        suggestions.add(mapSuggestion)

        // 2. Secondary: Copy Address
        val copySuggestion = ActionProposalBuilder(ActionType.COPY_TO_CLIPBOARD, baseScore)
            .title("Copy Address")
            .description("Copy \"$addressText\" to clipboard.")
            .isPrimary(false)
            .addParam(ActionParameters.TEXT, addressText)
            .requireKeys(ActionParameters.TEXT)
            .build()
        suggestions.add(copySuggestion)

        return suggestions
    }
}
