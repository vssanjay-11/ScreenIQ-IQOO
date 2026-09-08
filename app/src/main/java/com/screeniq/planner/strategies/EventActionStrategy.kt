package com.screeniq.planner.strategies

import com.screeniq.core.model.ActionSuggestion
import com.screeniq.core.model.ActionType
import com.screeniq.core.model.ContentClassification
import com.screeniq.core.model.EntityType
import com.screeniq.planner.ActionParameters
import com.screeniq.planner.ActionProposalBuilder
import com.screeniq.planner.CategoryActionStrategy

class EventActionStrategy : CategoryActionStrategy {

    override fun plan(classification: ContentClassification): List<ActionSuggestion> {
        val suggestions = mutableListOf<ActionSuggestion>()
        val baseScore = classification.confidenceScore

        // Extract entities
        val dateTimeEntity = classification.extractedEntities.firstOrNull { it.type == EntityType.DATE_TIME }
        val locationEntity = classification.extractedEntities.firstOrNull { it.type == EntityType.LOCATION_ADDRESS }
        val summaryText = classification.summary?.takeIf { it.isNotBlank() } ?: "Upcoming Event"

        val eventTitle = summaryText
        val dateValue = dateTimeEntity?.normalizedValue ?: dateTimeEntity?.rawValue ?: ""
        val locationValue = locationEntity?.normalizedValue ?: locationEntity?.rawValue ?: ""

        // 1. Primary: Add to Calendar
        val calendarSuggestion = ActionProposalBuilder(ActionType.ADD_TO_CALENDAR, baseScore)
            .title("Add \"$eventTitle\" to Calendar")
            .description("Schedule this event on your calendar with extracted date and venue.")
            .isPrimary(true)
            .addParam(ActionParameters.EVENT_TITLE, eventTitle)
            .addParam(ActionParameters.EVENT_START_DATE, dateValue)
            .addParam(ActionParameters.EVENT_LOCATION, locationValue)
            .addParam(ActionParameters.EVENT_DESCRIPTION, classification.summary)
            .requireKeys(ActionParameters.EVENT_TITLE)
            .optionalKeys(ActionParameters.EVENT_START_DATE, ActionParameters.EVENT_LOCATION)
            .build()
        suggestions.add(calendarSuggestion)

        // 2. Secondary: If location/venue is available, offer to view on Maps
        if (locationValue.isNotBlank()) {
            val mapSuggestion = ActionProposalBuilder(ActionType.OPEN_MAPS, baseScore)
                .title("View Location on Maps")
                .description("Navigate to $locationValue")
                .isPrimary(false)
                .addParam(ActionParameters.MAP_QUERY, locationValue)
                .addParam(ActionParameters.MAP_ADDRESS, locationValue)
                .requireKeys(ActionParameters.MAP_QUERY)
                .build()
            suggestions.add(mapSuggestion)
        }

        // 3. Secondary: Copy Event Details to Clipboard
        val detailsToCopy = buildString {
            append("Event: ").appendLine(eventTitle)
            if (dateValue.isNotBlank()) append("Date: ").appendLine(dateValue)
            if (locationValue.isNotBlank()) append("Location: ").appendLine(locationValue)
        }.trim()

        val copySuggestion = ActionProposalBuilder(ActionType.COPY_TO_CLIPBOARD, baseScore)
            .title("Copy Event Details")
            .description("Copy event information to clipboard.")
            .isPrimary(false)
            .addParam(ActionParameters.TEXT, detailsToCopy.ifBlank { summaryText })
            .requireKeys(ActionParameters.TEXT)
            .build()
        suggestions.add(copySuggestion)

        return suggestions
    }
}
