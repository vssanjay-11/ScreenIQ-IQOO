package com.screeniq.planner.strategies

import com.screeniq.core.model.ActionSuggestion
import com.screeniq.core.model.ActionType
import com.screeniq.core.model.ContentClassification
import com.screeniq.core.model.EntityType
import com.screeniq.planner.ActionParameters
import com.screeniq.planner.ActionProposalBuilder
import com.screeniq.planner.CategoryActionStrategy

class CommunicationActionStrategy : CategoryActionStrategy {

    override fun plan(classification: ContentClassification): List<ActionSuggestion> {
        val suggestions = mutableListOf<ActionSuggestion>()
        val baseScore = classification.confidenceScore

        val phoneEntity = classification.extractedEntities.firstOrNull { it.type == EntityType.PHONE_NUMBER }
        val emailEntity = classification.extractedEntities.firstOrNull { it.type == EntityType.EMAIL }

        // If phone number is found
        if (phoneEntity != null) {
            val phone = phoneEntity.normalizedValue ?: phoneEntity.rawValue

            // 1. Call via Dialer (Requires explicit confirmation, DANGEROUS)
            val dialSuggestion = ActionProposalBuilder(ActionType.DIAL_PHONE, baseScore)
                .title("Call $phone")
                .description("Open phone dialer to call $phone. Requires user confirmation.")
                .isPrimary(true)
                .addParam(ActionParameters.PHONE_NUMBER, phone)
                .requireKeys(ActionParameters.PHONE_NUMBER)
                .build()
            suggestions.add(dialSuggestion)

            // 2. Save Contact
            val contactSuggestion = ActionProposalBuilder(ActionType.SAVE_CONTACT, baseScore)
                .title("Save Contact")
                .description("Create a new contact entry with $phone.")
                .isPrimary(false)
                .addParam(ActionParameters.PHONE_NUMBER, phone)
                .addParam(ActionParameters.CONTACT_NAME, classification.summary)
                .requireKeys(ActionParameters.PHONE_NUMBER)
                .build()
            suggestions.add(contactSuggestion)

            // 3. Send SMS (Requires explicit confirmation, DANGEROUS)
            val smsSuggestion = ActionProposalBuilder(ActionType.SEND_SMS, baseScore)
                .title("Send SMS to $phone")
                .description("Prepare an SMS draft to $phone. Requires confirmation before sending.")
                .isPrimary(false)
                .addParam(ActionParameters.PHONE_NUMBER, phone)
                .requireKeys(ActionParameters.PHONE_NUMBER)
                .build()
            suggestions.add(smsSuggestion)

            // 4. Copy Phone
            val copySuggestion = ActionProposalBuilder(ActionType.COPY_TO_CLIPBOARD, baseScore)
                .title("Copy Phone Number")
                .description("Copy \"$phone\" to clipboard.")
                .isPrimary(false)
                .addParam(ActionParameters.TEXT, phone)
                .requireKeys(ActionParameters.TEXT)
                .build()
            suggestions.add(copySuggestion)
        }

        // If email is found
        if (emailEntity != null) {
            val email = emailEntity.normalizedValue ?: emailEntity.rawValue
            val isEmailPrimary = phoneEntity == null

            // 1. Compose Email (Opens composer draft; requires confirmation)
            val emailSuggestion = ActionProposalBuilder(ActionType.COMPOSE_EMAIL, baseScore)
                .title("Compose Email to $email")
                .description("Open email composer pre-filled for $email.")
                .isPrimary(isEmailPrimary)
                .addParam(ActionParameters.EMAIL_RECIPIENT, email)
                .requireKeys(ActionParameters.EMAIL_RECIPIENT)
                .build()
            suggestions.add(emailSuggestion)

            // 2. Copy Email
            val copyEmailSuggestion = ActionProposalBuilder(ActionType.COPY_TO_CLIPBOARD, baseScore)
                .title("Copy Email Address")
                .description("Copy \"$email\" to clipboard.")
                .isPrimary(false)
                .addParam(ActionParameters.TEXT, email)
                .requireKeys(ActionParameters.TEXT)
                .build()
            suggestions.add(copyEmailSuggestion)
        }

        // Fallback if neither entity was explicitly parsed
        if (suggestions.isEmpty()) {
            val fallbackText = classification.summary ?: "Communication Contact"
            suggestions.add(
                ActionProposalBuilder(ActionType.COPY_TO_CLIPBOARD, baseScore)
                    .title("Copy Details")
                    .description("Copy contact details to clipboard.")
                    .isPrimary(true)
                    .addParam(ActionParameters.TEXT, fallbackText)
                    .requireKeys(ActionParameters.TEXT)
                    .build()
            )
        }

        return suggestions
    }
}
