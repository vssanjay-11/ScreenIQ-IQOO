package com.screeniq.ui.preview

import com.screeniq.core.model.*

object MockData {

    // 1. Primary Event Scenario (AI Workshop from prompt/spec)
    val sampleEventClassification = ContentClassification(
        captureId = "cap_event_001",
        primaryCategory = ContentCategory.EVENT,
        extractedEntities = listOf(
            DetectedEntity(
                type = EntityType.DATE_TIME,
                rawValue = "20 September • 10:00 AM",
                normalizedValue = "2026-09-20T10:00:00",
                boundingBox = ScreenRect(100, 200, 500, 240),
                confidence = 0.95f
            ),
            DetectedEntity(
                type = EntityType.LOCATION_ADDRESS,
                rawValue = "Seminar Hall 2, Anna University",
                normalizedValue = "Seminar Hall 2, Anna University, Chennai",
                boundingBox = ScreenRect(100, 260, 600, 300),
                confidence = 0.92f
            )
        ),
        summary = "AI Workshop",
        confidenceScore = 0.96f
    )

    val sampleEventSuggestions = listOf(
        ActionSuggestion(
            actionId = "act_cal_01",
            type = ActionType.ADD_TO_CALENDAR,
            title = "📅 Add to Calendar",
            description = "Creates event on Sept 20, 10:00 AM at Seminar Hall 2",
            payload = mapOf(
                "title" to "AI Workshop",
                "dateTime" to "2026-09-20T10:00:00",
                "location" to "Seminar Hall 2, Anna University"
            ),
            confidence = ActionConfidence(0.96f, "Clear event date, time, and venue detected"),
            safetyLevel = ActionSafetyLevel.REQUIRE_CONFIRMATION,
            isPrimary = true
        ),
        ActionSuggestion(
            actionId = "act_copy_01",
            type = ActionType.COPY_TO_CLIPBOARD,
            title = "📋 Copy Details",
            description = "Copies title, date and venue to clipboard",
            payload = mapOf(
                "text" to "AI Workshop\n20 September • 10:00 AM\nSeminar Hall 2, Anna University"
            ),
            confidence = ActionConfidence(0.98f, "Exact text extraction available"),
            safetyLevel = ActionSafetyLevel.SAFE_AUTO,
            isPrimary = false
        ),
        ActionSuggestion(
            actionId = "act_map_01",
            type = ActionType.OPEN_MAPS,
            title = "🗺️ View on Maps",
            description = "Opens Anna University, Chennai in Google Maps",
            payload = mapOf(
                "query" to "Seminar Hall 2, Anna University, Chennai"
            ),
            confidence = ActionConfidence(0.91f, "Matched university campus venue"),
            safetyLevel = ActionSafetyLevel.SAFE_AUTO,
            isPrimary = false
        )
    )

    // 2. Location Scenario
    val sampleLocationClassification = ContentClassification(
        captureId = "cap_loc_002",
        primaryCategory = ContentCategory.LOCATION,
        extractedEntities = listOf(
            DetectedEntity(
                type = EntityType.LOCATION_ADDRESS,
                rawValue = "Kotturpuram, Chennai 600025",
                normalizedValue = "Kotturpuram, Chennai, Tamil Nadu 600025",
                boundingBox = ScreenRect(50, 150, 450, 190),
                confidence = 0.94f
            )
        ),
        summary = "Anna Centenary Library",
        confidenceScore = 0.93f
    )

