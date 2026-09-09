package com.screeniq.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.screeniq.ui.components.ScreenIQTopBar
import com.screeniq.ui.theme.BrandPrimary
import com.screeniq.ui.theme.ScreenIQShapes
import com.screeniq.ui.theme.SuccessGreen

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    var gestureEnabled by remember { mutableStateOf(true) }
    var autoExecuteSafe by remember { mutableStateOf(true) }
    var requireConfirmationForDangerous by remember { mutableStateOf(true) }
    var hapticFeedback by remember { mutableStateOf(true) }
    var telemetryOptIn by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        ScreenIQTopBar(title = "Settings")

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Privacy Verification Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = ScreenIQShapes.medium,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(SuccessGreen.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Privacy",
                            tint = SuccessGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Zero-Disk Privacy Guarantee",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Screenshots reside strictly in-memory during extraction and are recycled immediately. No bitmaps are saved to disk.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Section: Gesture & Trigger
            SettingsSectionHeader(title = "GESTURE & TRIGGER")
            SettingsCard {
                SettingsSwitchRow(
                    title = "4-Finger Swipe Detection",
                    subtitle = "System-wide touch gesture to invoke ScreenIQ",
                    checked = gestureEnabled,
                    onCheckedChange = { gestureEnabled = it }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                SettingsTextRow(
                    title = "Gesture Sensitivity",
                    subtitle = "Balanced (Default)",
                    trailingText = "Adjust"
                )
            }

            // Section: Action Execution
            SettingsSectionHeader(title = "ACTION AUTOMATION")
            SettingsCard {
                SettingsSwitchRow(
                    title = "Auto-execute Safe Actions",
                    subtitle = "Instantly open maps or browser links for SAFE_AUTO intents",
                    checked = autoExecuteSafe,
                    onCheckedChange = { autoExecuteSafe = it }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                SettingsSwitchRow(
                    title = "Always Confirm Sensitive Actions",
                    subtitle = "Require confirmation sheet before phone calls, messages, or payments",
                    checked = requireConfirmationForDangerous,
                    onCheckedChange = { requireConfirmationForDangerous = it }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                SettingsSwitchRow(
                    title = "Haptic Feedback",
                    subtitle = "Vibrate briefly on gesture recognition and intent dispatch",
                    checked = hapticFeedback,
                    onCheckedChange = { hapticFeedback = it }
                )
            }

            // Section: System & Permissions
            SettingsSectionHeader(title = "SYSTEM STATUS")
            SettingsCard {
                SettingsTextRow(
                    title = "Accessibility Service",
                    subtitle = "Active & monitoring 4-finger gestures",
                    trailingText = "Enabled"
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                SettingsSwitchRow(
                    title = "Anonymous Diagnostics",
                    subtitle = "Send crash reports to help improve gesture detection latency",
                    checked = telemetryOptIn,
                    onCheckedChange = { telemetryOptIn = it }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp
        ),
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun SettingsCard(
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = ScreenIQShapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            content()
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = BrandPrimary,
                checkedTrackColor = BrandPrimary.copy(alpha = 0.3f)
            )
        )
    }
}

@Composable
private fun SettingsTextRow(
    title: String,
    subtitle: String,
    trailingText: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(
            text = trailingText,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = BrandPrimary
        )
    }
}
