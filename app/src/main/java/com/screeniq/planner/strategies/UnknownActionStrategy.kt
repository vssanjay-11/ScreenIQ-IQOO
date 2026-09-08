package com.screeniq.planner.strategies

import com.screeniq.core.model.ActionSuggestion
import com.screeniq.core.model.ActionType
import com.screeniq.core.model.ContentClassification
import com.screeniq.planner.ActionParameters
import com.screeniq.planner.ActionProposalBuilder
import com.screeniq.planner.CategoryActionStrategy

class UnknownActionStrategy : CategoryActionStrategy {

    override fun plan(classification: ContentClassification): List<ActionSuggestion> {
        val suggestions = mutableListOf<ActionSuggestion>()
        val baseScore = classification.confidenceScore.coerceAtMost(0.49f) // Low confidence tier

        val summaryOrText = classification.summary?.takeIf { it.isNotBlank() } ?: "Screen Content"

        // 1. Primary: Ask User
        val askUserSuggestion = ActionProposalBuilder(ActionType.ASK_USER_CUSTOM, baseScore)
            .title("Ask ScreenIQ")
            .description("Tell ScreenIQ what you'd like to do with this screen.")
            .isPrimary(true)
            .addParam(ActionParameters.USER_PROMPT, "")
            .addParam(ActionParameters.CONTEXT_SUMMARY, summaryOrText)
            .build()
        suggestions.add(askUserSuggestion)

        // 2. Secondary: Copy Text
        val copySuggestion = ActionProposalBuilder(ActionType.COPY_TO_CLIPBOARD, baseScore)
            .title("Copy Screen Text")
            .description("Copy detected text to clipboard.")
            .isPrimary(false)
            .addParam(ActionParameters.TEXT, summaryOrText)
            .requireKeys(ActionParameters.TEXT)
            .build()
        suggestions.add(copySuggestion)

        return suggestions
    }
}
