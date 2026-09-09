package com.screeniq.planner

import com.screeniq.core.model.ActionSafetyLevel
import com.screeniq.core.model.ActionType
import com.screeniq.core.model.ContentCategory
import com.screeniq.core.model.ContentClassification
import com.screeniq.core.model.DetectedEntity
import com.screeniq.core.model.EntityType
import kotlinx.coroutines.runBlocking

/**
 * End-to-end unit test suite for Action Planning across all ContentCategories:
 * EVENT, LOCATION, COMMUNICATION, WEB_LINK, COMMERCE_PRODUCT, PRODUCTIVITY_TASK,
 * DOCUMENT_SUMMARY, QR_ACTION, UNKNOWN_GENERAL.
 */
class DefaultActionPlannerTest {

    private val planner = DefaultActionPlanner()

    fun testEventPlanning() = runBlocking {
        val classification = ContentClassification(
            captureId = "cap_event_01",
            primaryCategory = ContentCategory.EVENT,
            extractedEntities = listOf(
                DetectedEntity(
                    type = EntityType.DATE_TIME,
                    rawValue = "20 September 2026, 10:00 AM",
                    normalizedValue = "2026-09-20T10:00:00Z"
                ),
                DetectedEntity(
                    type = EntityType.LOCATION_ADDRESS,
                    rawValue = "Seminar Hall 2, Anna University",
                    normalizedValue = "Seminar Hall 2, Anna University, Chennai"
                )
            ),
            summary = "AI Workshop",
            confidenceScore = 0.96f
        )

        val suggestions = planner.planActions(classification)

        assert(suggestions.isNotEmpty()) { "Suggestions should not be empty" }

        // Primary action
        val primary = suggestions.first()
        assert(primary.isPrimary) { "First suggestion must be primary" }
        assert(primary.type == ActionType.ADD_TO_CALENDAR) { "Primary must be ADD_TO_CALENDAR" }
        assert(primary.title.contains("AI Workshop")) { "Title must reference event" }
        assert(primary.requiresConfirmation) { "Calendar addition must require confirmation" }
        assert(primary.payload[ActionParameters.EVENT_START_DATE] == "2026-09-20T10:00:00Z")
        assert(primary.payload[ActionParameters.EVENT_LOCATION] == "Seminar Hall 2, Anna University, Chennai")

        // Secondary actions: Open Maps & Copy Details
        val mapSuggestion = suggestions.firstOrNull { it.type == ActionType.OPEN_MAPS }
        assert(mapSuggestion != null) { "Should include Open Maps for venue" }

        val copySuggestion = suggestions.firstOrNull { it.type == ActionType.COPY_TO_CLIPBOARD }
        assert(copySuggestion != null) { "Should include Copy Event Details" }
    }

    fun testLocationPlanning() = runBlocking {
        val classification = ContentClassification(
            captureId = "cap_loc_01",
            primaryCategory = ContentCategory.LOCATION,
            extractedEntities = listOf(
                DetectedEntity(
                    type = EntityType.LOCATION_ADDRESS,
                    rawValue = "Phoenix Marketcity, Velachery, Chennai",
                    normalizedValue = "Phoenix Marketcity, Velachery, Chennai"
                )
            ),
            summary = "Phoenix Marketcity",
            confidenceScore = 0.92f
        )

        val suggestions = planner.planActions(classification)
        val primary = suggestions.first()
        assert(primary.type == ActionType.OPEN_MAPS)
        assert(primary.safetyLevel == ActionSafetyLevel.SAFE_AUTO) { "High confidence maps can be SAFE_AUTO" }
        assert(!primary.requiresConfirmation)

        val copy = suggestions.firstOrNull { it.type == ActionType.COPY_TO_CLIPBOARD }
        assert(copy != null)
    }

    fun testPhoneCommunicationPlanning() = runBlocking {
        val classification = ContentClassification(
            captureId = "cap_comm_01",
            primaryCategory = ContentCategory.PHONE,
            extractedEntities = listOf(
                DetectedEntity(
                    type = EntityType.PHONE_NUMBER,
                    rawValue = "+91 98765 43210",
                    normalizedValue = "+919876543210"
                )
            ),
            summary = "Customer Support",
            confidenceScore = 0.95f
        )

        val suggestions = planner.planActions(classification)
        val dialAction = suggestions.first { it.type == ActionType.DIAL_PHONE }
        assert(dialAction.isPrimary)
        assert(dialAction.safetyLevel == ActionSafetyLevel.DANGEROUS) { "Phone call must be DANGEROUS" }
        assert(dialAction.requiresConfirmation) { "Phone call must require confirmation" }

        val saveContact = suggestions.firstOrNull { it.type == ActionType.SAVE_CONTACT }
        assert(saveContact != null)

        val sendSms = suggestions.firstOrNull { it.type == ActionType.SEND_SMS }
        assert(sendSms != null)
        assert(sendSms?.safetyLevel == ActionSafetyLevel.DANGEROUS)
    }

    fun testEmailCommunicationPlanning() = runBlocking {
        val classification = ContentClassification(
            captureId = "cap_comm_02",
            primaryCategory = ContentCategory.EMAIL,
            extractedEntities = listOf(
                DetectedEntity(
                    type = EntityType.EMAIL,
                    rawValue = "contact@screeniq.ai",
                    normalizedValue = "contact@screeniq.ai"
                )
            ),
            summary = "Inquiry Email",
            confidenceScore = 0.90f
        )

        val suggestions = planner.planActions(classification)
        val emailAction = suggestions.first { it.type == ActionType.COMPOSE_EMAIL }
        assert(emailAction.isPrimary)
        assert(emailAction.requiresConfirmation)
        assert(emailAction.payload[ActionParameters.EMAIL_RECIPIENT] == "contact@screeniq.ai")
    }

