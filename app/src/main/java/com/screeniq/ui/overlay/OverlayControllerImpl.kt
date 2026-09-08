package com.screeniq.ui.overlay

import com.screeniq.core.contracts.OverlayController
import com.screeniq.core.model.ActionResult
import com.screeniq.core.model.ActionSuggestion
import com.screeniq.core.model.ContentClassification
import com.screeniq.ui.components.PipelineStage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Agent 8 Implementation of OverlayController.
 * Coordinates UI overlay state between system triggers, pipeline progression, and user interactions.
 */
class OverlayControllerImpl : OverlayController {

    private val _overlayState = MutableStateFlow<OverlayState>(OverlayState.Hidden)
    val overlayState: StateFlow<OverlayState> = _overlayState.asStateFlow()

    private var actionSelectedCallback: ((ActionSuggestion) -> Unit)? = null
    private var dismissCallback: (() -> Unit)? = null

    override fun showActionSuggestions(
        classification: ContentClassification,
        suggestions: List<ActionSuggestion>,
        onActionSelected: (ActionSuggestion) -> Unit,
        onDismiss: () -> Unit
    ) {
        actionSelectedCallback = onActionSelected
        dismissCallback = onDismiss
        _overlayState.value = OverlayState.SuggestionsReady(
            classification = classification,
            suggestions = suggestions,
            elapsedMs = 420L
        )
    }

    override fun dismissOverlay() {
        _overlayState.value = OverlayState.Hidden
        dismissCallback?.invoke()
        actionSelectedCallback = null
        dismissCallback = null
    }

    /**
     * Fast visual state transitions for gesture trigger feedback
     */
    fun notifyCapturing(elapsedMs: Long = 80L) {
        _overlayState.value = OverlayState.Processing(
            stage = PipelineStage.CAPTURING,
            elapsedMs = elapsedMs
        )
    }

    fun notifyUnderstanding(elapsedMs: Long = 250L) {
        _overlayState.value = OverlayState.Processing(
            stage = PipelineStage.UNDERSTANDING,
            elapsedMs = elapsedMs
        )
    }

    fun requestConfirmation(action: ActionSuggestion, classification: ContentClassification) {
        _overlayState.value = OverlayState.ConfirmingAction(
            action = action,
            classification = classification
        )
    }

    fun notifyExecuting(action: ActionSuggestion) {
        _overlayState.value = OverlayState.Executing(action = action)
    }

    fun notifyExecutionComplete(action: ActionSuggestion, result: ActionResult) {
        _overlayState.value = OverlayState.ExecutionComplete(
            action = action,
            result = result
        )
    }

    fun notifyError(message: String) {
        _overlayState.value = OverlayState.Error(message = message)
    }

    fun handleActionSelected(action: ActionSuggestion) {
        actionSelectedCallback?.invoke(action)
    }
}
