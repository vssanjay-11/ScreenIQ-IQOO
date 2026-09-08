package com.screeniq.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.screeniq.core.model.ActionConfidence
import com.screeniq.ui.theme.SuccessGreen
import com.screeniq.ui.theme.SuccessGreenBg
import com.screeniq.ui.theme.WarningAmber
import com.screeniq.ui.theme.WarningAmberBg
import kotlin.math.roundToInt

@Composable
fun ConfidenceBadge(
    confidence: ActionConfidence,
    modifier: Modifier = Modifier
) {
    val score = confidence.score
    val percentage = (score * 100).roundToInt()

    val (bgColor, textColor, label) = when {
        score >= 0.85f -> Triple(SuccessGreenBg.copy(alpha = 0.35f), SuccessGreen, "High")
        score >= 0.65f -> Triple(WarningAmberBg.copy(alpha = 0.35f), WarningAmber, "Good")
        else -> Triple(Color(0xFF374151), Color(0xFF9CA3AF), "Moderate")
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "$percentage% $label",
            style = MaterialTheme.typography.labelSmall,
            color = textColor
        )
    }
}
