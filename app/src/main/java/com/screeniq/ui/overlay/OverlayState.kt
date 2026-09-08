package com.screeniq.ui.overlay

import com.screeniq.core.model.ActionResult
import com.screeniq.core.model.ActionSuggestion
import com.screeniq.core.model.ContentClassification
import com.screeniq.ui.components.PipelineStage

sealed interface OverlayState {
    object Hidden : OverlayState

    data class Processing(
        val stage: PipelineStage,
        val elapsedMs: Long = 0L
    ) : OverlayState

    data class SuggestionsReady(
        val classification: ContentClassification,
        val suggestions: List<ActionSuggestion>,
        val elapsedMs: Long = 0L
    ) : OverlayState

    data class ConfirmingAction(
        val action: ActionSuggestion,
        val classification: ContentClassification
    ) : OverlayState

    data class Executing(
        val action: ActionSuggestion
    ) : OverlayState

    data class ExecutionComplete(
        val action: ActionSuggestion,
        val result: ActionResult
    ) : OverlayState

    data class Error(
        val message: String
    ) : OverlayState
}
