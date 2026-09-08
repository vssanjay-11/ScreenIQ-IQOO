package com.screeniq.core.contracts

import com.screeniq.core.model.ActionSuggestion
import com.screeniq.core.model.ContentClassification

/**
 * Agent 8: UI Overlay Controller
 * Manages the floating system overlay and presents action suggestion cards to the user.
 */
interface OverlayController {
    fun showActionSuggestions(
        classification: ContentClassification,
        suggestions: List<ActionSuggestion>,
        onActionSelected: (ActionSuggestion) -> Unit,
        onDismiss: () -> Unit
    )
    fun dismissOverlay()
}
