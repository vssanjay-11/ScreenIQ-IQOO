package com.screeniq.planner.strategies

import com.screeniq.core.model.ActionSuggestion
import com.screeniq.core.model.ActionType
import com.screeniq.core.model.ContentClassification
import com.screeniq.core.model.EntityType
import com.screeniq.planner.ActionParameters
import com.screeniq.planner.ActionProposalBuilder
import com.screeniq.planner.CategoryActionStrategy

class DocumentActionStrategy : CategoryActionStrategy {

    override fun plan(classification: ContentClassification): List<ActionSuggestion> {
        val suggestions = mutableListOf<ActionSuggestion>()
        val baseScore = classification.confidenceScore

        val documentEntity = classification.extractedEntities.firstOrNull { it.type == EntityType.DOCUMENT_SNIPPET }
        val rawText = documentEntity?.rawValue ?: classification.summary ?: ""
        val preview = if (rawText.length > 60) "${rawText.take(57)}..." else rawText

        // 1. Primary: Summarize Document
        val summarizeSuggestion = ActionProposalBuilder(ActionType.SUMMARIZE_DOCUMENT, baseScore)
            .title("Summarize Document")
            .description("Generate executive summary and key takeaways.")
            .isPrimary(true)
            .addParam(ActionParameters.RAW_CONTENT, rawText)
            .addParam(ActionParameters.SUMMARY, classification.summary)
            .requireKeys(ActionParameters.RAW_CONTENT)
            .build()
        suggestions.add(summarizeSuggestion)

        // 2. Secondary: Extract & Copy Text
        val copySuggestion = ActionProposalBuilder(ActionType.COPY_TO_CLIPBOARD, baseScore)
            .title("Extract & Copy Text")
            .description("Copy entire document text to clipboard.")
            .isPrimary(false)
            .addParam(ActionParameters.TEXT, rawText)
            .requireKeys(ActionParameters.TEXT)
            .build()
        suggestions.add(copySuggestion)

        // 3. Secondary: Save as Note
        val noteSuggestion = ActionProposalBuilder(ActionType.CREATE_TASK, baseScore)
            .title("Save as Note")
            .description("Save \"$preview\" to notes.")
            .isPrimary(false)
            .addParam(ActionParameters.TASK_TITLE, "Document Note: $preview")
            .addParam(ActionParameters.TASK_NOTES, rawText)
            .requireKeys(ActionParameters.TASK_TITLE)
            .build()
        suggestions.add(noteSuggestion)

        return suggestions
    }
}
