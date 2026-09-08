package com.screeniq.planner.strategies

import com.screeniq.core.model.ActionSuggestion
import com.screeniq.core.model.ActionType
import com.screeniq.core.model.ContentClassification
import com.screeniq.core.model.EntityType
import com.screeniq.planner.ActionParameters
import com.screeniq.planner.ActionProposalBuilder
import com.screeniq.planner.CategoryActionStrategy

class CommerceActionStrategy : CategoryActionStrategy {

    override fun plan(classification: ContentClassification): List<ActionSuggestion> {
        val suggestions = mutableListOf<ActionSuggestion>()
        val baseScore = classification.confidenceScore

        val productEntity = classification.extractedEntities.firstOrNull { it.type == EntityType.PRODUCT_INFO }
        val priceEntity = classification.extractedEntities.firstOrNull { it.type == EntityType.NUMERIC_FINANCIAL }

        val productTitle = productEntity?.normalizedValue
            ?: productEntity?.rawValue
            ?: classification.summary
            ?: "Detected Product"

        val price = priceEntity?.normalizedValue ?: priceEntity?.rawValue

        // 1. Primary: Search Product
        val searchSuggestion = ActionProposalBuilder(ActionType.SEARCH_PRODUCT, baseScore)
            .title("Search \"$productTitle\"")
            .description("Look up specifications and price comparisons online.")
            .isPrimary(true)
            .addParam(ActionParameters.SEARCH_QUERY, productTitle)
            .addParam(ActionParameters.PRODUCT_TITLE, productTitle)
            .addParam(ActionParameters.PRODUCT_PRICE, price)
            .requireKeys(ActionParameters.SEARCH_QUERY)
            .build()
        suggestions.add(searchSuggestion)

        // 2. Secondary: Save for Later (Task/Wishlist)
        val saveSuggestion = ActionProposalBuilder(ActionType.CREATE_TASK, baseScore)
            .title("Save for Later")
            .description("Add \"$productTitle\" to your wishlist or task list.")
            .isPrimary(false)
            .addParam(ActionParameters.TASK_TITLE, "Buy: $productTitle")
            .addParam(ActionParameters.TASK_NOTES, if (price != null) "Price: $price" else null)
            .requireKeys(ActionParameters.TASK_TITLE)
            .build()
        suggestions.add(saveSuggestion)

        // 3. Secondary: Copy Details
        val detailsText = buildString {
            append("Product: ").append(productTitle)
            if (price != null) append(" | Price: ").append(price)
        }
        val copySuggestion = ActionProposalBuilder(ActionType.COPY_TO_CLIPBOARD, baseScore)
            .title("Copy Product Details")
            .description("Copy product title and price info.")
            .isPrimary(false)
            .addParam(ActionParameters.TEXT, detailsText)
            .requireKeys(ActionParameters.TEXT)
            .build()
        suggestions.add(copySuggestion)

        return suggestions
    }
}