    val sampleLocationSuggestions = listOf(
        ActionSuggestion(
            actionId = "act_loc_01",
            type = ActionType.OPEN_MAPS,
            title = "🗺️ Navigate in Maps",
            description = "Directions to Anna Centenary Library, Kotturpuram",
            payload = mapOf("query" to "Anna Centenary Library, Kotturpuram, Chennai"),
            confidence = ActionConfidence(0.94f, "Postal code and landmark verified"),
            safetyLevel = ActionSafetyLevel.SAFE_AUTO,
            isPrimary = true
        ),
        ActionSuggestion(
            actionId = "act_loc_02",
            type = ActionType.COPY_TO_CLIPBOARD,
            title = "📋 Copy Address",
            description = "Copies address string to clipboard",
            payload = mapOf("text" to "Anna Centenary Library, Kotturpuram, Chennai 600025"),
            confidence = ActionConfidence(0.99f, "Standard string payload"),
            safetyLevel = ActionSafetyLevel.SAFE_AUTO,
            isPrimary = false
        )
    )

    // 3. Contact Scenario
    val sampleContactClassification = ContentClassification(
        captureId = "cap_contact_003",
        primaryCategory = ContentCategory.PHONE,
        extractedEntities = listOf(
            DetectedEntity(
                type = EntityType.PHONE_NUMBER,
                rawValue = "+91 98401 23456",
                normalizedValue = "+919840123456",
                boundingBox = ScreenRect(80, 220, 380, 260),
                confidence = 0.97f
            )
        ),
        summary = "Technical Helpdesk Contact",
        confidenceScore = 0.95f
    )

    val sampleContactSuggestions = listOf(
        ActionSuggestion(
            actionId = "act_dial_01",
            type = ActionType.DIAL_PHONE,
            title = "📞 Call Number",
            description = "Pre-fills +91 98401 23456 in Phone dialer",
            payload = mapOf("phoneNumber" to "+919840123456"),
            confidence = ActionConfidence(0.97f, "Standard 10-digit mobile number with country code"),
            safetyLevel = ActionSafetyLevel.DANGEROUS,
            isPrimary = true
        ),
        ActionSuggestion(
            actionId = "act_sms_01",
            type = ActionType.SEND_SMS,
            title = "💬 Send Message",
            description = "Opens SMS thread with +91 98401 23456",
            payload = mapOf("phoneNumber" to "+919840123456"),
            confidence = ActionConfidence(0.97f, "Valid mobile number destination"),
            safetyLevel = ActionSafetyLevel.REQUIRE_CONFIRMATION,
            isPrimary = false
        )
    )

    // 4. Sample History Items
    val sampleHistoryItems = listOf(
        ActionHistoryItem(
            id = "hist_001",
            timestampMs = System.currentTimeMillis() - 120_000, // 2 mins ago
            category = ContentCategory.EVENT,
            actionType = ActionType.ADD_TO_CALENDAR,
            summarySnippet = "AI Workshop added to Google Calendar (Sept 20, 10:00 AM)",
            wasSuccessful = true
        ),
        ActionHistoryItem(
            id = "hist_002",
            timestampMs = System.currentTimeMillis() - 900_000, // 15 mins ago
            category = ContentCategory.LOCATION,
            actionType = ActionType.OPEN_MAPS,
            summarySnippet = "Directions opened for Anna Centenary Library",
            wasSuccessful = true
        ),
        ActionHistoryItem(
            id = "hist_003",
            timestampMs = System.currentTimeMillis() - 3_600_000, // 1 hour ago
            category = ContentCategory.PHONE,
            actionType = ActionType.DIAL_PHONE,
            summarySnippet = "Dialer pre-filled with +91 98401 23456",
            wasSuccessful = true
        ),
        ActionHistoryItem(
            id = "hist_004",
            timestampMs = System.currentTimeMillis() - 86_400_000, // 1 day ago
            category = ContentCategory.URL,
            actionType = ActionType.OPEN_BROWSER,
            summarySnippet = "Opened https://github.com/iQOO-Hackathon/ScreenIQ",
            wasSuccessful = true
        ),
        ActionHistoryItem(
            id = "hist_005",
            timestampMs = System.currentTimeMillis() - 172_800_000, // 2 days ago
            category = ContentCategory.TASK,
            actionType = ActionType.CREATE_TASK,
            summarySnippet = "Created task: Submit City Battle project deck",
            wasSuccessful = true
        )
    )
}