    fun testWebLinkPlanning() = runBlocking {
        val classification = ContentClassification(
            captureId = "cap_url_01",
            primaryCategory = ContentCategory.URL,
            extractedEntities = listOf(
                DetectedEntity(
                    type = EntityType.URL,
                    rawValue = "https://github.com/screeniq/app",
                    normalizedValue = "https://github.com/screeniq/app"
                )
            ),
            summary = "GitHub Repo",
            confidenceScore = 0.94f
        )

        val suggestions = planner.planActions(classification)
        val primary = suggestions.first()
        assert(primary.type == ActionType.OPEN_BROWSER)
        assert(primary.payload[ActionParameters.URL] == "https://github.com/screeniq/app")
    }

    fun testCommerceProductPlanning() = runBlocking {
        val classification = ContentClassification(
            captureId = "cap_prod_01",
            primaryCategory = ContentCategory.PRODUCT,
            extractedEntities = listOf(
                DetectedEntity(
                    type = EntityType.PRODUCT_INFO,
                    rawValue = "iQOO 13 5G Smartphone",
                    normalizedValue = "iQOO 13 5G"
                ),
                DetectedEntity(
                    type = EntityType.NUMERIC_FINANCIAL,
                    rawValue = "Rs. 54,999",
                    normalizedValue = "54999"
                )
            ),
            summary = "iQOO 13 5G",
            confidenceScore = 0.89f
        )

        val suggestions = planner.planActions(classification)
        val search = suggestions.first { it.type == ActionType.SEARCH_PRODUCT }
        assert(search.isPrimary)

        val saveLater = suggestions.firstOrNull { it.type == ActionType.CREATE_TASK }
        assert(saveLater != null)
    }

    fun testProductivityTaskPlanning() = runBlocking {
        val classification = ContentClassification(
            captureId = "cap_task_01",
            primaryCategory = ContentCategory.TASK,
            extractedEntities = listOf(
                DetectedEntity(
                    type = EntityType.TASK_TODO,
                    rawValue = "Submit hackathon presentation slides",
                    normalizedValue = "Submit hackathon presentation slides"
                ),
                DetectedEntity(
                    type = EntityType.DATE_TIME,
                    rawValue = "Tomorrow 5 PM",
                    normalizedValue = "Tomorrow 17:00"
                )
            ),
            summary = "Submit slides",
            confidenceScore = 0.91f
        )

        val suggestions = planner.planActions(classification)
        val task = suggestions.first { it.type == ActionType.CREATE_TASK }
        assert(task.isPrimary)
        assert(task.requiresConfirmation)

        val reminder = suggestions.firstOrNull { it.type == ActionType.SET_REMINDER }
        assert(reminder != null)
    }

    fun testDocumentSummaryPlanning() = runBlocking {
        val classification = ContentClassification(
            captureId = "cap_doc_01",
            primaryCategory = ContentCategory.DOCUMENT,
            extractedEntities = listOf(
                DetectedEntity(
                    type = EntityType.DOCUMENT_SNIPPET,
                    rawValue = "This agreement governs the terms and conditions of the ScreenIQ hackathon challenge..."
                )
            ),
            summary = "Hackathon Terms Agreement",
            confidenceScore = 0.88f
        )

        val suggestions = planner.planActions(classification)
        val summarize = suggestions.first { it.type == ActionType.SUMMARIZE_DOCUMENT }
        assert(summarize.isPrimary)

        val copy = suggestions.firstOrNull { it.type == ActionType.COPY_TO_CLIPBOARD }
        assert(copy != null)
    }

    fun testQrActionPlanning() = runBlocking {
        // Test Payment QR (Must be marked DANGEROUS, user confirmation required)
        val paymentQrClassification = ContentClassification(
            captureId = "cap_qr_01",
            primaryCategory = ContentCategory.QR_CODE,
            extractedEntities = listOf(
                DetectedEntity(
                    type = EntityType.QR_BARCODE,
                    rawValue = "upi://pay?pa=organizer@upi&pn=Hackathon&am=100",
                    normalizedValue = "upi://pay?pa=organizer@upi&pn=Hackathon&am=100"
                )
            ),
            summary = "UPI Payment QR",
            confidenceScore = 0.99f
        )

        val suggestions = planner.planActions(paymentQrClassification)
        val inspectAction = suggestions.first { it.type == ActionType.INSPECT_QR }
        assert(inspectAction.safetyLevel == ActionSafetyLevel.DANGEROUS) { "Payment QR must be marked DANGEROUS" }
        assert(inspectAction.requiresConfirmation) { "Payment QR must require confirmation" }
    }

    fun testUnknownGeneralPlanning() = runBlocking {
        val classification = ContentClassification(
            captureId = "cap_unk_01",
            primaryCategory = ContentCategory.UNKNOWN,
            summary = "Unrecognized graphical diagram",
            confidenceScore = 0.35f
        )

        val suggestions = planner.planActions(classification)
        val primary = suggestions.first()
        assert(primary.type == ActionType.ASK_USER_CUSTOM)
        assert(primary.requiresConfirmation) { "Low confidence unknown screen must prompt user" }
    }
}
