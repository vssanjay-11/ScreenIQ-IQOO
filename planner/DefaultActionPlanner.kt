package com.screeniq.planner

import com.screeniq.core.contracts.ActionPlanner
import com.screeniq.core.model.ActionSuggestion
import com.screeniq.core.model.ContentCategory
import com.screeniq.core.model.ContentClassification
import com.screeniq.planner.strategies.CommerceActionStrategy
import com.screeniq.planner.strategies.CommunicationActionStrategy
import com.screeniq.planner.strategies.DocumentActionStrategy
import com.screeniq.planner.strategies.EventActionStrategy
import com.screeniq.planner.strategies.LocationActionStrategy
import com.screeniq.planner.strategies.ProductivityTaskStrategy
import com.screeniq.planner.strategies.QrActionStrategy
import com.screeniq.planner.strategies.UnknownActionStrategy
import com.screeniq.planner.strategies.WebLinkActionStrategy

/**
 * Default implementation of ActionPlanner.
 * Converts semantic ContentClassification into prioritized, safety-enforced ActionSuggestions.
 */
class DefaultActionPlanner(
    private val strategies: Map<ContentCategory, CategoryActionStrategy> = defaultStrategies()
) : ActionPlanner {

    override suspend fun planActions(classification: ContentClassification): List<ActionSuggestion> {
        val resultSuggestions = mutableListOf<ActionSuggestion>()

        // 1. Resolve strategy for primary category
        val primaryStrategy = strategies[classification.primaryCategory] ?: strategies[ContentCategory.UNKNOWN_GENERAL]!!
        val primarySuggestions = primaryStrategy.plan(classification)

        resultSuggestions.addAll(primarySuggestions)

        // 2. Resolve additional actions from secondary categories if present
        for (secondaryCategory in classification.secondaryCategories) {
            if (secondaryCategory != classification.primaryCategory) {
                strategies[secondaryCategory]?.let { secondaryStrategy ->
                    val secondarySuggestions = secondaryStrategy.plan(classification)
                    // Mark secondary suggestions as not primary
                    resultSuggestions.addAll(secondarySuggestions.map { it.copy(isPrimary = false) })
                }
            }
        }

        // 3. Deduplicate actions by type and key payload
        val seenSignatures = mutableSetOf<String>()
        val deduplicated = mutableListOf<ActionSuggestion>()

        for (suggestion in resultSuggestions) {
            val signature = "${suggestion.type}_${suggestion.payload.entries.sortedBy { it.key }.joinToString("&") { "${it.key}=${it.value}" }}"
            if (seenSignatures.add(signature)) {
                deduplicated.add(suggestion)
            }
        }

        // 4. Ensure exactly one primary action is set (the first element)
        return deduplicated.mapIndexed { index, suggestion ->
            if (index == 0) {
                suggestion.copy(isPrimary = true)
            } else {
                suggestion.copy(isPrimary = false)
            }
        }
    }

    companion object {
        fun defaultStrategies(): Map<ContentCategory, CategoryActionStrategy> {
            return mapOf(
                ContentCategory.EVENT to EventActionStrategy(),
                ContentCategory.LOCATION to LocationActionStrategy(),
                ContentCategory.COMMUNICATION to CommunicationActionStrategy(),
                ContentCategory.WEB_LINK to WebLinkActionStrategy(),
                ContentCategory.COMMERCE_PRODUCT to CommerceActionStrategy(),
                ContentCategory.PRODUCTIVITY_TASK to ProductivityTaskStrategy(),
                ContentCategory.DOCUMENT_SUMMARY to DocumentActionStrategy(),
                ContentCategory.QR_ACTION to QrActionStrategy(),
                ContentCategory.UNKNOWN_GENERAL to UnknownActionStrategy()
            )
        }
    }
}
