package com.screeniq.integration

import com.screeniq.classifier.LayeredContentClassifier
import com.screeniq.core.contracts.ActionExecutor
import com.screeniq.core.model.ActionRequest
import com.screeniq.core.model.ActionResult
import com.screeniq.core.model.ActionType
import com.screeniq.core.model.ContentCategory
import com.screeniq.demo.DemoHarness
import com.screeniq.ocr.analyzer.ScreenContentAnalyzer
import com.screeniq.ocr.engine.TextRecognizerDelegate
import com.screeniq.planner.ActionParameters
import com.screeniq.planner.DefaultActionPlanner
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Agent 10: End-to-End Pipeline & Action Integration Test
 */
class ScreenIqPipelineIntegrationTest {

    @Test
    fun testEventEndToEndPipeline() = runBlocking {
        val ocr = ScreenContentAnalyzer(textRecognizer = object : TextRecognizerDelegate {
            override suspend fun recognize(bitmap: android.graphics.Bitmap?): List<com.screeniq.core.model.TextBlock> {
                return listOf(
                    com.screeniq.core.model.TextBlock(DemoHarness.EVENT_TEXT, null, 0.95f)
                )
            }
        })

        val classifier = LayeredContentClassifier()
        val planner = DefaultActionPlanner()

        val executedRequests = mutableListOf<ActionRequest>()
        val mockExecutor = object : ActionExecutor {
            override suspend fun executeAction(request: ActionRequest): ActionResult {
                executedRequests.add(request)
                return ActionResult(request.actionId, true, "Executed ${request.type}")
            }
        }

        val dummyCapture = DemoHarness.createCaptureResult(DemoHarness.EVENT_TEXT)

        // 1. OCR Extraction
        val content = ocr.extractContent(dummyCapture).getOrThrow()
        assertTrue("Content should contain AI Workshop", content.rawFullText.contains("AI Workshop"))

        // 2. Classification
        val classification = classifier.classify(content).getOrThrow()
        assertEquals(ContentCategory.EVENT, classification.primaryCategory)
        assertTrue("Confidence should be high", classification.confidenceScore >= 0.7f)

        // 3. Action Planning
        val suggestions = planner.planActions(classification)
        assertTrue("Suggestions should not be empty", suggestions.isNotEmpty())

        val primaryAction = suggestions.firstOrNull { it.isPrimary } ?: suggestions.first()
        assertEquals(ActionType.ADD_TO_CALENDAR, primaryAction.type)

        // 4. Execution
        val req = ActionRequest(
            actionId = primaryAction.actionId,
            type = primaryAction.type,
            parameters = primaryAction.payload,
            confirmedByUser = true
        )
        val result = mockExecutor.executeAction(req)
        assertTrue("Execution should succeed", result.success)
        assertEquals(1, executedRequests.size)
        assertTrue(executedRequests.first().parameters[ActionParameters.EVENT_TITLE]?.startsWith("AI Workshop") == true)
    }

    @Test
    fun testLocationEndToEndPipeline() = runBlocking {
        val ocr = ScreenContentAnalyzer(textRecognizer = object : TextRecognizerDelegate {
            override suspend fun recognize(bitmap: android.graphics.Bitmap?): List<com.screeniq.core.model.TextBlock> {
                return listOf(
                    com.screeniq.core.model.TextBlock(DemoHarness.LOCATION_TEXT, null, 0.95f)
                )
            }
        })

        val classifier = LayeredContentClassifier()
        val planner = DefaultActionPlanner()

        val dummyCapture = DemoHarness.createCaptureResult(DemoHarness.LOCATION_TEXT)
        val content = ocr.extractContent(dummyCapture).getOrThrow()
        val classification = classifier.classify(content).getOrThrow()

        assertEquals(ContentCategory.LOCATION, classification.primaryCategory)

        val suggestions = planner.planActions(classification)
        val primary = suggestions.firstOrNull { it.isPrimary } ?: suggestions.first()
        assertEquals(ActionType.OPEN_MAPS, primary.type)
        assertTrue(primary.payload.containsKey(ActionParameters.MAP_QUERY) || primary.payload.containsKey(ActionParameters.MAP_ADDRESS))
    }

    @Test
    fun testContactPhoneEndToEndPipeline() = runBlocking {
        val ocr = ScreenContentAnalyzer(textRecognizer = object : TextRecognizerDelegate {
            override suspend fun recognize(bitmap: android.graphics.Bitmap?): List<com.screeniq.core.model.TextBlock> {
                return listOf(
                    com.screeniq.core.model.TextBlock(DemoHarness.CONTACT_TEXT, null, 0.95f)
                )
            }
        })

        val classifier = LayeredContentClassifier()
        val planner = DefaultActionPlanner()

        val dummyCapture = DemoHarness.createCaptureResult(DemoHarness.CONTACT_TEXT)
        val content = ocr.extractContent(dummyCapture).getOrThrow()
        val classification = classifier.classify(content).getOrThrow()

        assertTrue(
            classification.primaryCategory == ContentCategory.PHONE ||
            classification.primaryCategory == ContentCategory.CONTACT ||
            classification.secondaryCategories.contains(ContentCategory.PHONE)
        )

        val suggestions = planner.planActions(classification)
        val dialSuggestion = suggestions.find { it.type == ActionType.DIAL_PHONE }
        assertNotNull("Dial action should be suggested", dialSuggestion)
        val phone = dialSuggestion!!.payload[ActionParameters.PHONE_NUMBER]
        assertTrue("Phone number should match", phone?.contains("9876543210") == true || phone?.contains("98765 43210") == true)
    }
}
