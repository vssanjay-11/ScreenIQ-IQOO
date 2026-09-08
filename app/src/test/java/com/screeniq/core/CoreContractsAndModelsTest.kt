package com.screeniq.core

import com.screeniq.core.common.DefaultDispatcherProvider
import com.screeniq.core.common.ScreenIqConstants
import com.screeniq.core.common.ScreenIqResult
import com.screeniq.core.model.*
import org.junit.Assert.*
import org.junit.Test

class CoreContractsAndModelsTest {

    @Test
    fun testConfidenceLevelThresholds() {
        assertEquals(ConfidenceLevel.HIGH, ConfidenceLevel.fromScore(0.95f))
        assertEquals(ConfidenceLevel.HIGH, ConfidenceLevel.fromScore(0.75f))
        assertEquals(ConfidenceLevel.MEDIUM, ConfidenceLevel.fromScore(0.74f))
        assertEquals(ConfidenceLevel.MEDIUM, ConfidenceLevel.fromScore(0.45f))
        assertEquals(ConfidenceLevel.LOW, ConfidenceLevel.fromScore(0.44f))
        assertEquals(ConfidenceLevel.LOW, ConfidenceLevel.fromScore(0.01f))
        assertEquals(ConfidenceLevel.UNKNOWN, ConfidenceLevel.fromScore(0.0f))
        assertEquals(ConfidenceLevel.UNKNOWN, ConfidenceLevel.fromScore(-0.1f))
    }

    @Test
    fun testScreenContentModel() {
        val rect = ScreenRect(left = 10, top = 20, right = 110, bottom = 120)
        assertEquals(100, rect.width)
        assertEquals(100, rect.height)

        val entity = DetectedEntity(
            type = EntityType.DATE_TIME,
            rawValue = "20 September 2026",
            normalizedValue = "2026-09-20",
            boundingBox = rect,
            confidence = 0.98f
        )

        val textBlock = TextBlock(
            text = "AI Workshop\n20 September 2026",
            boundingBox = rect,
            confidence = 0.95f
        )

        val content = ScreenContent(
            captureId = "capture-123",
            rawFullText = "AI Workshop\n20 September 2026",
            textBlocks = listOf(textBlock),
            detectedEntities = listOf(entity),
            extractionDurationMs = 120L
        )

        assertEquals("capture-123", content.captureId)
        assertEquals(1, content.detectedEntities.size)
        assertEquals(EntityType.DATE_TIME, content.detectedEntities.first().type)
        assertEquals(120L, content.extractionDurationMs)
    }

    @Test
    fun testContentClassificationAndAlias() {
        val classification = ContentClassification(
            captureId = "cap-001",
            primaryCategory = ContentCategory.EVENT,
            secondaryCategories = listOf(ContentCategory.LOCATION),
            confidenceScore = 0.88f,
            summary = "AI Workshop flyer"
        )

        assertEquals(ContentCategory.EVENT, classification.primaryCategory)
        assertEquals(ContentCategory.EVENT, classification.type)
        assertEquals(ConfidenceLevel.HIGH, classification.confidenceLevel)
        assertEquals(0.88f, classification.confidence, 0.001f)

        // Verify typealias ContentType interoperability
        val contentType: ContentType = classification.primaryCategory
        assertEquals(ContentType.EVENT, contentType)
    }

    @Test
    fun testActionSuggestionAndConfidence() {
        val suggestion = ActionSuggestion(
            actionId = "action-cal-01",
            type = ActionType.ADD_TO_CALENDAR,
            title = "Add to Calendar",
            description = "AI Workshop at 10:00 AM",
            payload = mapOf("title" to "AI Workshop", "date" to "2026-09-20"),
            confidence = ActionConfidence(score = 0.92f, reason = "Explicit date & time found"),
            safetyLevel = ActionSafetyLevel.REQUIRE_CONFIRMATION,
            isPrimary = true
        )

        assertTrue(suggestion.isPrimary)
        assertTrue(suggestion.requiresConfirmation)
        assertEquals(ConfidenceLevel.HIGH, suggestion.confidence.level)
        assertEquals(ActionType.ADD_TO_CALENDAR, suggestion.type)
    }

    @Test
    fun testActionRequestAndResult() {
        val request = ActionRequest(
            actionId = "act-99",
            type = ActionType.OPEN_MAPS,
            parameters = mapOf("query" to "Seminar Hall 2"),
            confirmedByUser = true
        )

        assertTrue(request.confirmedByUser)
        assertEquals(ActionType.OPEN_MAPS, request.type)

        val result = ActionResult(
            actionId = request.actionId,
            success = true,
            executedIntentSummary = "Opened Google Maps with query: Seminar Hall 2"
        )

        assertTrue(result.success)
        assertNull(result.errorMessage)
    }

    @Test
    fun testScreenIqResultMonad() {
        val success: ScreenIqResult<String> = ScreenIqResult.Success("Data")
        assertTrue(success.isSuccess)
        assertFalse(success.isFailure)
        assertEquals("Data", success.getOrNull())
        assertEquals("Data", success.getOrThrow())

        var callbackExecuted = false
        success.onSuccess {
            callbackExecuted = true
            assertEquals("Data", it)
        }
        assertTrue(callbackExecuted)

        val failure: ScreenIqResult<String> = ScreenIqResult.Failure(RuntimeException("Failed"), "Network error")
        assertTrue(failure.isFailure)
        assertNull(failure.getOrNull())

        var errorCaught = false
        failure.onFailure { throwable, message ->
            errorCaught = true
            assertEquals("Network error", message)
            assertEquals("Failed", throwable.message)
        }
        assertTrue(errorCaught)
    }

    @Test
    fun testDispatcherProvider() {
        val provider = DefaultDispatcherProvider()
        assertNotNull(provider.main)
        assertNotNull(provider.io)
        assertNotNull(provider.default)
        assertNotNull(provider.unconfined)
    }

    @Test
    fun testConstants() {
        assertEquals("ScreenIQ", ScreenIqConstants.APP_NAME)
        assertEquals("com.screeniq", ScreenIqConstants.PACKAGE_NAME)
        assertEquals(4, ScreenIqConstants.REQUIRED_POINTER_COUNT)
    }
}
