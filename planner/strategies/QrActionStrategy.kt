package com.screeniq.planner.strategies

import com.screeniq.core.model.ActionSuggestion
import com.screeniq.core.model.ActionType
import com.screeniq.core.model.ContentClassification
import com.screeniq.core.model.EntityType
import com.screeniq.planner.ActionParameters
import com.screeniq.planner.ActionProposalBuilder
import com.screeniq.planner.CategoryActionStrategy

class QrActionStrategy : CategoryActionStrategy {

    override fun plan(classification: ContentClassification): List<ActionSuggestion> {
        val suggestions = mutableListOf<ActionSuggestion>()
        val baseScore = classification.confidenceScore

        val qrEntity = classification.extractedEntities.firstOrNull { it.type == EntityType.QR_BARCODE }
        val qrContent = qrEntity?.normalizedValue ?: qrEntity?.rawValue ?: classification.summary ?: ""

        val isUrl = qrContent.startsWith("http://", ignoreCase = true) || qrContent.startsWith("https://", ignoreCase = true)
        val isPayment = qrContent.startsWith("upi://", ignoreCase = true) || qrContent.contains("paytm", ignoreCase = true)
        val isTel = qrContent.startsWith("tel:", ignoreCase = true)

        // 1. Primary: Inspect QR Code
        val inspectSuggestion = ActionProposalBuilder(ActionType.INSPECT_QR, baseScore)
            .title(if (isPayment) "Review Payment QR" else "Inspect QR Code")
            .description(if (isPayment) "Payment request detected. Explicit confirmation required." else "View raw QR code payload and metadata.")
            .isPrimary(true)
            .addParam(ActionParameters.QR_CONTENT, qrContent)
            .addParam(ActionParameters.IS_PAYMENT, isPayment.toString())
            .requireKeys(ActionParameters.QR_CONTENT)
            .build()
        suggestions.add(inspectSuggestion)

        // 2. Contextual secondary actions
        if (isUrl) {
            val openLinkSuggestion = ActionProposalBuilder(ActionType.OPEN_BROWSER, baseScore)
                .title("Open QR Link")
                .description("Navigate to $qrContent")
                .isPrimary(false)
                .addParam(ActionParameters.URL, qrContent)
                .requireKeys(ActionParameters.URL)
                .build()
            suggestions.add(openLinkSuggestion)
        } else if (isTel) {
            val phone = qrContent.removePrefix("tel:")
            val dialSuggestion = ActionProposalBuilder(ActionType.DIAL_PHONE, baseScore)
                .title("Call $phone")
                .description("Dial number extracted from QR code.")
                .isPrimary(false)
                .addParam(ActionParameters.PHONE_NUMBER, phone)
                .requireKeys(ActionParameters.PHONE_NUMBER)
                .build()
            suggestions.add(dialSuggestion)
        }

        // 3. Secondary: Copy QR Content
        val copySuggestion = ActionProposalBuilder(ActionType.COPY_TO_CLIPBOARD, baseScore)
            .title("Copy QR Content")
            .description("Copy \"$qrContent\" to clipboard.")
            .isPrimary(false)
            .addParam(ActionParameters.TEXT, qrContent)
            .requireKeys(ActionParameters.TEXT)
            .build()
        suggestions.add(copySuggestion)

        return suggestions
    }
}
