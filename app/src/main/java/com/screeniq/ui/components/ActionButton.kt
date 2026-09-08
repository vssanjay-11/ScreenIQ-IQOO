package com.screeniq.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.screeniq.ui.theme.BrandPrimary
import com.screeniq.ui.theme.DangerRed
import com.screeniq.ui.theme.ScreenIQShapes

enum class ActionButtonVariant {
    PRIMARY,
    SECONDARY,
    OUTLINED,
    GHOST,
    DANGER
}

@Composable
fun ScreenIQActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ActionButtonVariant = ActionButtonVariant.PRIMARY,
    leadingIcon: (@Composable () -> Unit)? = null,
    enabled: Boolean = true
) {
    val buttonModifier = modifier
        .fillMaxWidth()
        .height(52.dp)

    when (variant) {
        ActionButtonVariant.PRIMARY -> {
            Button(
                onClick = onClick,
                modifier = buttonModifier,
                enabled = enabled,
                shape = ScreenIQShapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrandPrimary,
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(horizontal = 20.dp)
            ) {
                ButtonContent(text = text, leadingIcon = leadingIcon)
            }
        }
        ActionButtonVariant.SECONDARY -> {
            Button(
                onClick = onClick,
                modifier = buttonModifier,
                enabled = enabled,
                shape = ScreenIQShapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                contentPadding = PaddingValues(horizontal = 20.dp)
            ) {
                ButtonContent(text = text, leadingIcon = leadingIcon)
            }
        }
        ActionButtonVariant.OUTLINED -> {
            OutlinedButton(
                onClick = onClick,
                modifier = buttonModifier,
                enabled = enabled,
                shape = ScreenIQShapes.medium,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                contentPadding = PaddingValues(horizontal = 20.dp)
            ) {
                ButtonContent(text = text, leadingIcon = leadingIcon)
            }
        }
        ActionButtonVariant.GHOST -> {
            TextButton(
                onClick = onClick,
                modifier = buttonModifier,
                enabled = enabled,
                shape = ScreenIQShapes.medium,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                contentPadding = PaddingValues(horizontal = 20.dp)
            ) {
                ButtonContent(text = text, leadingIcon = leadingIcon)
            }
        }
        ActionButtonVariant.DANGER -> {
            Button(
                onClick = onClick,
                modifier = buttonModifier,
                enabled = enabled,
                shape = ScreenIQShapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = DangerRed,
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(horizontal = 20.dp)
            ) {
                ButtonContent(text = text, leadingIcon = leadingIcon)
            }
        }
    }
}

@Composable
private fun RowScope.ButtonContent(
    text: String,
    leadingIcon: (@Composable () -> Unit)?
) {
    if (leadingIcon != null) {
        leadingIcon()
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.androidx.compose.foundation.layout.width(8.dp))
    }
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
    )
}
