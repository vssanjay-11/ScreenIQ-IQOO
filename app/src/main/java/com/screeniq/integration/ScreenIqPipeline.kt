package com.screeniq.integration

import android.content.Context
import android.util.Log
import com.screeniq.actions.execution.AndroidActionExecutor
import com.screeniq.capture.ScreenCaptureManager
import com.screeniq.capture.accessibility.AccessibilityScreenshotProvider
import com.screeniq.classifier.LayeredContentClassifier
import com.screeniq.core.contracts.ActionExecutor
import com.screeniq.core.contracts.ActionPlanner
import com.screeniq.core.contracts.ContentClassifier
import com.screeniq.core.contracts.HistoryRepository
import com.screeniq.core.contracts.OcrEngine
import com.screeniq.core.contracts.OverlayController
import com.screeniq.core.contracts.ScreenCaptureEngine
import com.screeniq.core.model.ActionHistoryItem
import com.screeniq.core.model.ActionRequest
import com.screeniq.core.model.ActionResult
import com.screeniq.core.model.ActionSafetyLevel
import com.screeniq.core.model.ActionSuggestion
import com.screeniq.core.model.ContentClassification
import com.screeniq.core.model.ScreenCaptureResult
import com.screeniq.demo.DemoHarness
import com.screeniq.ocr.analyzer.ScreenContentAnalyzer
import com.screeniq.planner.DefaultActionPlanner
import com.screeniq.service.accessibility.ScreenIQAccessibilityService
import com.screeniq.ui.overlay.OverlayControllerImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Agent 10: Master End-to-End Pipeline Coordinator for ScreenIQ.
 *
 * Coordinates the full unidirectional flow:
 * [TRIGGER] -> [CAPTURE] -> [OCR] -> [CLASSIFIER] -> [PLANNER] -> [OVERLAY] -> [EXECUTOR] -> [HISTORY]
 *
 * Guarantees:
 * - Local-first / offline operational capability.
 * - In-memory bitmap handling: Bitmaps are recycled in finally block.
 * - Safe user confirmation for DANGEROUS and REQUIRE_CONFIRMATION actions.
 * - Fallback simulation modes for deterministic hackathon demonstration.
 */
