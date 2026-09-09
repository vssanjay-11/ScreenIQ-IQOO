package com.screeniq.ui.overlay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.screeniq.core.model.ActionSafetyLevel
import com.screeniq.core.model.ActionSuggestion
import com.screeniq.core.model.ContentClassification
import com.screeniq.ui.components.ActionButtonVariant
import com.screeniq.ui.components.ConfidenceBadge
import com.screeniq.ui.components.EntityDetailCard
import com.screeniq.ui.components.PipelineStage
import com.screeniq.ui.components.PipelineStatusIndicator
import com.screeniq.ui.components.SafetyChip
import com.screeniq.ui.components.ScreenIQActionButton
import com.screeniq.ui.theme.BrandPrimary
import com.screeniq.ui.theme.OverlayScrim
import com.screeniq.ui.theme.ScreenIQShapes
import com.screeniq.ui.theme.SuccessGreen

@Composable
fun ScreenIQOverlay(
    state: OverlayState,
    onActionSelected: (ActionSuggestion) -> Unit,
    onConfirmAction: (ActionSuggestion) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isVisible = state !is OverlayState.Hidden

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(OverlayScrim)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.BottomCenter
        ) {
            when (state) {
                is OverlayState.Processing -> {
                    // True Full-screen high-tech laser scanning animation
                    ScreenScanAnimationOverlay()
                }
                is OverlayState.SuggestionsReady -> {
                    // Floating pop-up window in the center of the screen
                    FloatingProcessResultWindow(
                        classification = state.classification,
                        suggestions = state.suggestions,
                        elapsedMs = state.elapsedMs,
                        onActionSelected = { action ->
                            if (action.safetyLevel == ActionSafetyLevel.SAFE_AUTO) {
                                onActionSelected(action)
                            } else {
                                onConfirmAction(action)
                            }
                        },
                        onDismiss = onDismiss
                    )
                }
                else -> {
                    AnimatedVisibility(
                        visible = isVisible,
                        enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { /* Consume clicks inside panel */ }
                        )
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                                .navigationBarsPadding()
                                .imePadding(),
                            shape = ScreenIQShapes.extraLarge,
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 8.dp,
                            shadowElevation = 16.dp
                        ) {
                            when (state) {
                                is OverlayState.ConfirmingAction -> {
                                    ConfirmationCard(
                                        action = state.action,
                                        classification = state.classification,
                                        onConfirm = { onActionSelected(state.action) },
                                        onCancel = onDismiss
                                    )
                                }
                                is OverlayState.Executing -> {
                                    ExecutingCard(
                                        action = state.action,
                                        onCancel = onDismiss
                                    )
                                }
                                is OverlayState.ExecutionComplete -> {
                                    ExecutionCompleteCard(
                                        state = state,
                                        onDismiss = onDismiss
                                    )
                                }
                                is OverlayState.Error -> {
                                    ErrorCard(
                                        message = state.message,
                                        onDismiss = onDismiss
                                    )
                                }
                                else -> Unit
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Rapid Processing State Card
 * ("Capturing..." -> "Understanding..." -> "Action ready...")
 */
@Composable
private fun ProcessingCard(
    stage: PipelineStage,
    elapsedMs: Long,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "ScreenIQ",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            IconButton(onClick = onCancel, modifier = Modifier.size(28.dp)) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cancel",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        PipelineStatusIndicator(
            stage = stage,
            elapsedMs = elapsedMs
        )

        Text(
            text = when (stage) {
                PipelineStage.CAPTURING -> "Scanning current screen in-memory..."
                PipelineStage.UNDERSTANDING -> "Understanding content & context..."
                PipelineStage.ACTION_READY -> "Action ready!"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        ScreenIQActionButton(
            text = "Cancel",
            onClick = onCancel,
            variant = ActionButtonVariant.GHOST
        )
    }
}

/**
 * Suggestions View (matches the exact prompt specification layout)
 */
@Composable
private fun SuggestionsCard(
    classification: ContentClassification,
    suggestions: List<ActionSuggestion>,
    elapsedMs: Long,
    onActionClicked: (ActionSuggestion) -> Unit,
    onCancel: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top Brand & Pipeline Status
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(BrandPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "IQ",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                        color = Color.White,
                        fontSize = 10.sp
                    )
                }
                Text(
                    text = "ScreenIQ",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            PipelineStatusIndicator(
                stage = PipelineStage.ACTION_READY,
                elapsedMs = elapsedMs
            )
        }

        // Entity Detail Summary Card (e.g. 🧠 EVENT DETECTED, AI Workshop...)
        EntityDetailCard(
            classification = classification
        )

        // "What would you like to do?" Section Heading
        Text(
            text = "What would you like to do?",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
        )

        // Suggested Actions Buttons
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            suggestions.forEachIndexed { index, suggestion ->
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val variant = if (suggestion.isPrimary || index == 0) {
                        ActionButtonVariant.PRIMARY
                    } else {
                        ActionButtonVariant.SECONDARY
                    }

                    ScreenIQActionButton(
                        text = suggestion.title,
                        onClick = { onActionClicked(suggestion) },
                        variant = variant
                    )

                    // Sub-info row with confidence and safety rating
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = suggestion.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            modifier = Modifier.weight(1f, fill = false)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SafetyChip(safetyLevel = suggestion.safetyLevel)
                            ConfidenceBadge(confidence = suggestion.confidence)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Cancel Button
        ScreenIQActionButton(
            text = "Cancel",
            onClick = onCancel,
            variant = ActionButtonVariant.GHOST
        )
    }
}

/**
 * Two-Step Review / Confirmation Card for Sensitive Actions
 */
@Composable
private fun ConfirmationCard(
    action: ActionSuggestion,
    classification: ContentClassification,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Review Action",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            SafetyChip(safetyLevel = action.safetyLevel)
        }

        Text(
            text = action.title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.primary
        )

        // Summary of parameters being dispatched
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = ScreenIQShapes.medium,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                action.payload.forEach { (key, value) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = key.replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = value,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.End,
                            modifier = Modifier.fillMaxWidth(0.7f)
                        )
                    }
                }
            }
        }

        Text(
            text = "Confirming will execute this action via Android system services.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        ScreenIQActionButton(
            text = "Confirm & Proceed",
            onClick = onConfirm,
            variant = ActionButtonVariant.PRIMARY
        )

        ScreenIQActionButton(
            text = "Back",
            onClick = onCancel,
            variant = ActionButtonVariant.GHOST
        )
    }
}

/**
 * Action Executing View
 */
@Composable
private fun ExecutingCard(
    action: ActionSuggestion,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(36.dp),
            strokeWidth = 3.dp,
            color = BrandPrimary
        )

        Text(
            text = "Executing Action...",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = action.title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        ScreenIQActionButton(
            text = "Dismiss",
            onClick = onCancel,
            variant = ActionButtonVariant.GHOST
        )
    }
}

/**
 * Execution Complete View
 */
@Composable
private fun ExecutionCompleteCard(
    state: OverlayState.ExecutionComplete,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = "Success",
            tint = SuccessGreen,
            modifier = Modifier.size(44.dp)
        )

        Text(
            text = "Action Completed",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = state.result.executedIntentSummary,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        ScreenIQActionButton(
            text = "Done",
            onClick = onDismiss,
            variant = ActionButtonVariant.PRIMARY
        )
    }
}

/**
 * Error View
 */
@Composable
private fun ErrorCard(
    message: String,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Notice",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.error
        )

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        ScreenIQActionButton(
            text = "Dismiss",
            onClick = onDismiss,
            variant = ActionButtonVariant.SECONDARY
        )
    }
}
