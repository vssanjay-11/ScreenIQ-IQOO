package com.screeniq.ui.components

import androidx.compose.foundation.background
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
import com.screeniq.core.model.ActionSafetyLevel
import com.screeniq.ui.theme.DangerRed
import com.screeniq.ui.theme.DangerRedBg
import com.screeniq.ui.theme.SuccessGreen
import com.screeniq.ui.theme.SuccessGreenBg
import com.screeniq.ui.theme.WarningAmber
import com.screeniq.ui.theme.WarningAmberBg

@Composable
fun SafetyChip(
    safetyLevel: ActionSafetyLevel,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, label) = when (safetyLevel) {
        ActionSafetyLevel.SAFE_AUTO -> Triple(
            SuccessGreenBg.copy(alpha = 0.35f),
            SuccessGreen,
            "⚡ Safe Direct"
        )
        ActionSafetyLevel.REQUIRE_CONFIRMATION -> Triple(
            WarningAmberBg.copy(alpha = 0.35f),
            WarningAmber,
            "🛡️ Review First"
        )
        ActionSafetyLevel.DANGEROUS -> Triple(
            DangerRedBg.copy(alpha = 0.35f),
            DangerRed,
            "⚠️ Sensitive"
        )
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = textColor
        )
    }
}