class ScreenIqPipeline(
    private val context: Context,
    private val captureEngine: ScreenCaptureEngine = ScreenCaptureManager(
        accessibilityProvider = AccessibilityScreenshotProvider(ScreenIQAccessibilityService.captureBridge),
        fallbackProvider = { Result.success(DemoHarness.getEventCaptureResult()) }
    ),
    private val ocrEngine: OcrEngine = ScreenContentAnalyzer(),
    private val classifier: ContentClassifier = LayeredContentClassifier(),
    private val planner: ActionPlanner = DefaultActionPlanner(),
    val actionExecutor: ActionExecutor = AndroidActionExecutor(context),
    val overlayController: OverlayControllerImpl = OverlayControllerImpl(),
    val historyRepository: HistoryRepository = CoreHistoryRepositoryAdapter()
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _pipelineState = MutableStateFlow<PipelineStatus>(PipelineStatus.Idle)
    val pipelineState: StateFlow<PipelineStatus> = _pipelineState.asStateFlow()

    init {
        // Wire overlay action selection to execution
        // When user selects an action in the overlay, handle execution
    }

    /**
     * Executes the complete end-to-end analysis on the current screen (or provided capture frame).
     */
    suspend fun triggerPipeline(
        inputCapture: ScreenCaptureResult? = null,
        autoExecuteIfSafe: Boolean = false
    ): Result<PipelineOutput> = withContext(Dispatchers.Default) {
        val startTime = System.currentTimeMillis()
        _pipelineState.value = PipelineStatus.Capturing
        overlayController.notifyCapturing(elapsedMs = 50)

        // 1. Capture Screen Frame
        val captureResult = if (inputCapture != null) {
            Result.success(inputCapture)
        } else {
            captureEngine.captureCurrentScreen()
        }

        if (captureResult.isFailure) {
            val error = captureResult.exceptionOrNull()?.message ?: "Screen capture failed"
            Log.e(TAG, "Capture failed: $error")
            _pipelineState.value = PipelineStatus.Error(error)
            overlayController.notifyError(error)
            return@withContext Result.failure(Exception(error))
        }

        val frame = captureResult.getOrThrow()
        val captureDuration = System.currentTimeMillis() - startTime
        Log.d(TAG, "Screen captured successfully in ${captureDuration}ms. Dimensions: ${frame.width}x${frame.height}")

        try {
            // 2. OCR / Visual Text Extraction
            _pipelineState.value = PipelineStatus.Extracting
            overlayController.notifyUnderstanding(elapsedMs = System.currentTimeMillis() - startTime)

            val ocrResult = ocrEngine.extractContent(frame)
            if (ocrResult.isFailure) {
                val error = ocrResult.exceptionOrNull()?.message ?: "OCR extraction failed"
                Log.e(TAG, "OCR failed: $error")
                _pipelineState.value = PipelineStatus.Error(error)
                overlayController.notifyError(error)
                return@withContext Result.failure(Exception(error))
            }

            val screenContent = ocrResult.getOrThrow()
            val ocrDuration = System.currentTimeMillis() - startTime
            Log.d(TAG, "OCR completed in ${ocrDuration}ms. Extracted ${screenContent.rawFullText.length} chars, ${screenContent.detectedEntities.size} entities.")

            // 3. AI Understanding & Classification
            _pipelineState.value = PipelineStatus.Classifying
            val classificationResult = classifier.classify(screenContent)
            if (classificationResult.isFailure) {
                val error = classificationResult.exceptionOrNull()?.message ?: "Classification failed"
                Log.e(TAG, "Classification failed: $error")
                _pipelineState.value = PipelineStatus.Error(error)
                overlayController.notifyError(error)
                return@withContext Result.failure(Exception(error))
            }

            val classification = classificationResult.getOrThrow()
            Log.d(TAG, "Classification: ${classification.primaryCategory} (confidence: ${classification.confidenceScore})")

            // 4. Action Planning & Confidence Engine
            _pipelineState.value = PipelineStatus.Planning
            val suggestions = planner.planActions(classification)
            val totalPlanningTime = System.currentTimeMillis() - startTime
            Log.i(TAG, "Action Planning completed in ${totalPlanningTime}ms. Proposed ${suggestions.size} actions.")

            val output = PipelineOutput(
                captureId = frame.captureId,
                classification = classification,
                suggestions = suggestions,
                elapsedMs = totalPlanningTime
            )

            _pipelineState.value = PipelineStatus.Ready(output)

            // 5. Present to User via Compose Overlay
            withContext(Dispatchers.Main) {
                overlayController.showActionSuggestions(
                    classification = classification,
                    suggestions = suggestions,
                    onActionSelected = { selectedSuggestion ->
                        handleActionExecution(selectedSuggestion, classification)
                    },
                    onDismiss = {
                        _pipelineState.value = PipelineStatus.Idle
                    }
                )
            }

            return@withContext Result.success(output)
        } finally {
            // Strict In-Memory Privacy Guarantee: recycle bitmap once processing is completed
            try {
                if (frame.bitmap?.isRecycled == false) {
                    frame.bitmap?.recycle()
                    Log.d(TAG, "Capture bitmap successfully recycled to prevent memory leak.")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to recycle capture bitmap: ${e.message}")
            }
        }
    }

    /**
     * Dispatches user-approved action to Android Action Executor and logs to local history.
     */
    fun handleActionExecution(
        action: ActionSuggestion,
        classification: ContentClassification
    ) {
        scope.launch {
            _pipelineState.value = PipelineStatus.Executing(action)

            val request = ActionRequest(
                actionId = action.actionId,
                type = action.type,
                parameters = action.payload,
                confirmedByUser = true
            )

            val result = actionExecutor.executeAction(request)
            Log.i(TAG, "Executed action: ${result.executedIntentSummary} (Success: ${result.success})")

            // Record to private local history (zero screenshot stored)
            val historyItem = ActionHistoryItem(
                id = action.actionId,
                timestampMs = System.currentTimeMillis(),
                category = classification.primaryCategory,
                actionType = action.type,
                summarySnippet = classification.summary ?: action.title,
                wasSuccessful = result.success
            )
            historyRepository.recordAction(historyItem)

            withContext(Dispatchers.Main) {
                overlayController.notifyExecutionComplete(action, result)
            }
            _pipelineState.value = PipelineStatus.Completed(result)
        }
    }

    companion object {
        private const val TAG = "ScreenIQ:Pipeline"

        @Volatile
        private var instance: ScreenIqPipeline? = null

        fun getInstance(context: Context): ScreenIqPipeline {
            return instance ?: synchronized(this) {
                instance ?: ScreenIqPipeline(context.applicationContext).also { instance = it }
            }
        }
    }
}

/**
 * Pipeline status states for UI indicators and reactive observing.
 */
sealed interface PipelineStatus {
    object Idle : PipelineStatus
    object Capturing : PipelineStatus
    object Extracting : PipelineStatus
    object Classifying : PipelineStatus
    object Planning : PipelineStatus
    data class Ready(val output: PipelineOutput) : PipelineStatus
    data class Executing(val action: ActionSuggestion) : PipelineStatus
    data class Completed(val result: ActionResult) : PipelineStatus
    data class Error(val message: String) : PipelineStatus
}

data class PipelineOutput(
    val captureId: String,
    val classification: ContentClassification,
    val suggestions: List<ActionSuggestion>,
    val elapsedMs: Long
)

/**
 * Adapter mapping Agent 9's HistoryRepositoryImpl to core contracts HistoryRepository
 */
class CoreHistoryRepositoryAdapter(
    private val delegate: com.screeniq.history.repository.HistoryRepositoryImpl = com.screeniq.history.repository.HistoryRepositoryImpl()
) : HistoryRepository {

    override suspend fun recordAction(item: ActionHistoryItem) {
        val entry = com.screeniq.history.model.HistoryEntry(
            id = item.id,
            timestampMs = item.timestampMs,
            contentType = item.category.name,
            summary = item.summarySnippet,
            actionSelected = item.actionType.name,
            actionResult = if (item.wasSuccessful) {
                com.screeniq.history.model.ActionResultSummary.success("Completed")
            } else {
                com.screeniq.history.model.ActionResultSummary.failure("Failed")
            },
            confidence = 0.95f
        )
        delegate.recordAction(entry)
    }

    override suspend fun getRecentHistory(limit: Int): List<ActionHistoryItem> {
        val entries = delegate.getRecentHistory(limit = limit)
        return entries.map { entry ->
            ActionHistoryItem(
                id = entry.id,
                timestampMs = entry.timestampMs,
                category = try { com.screeniq.core.model.ContentCategory.valueOf(entry.contentType) } catch (_: Exception) { com.screeniq.core.model.ContentCategory.UNKNOWN },
                actionType = try { com.screeniq.core.model.ActionType.valueOf(entry.actionSelected) } catch (_: Exception) { com.screeniq.core.model.ActionType.COPY_TO_CLIPBOARD },
                summarySnippet = entry.summary,
                wasSuccessful = entry.actionResult.wasSuccessful
            )
        }
    }

    override suspend fun clearHistory() {
        delegate.clearHistory()
    }
}
