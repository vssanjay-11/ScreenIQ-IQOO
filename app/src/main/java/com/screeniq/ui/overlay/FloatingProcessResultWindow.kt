package com.screeniq.ui.overlay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.screeniq.core.model.ActionSuggestion
import com.screeniq.core.model.ContentClassification
import com.screeniq.ui.components.ActionButtonVariant
import com.screeniq.ui.components.ConfidenceBadge
import com.screeniq.ui.components.EntityDetailCard
import com.screeniq.ui.components.ScreenIQActionButton
import com.screeniq.ui.theme.BrandPrimary
import com.screeniq.ui.theme.BrandSecondary
import com.screeniq.ui.theme.DarkBorder
import com.screeniq.ui.theme.DarkSurface
import com.screeniq.ui.theme.DarkSurfaceElevated
import com.screeniq.ui.theme.OverlayScrim
import com.screeniq.ui.theme.SuccessGreen

/**
 * Floating Pop-up Window that explicitly lists what background processes ScreenIQ
 * has performed and displays the generated actionable options.
 */
@Composable
fun FloatingProcessResultWindow(
    classification: ContentClassification,
    suggestions: List<ActionSuggestion>,
    elapsedMs: Long,
    onActionSelected: (ActionSuggestion) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(OverlayScrim)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            ),
        contentAlignment = Alignment.Center
    ) {
        // Floating Card Modal
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 24.dp)
                .navigationBarsPadding()
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        listOf(Color(0xFF38BDF8).copy(alpha = 0.6f), Color(0xFF6366F1).copy(alpha = 0.2f))
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { /* Consume clicks inside modal */ }
                ),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 10.dp,
            shadowElevation = 20.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Bar: Title & Dismiss
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(listOf(BrandPrimary, BrandSecondary))
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("IQ", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        Column {
                            Text(
                                text = "ScreenIQ Intelligence",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Analysis completed in ${elapsedMs}ms",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Classification Badge Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    ) {
                        Text(
                            text = classification.primaryCategory.name.uppercase(),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    ConfidenceBadge(
                        confidence = com.screeniq.core.model.ActionConfidence(
                            score = classification.confidenceScore,
                            reason = "ML Kit Classification"
                        )
                    )
                }

                // Process Checklist Section ("What ScreenIQ Has Done")
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "COMPLETED BACKGROUND PROCESSES",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 0.8.sp
                        )

                        // Step 1: Screen Captured
                        ProcessStepItem(
                            stepNumber = "1",
                            title = "Screen Captured",
                            detail = "In-memory frame captured (zero disk persistence)"
                        )

                        // Step 2: OCR Extraction
                        ProcessStepItem(
                            stepNumber = "2",
                            title = "OCR & Visual Extraction",
                            detail = "Google ML Kit extracted text tokens & spatial clusters"
                        )

                        // Step 3: AI Content Understanding
                        ProcessStepItem(
                            stepNumber = "3",
                            title = "AI Classification & Confidence",
                            detail = "Classified as ${classification.primaryCategory.name} (${(classification.confidenceScore * 100).toInt()}% confidence)"
                        )

                        // Step 4: Action Planning
                        ProcessStepItem(
                            stepNumber = "4",
                            title = "Action Synthesis",
                            detail = "Generated ${suggestions.size} smart action recommendations"
                        )
                    }
                }

                // Extracted Entities / Summary Card
                EntityDetailCard(
                    classification = classification,
                    modifier = Modifier.fillMaxWidth()
                )

                // Actionable Buttons Section
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "AVAILABLE ACTIONS",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 0.8.sp
                    )

                    // Primary Suggested Action (High Visual Prominence)
                    suggestions.firstOrNull()?.let { primaryAction ->
                        ScreenIQActionButton(
                            text = primaryAction.title,
                            onClick = { onActionSelected(primaryAction) },
                            variant = ActionButtonVariant.PRIMARY,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Secondary Actions
                    suggestions.drop(1).take(2).forEach { secondaryAction ->
                        ScreenIQActionButton(
                            text = secondaryAction.title,
                            onClick = { onActionSelected(secondaryAction) },
                            variant = ActionButtonVariant.SECONDARY,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    ScreenIQActionButton(
                        text = "Dismiss",
                        onClick = onDismiss,
                        variant = ActionButtonVariant.GHOST,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun ProcessStepItem(
    stepNumber: String,
    title: String,
    detail: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(SuccessGreen.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = SuccessGreen,
                modifier = Modifier.size(13.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = detail,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
