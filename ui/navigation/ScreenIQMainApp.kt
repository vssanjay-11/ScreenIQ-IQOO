package com.screeniq.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.screeniq.core.contracts.ActionExecutor
import com.screeniq.core.contracts.HistoryRepository
import com.screeniq.core.model.ActionHistoryItem
import com.screeniq.core.model.ActionRequest
import com.screeniq.core.model.ActionResult
import com.screeniq.core.model.ActionSuggestion
import com.screeniq.core.model.ContentCategory
import com.screeniq.core.model.ContentClassification
import com.screeniq.ui.overlay.OverlayControllerImpl
import com.screeniq.ui.overlay.OverlayState
import com.screeniq.ui.overlay.ScreenIQOverlay
import com.screeniq.ui.preview.MockData
import com.screeniq.ui.screens.AboutScreen
import com.screeniq.ui.screens.ActionResultScreen
import com.screeniq.ui.screens.HistoryScreen
import com.screeniq.ui.screens.HomeScreen
import com.screeniq.ui.screens.SettingsScreen
import com.screeniq.ui.theme.ScreenIQTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ScreenIQMainApp(
    overlayController: OverlayControllerImpl = remember { OverlayControllerImpl() },
    actionExecutor: ActionExecutor? = null,
    historyRepository: HistoryRepository? = null,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var currentDestination by remember { mutableStateOf(ScreenIQDestination.HOME) }
    val overlayState by overlayController.overlayState.collectAsState()

    // Local in-memory history backing state for interactive UI
    val historyItems = remember { mutableStateListOf<ActionHistoryItem>().apply { addAll(MockData.sampleHistoryItems) } }

    // Optional inspector detail state
    var inspectingClassification by remember { mutableStateOf<ContentClassification?>(null) }
    var inspectingSuggestions by remember { mutableStateOf<List<ActionSuggestion>>(emptyList()) }

    ScreenIQTheme {
        Box(modifier = modifier.fillMaxSize()) {
            Scaffold(
                bottomBar = {
                    if (inspectingClassification == null) {
                        ScreenIQBottomNavigation(
                            currentDestination = currentDestination,
                            onNavigate = { destination ->
                                inspectingClassification = null
                                currentDestination = destination
                            }
                        )
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (inspectingClassification != null) {
                        ActionResultScreen(
                            classification = inspectingClassification!!,
                            suggestions = inspectingSuggestions,
                            onActionSelected = { suggestion ->
                                handleActionExecution(
                                    action = suggestion,
                                    classification = inspectingClassification!!,
                                    overlayController = overlayController,
                                    actionExecutor = actionExecutor,
                                    historyRepository = historyRepository,
                                    historyList = historyItems,
                                    scope = coroutineScope
                                )
                            },
                            onBack = { inspectingClassification = null }
                        )
                    } else {
                        when (currentDestination) {
                            ScreenIQDestination.HOME -> {
                                HomeScreen(
                                    onTriggerScenario = { category ->
                                        triggerFastGesturePipeline(
                                            category = category,
                                            controller = overlayController,
                                            scope = coroutineScope
                                        )
                                    },
                                    onNavigateToHistory = { currentDestination = ScreenIQDestination.HISTORY },
                                    onNavigateToSettings = { currentDestination = ScreenIQDestination.SETTINGS }
                                )
                            }
                            ScreenIQDestination.HISTORY -> {
                                HistoryScreen(
                                    historyItems = historyItems,
                                    onClearHistory = {
                                        historyItems.clear()
                                        coroutineScope.launch { historyRepository?.clearHistory() }
                                    }
                                )
                            }
                            ScreenIQDestination.SETTINGS -> {
                                SettingsScreen()
                            }
                            ScreenIQDestination.ABOUT -> {
                                AboutScreen()
                            }
                        }
                    }
                }
            }

            // Primary Floating Action / Result Overlay
            ScreenIQOverlay(
                state = overlayState,
                onActionSelected = { suggestion ->
                    val classification = (overlayState as? OverlayState.SuggestionsReady)?.classification
                        ?: (overlayState as? OverlayState.ConfirmingAction)?.classification
                        ?: MockData.sampleEventClassification

                    handleActionExecution(
                        action = suggestion,
                        classification = classification,
                        overlayController = overlayController,
                        actionExecutor = actionExecutor,
                        historyRepository = historyRepository,
                        historyList = historyItems,
                        scope = coroutineScope
                    )
                },
                onConfirmAction = { suggestion ->
                    val classification = (overlayState as? OverlayState.SuggestionsReady)?.classification
                        ?: MockData.sampleEventClassification
                    overlayController.requestConfirmation(suggestion, classification)
                },
                onDismiss = {
                    overlayController.dismissOverlay()
                }
            )
        }
    }
}

/**
 * Fast, non-blocking pipeline progression:
 * Capturing... (~70ms) -> Understanding... (~180ms) -> Action ready...
 */
private fun triggerFastGesturePipeline(
    category: ContentCategory,
    controller: OverlayControllerImpl,
    scope: kotlinx.coroutines.CoroutineScope
) {
    scope.launch {
        // Step 1: Capturing screen in-memory
        controller.notifyCapturing(elapsedMs = 75L)
        delay(75)

        // Step 2: OCR & On-Device Understanding
        controller.notifyUnderstanding(elapsedMs = 240L)
        delay(165)

        // Step 3: Present Action Result Panel
        val (classification, suggestions) = when (category) {
            ContentCategory.EVENT -> Pair(MockData.sampleEventClassification, MockData.sampleEventSuggestions)
            ContentCategory.LOCATION -> Pair(MockData.sampleLocationClassification, MockData.sampleLocationSuggestions)
            ContentCategory.PHONE -> Pair(MockData.sampleContactClassification, MockData.sampleContactSuggestions)
            else -> Pair(MockData.sampleEventClassification, MockData.sampleEventSuggestions)
        }

        controller.showActionSuggestions(
            classification = classification,
            suggestions = suggestions,
            onActionSelected = { /* handled via controller flow */ },
            onDismiss = { /* handled via controller flow */ }
        )
    }
}

/**
 * Executes action via ActionExecutor contract (or mock fallback) and records in HistoryRepository
 */
private fun handleActionExecution(
    action: ActionSuggestion,
    classification: ContentClassification,
    overlayController: OverlayControllerImpl,
    actionExecutor: ActionExecutor?,
    historyRepository: HistoryRepository?,
    historyList: MutableList<ActionHistoryItem>,
    scope: kotlinx.coroutines.CoroutineScope
) {
    scope.launch {
        overlayController.notifyExecuting(action)

        val request = ActionRequest(
            actionId = action.actionId,
            type = action.type,
            parameters = action.payload,
            confirmedByUser = true
        )

        val result = actionExecutor?.executeAction(request) ?: ActionResult(
            actionId = action.actionId,
            success = true,
            executedIntentSummary = "Dispatched native intent for '${action.title}'",
            timestampMs = System.currentTimeMillis()
        )

        // Record in history
        val historyItem = ActionHistoryItem(
            id = "hist_${System.currentTimeMillis()}",
            timestampMs = System.currentTimeMillis(),
            category = classification.primaryCategory,
            actionType = action.type,
            summarySnippet = "${action.title}: ${action.description}",
            wasSuccessful = result.success
        )
        historyList.add(0, historyItem)
        historyRepository?.recordAction(historyItem)

        overlayController.notifyExecutionComplete(action, result)
        delay(800)
        overlayController.dismissOverlay()
    }
}
