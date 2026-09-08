package com.screeniq.settings.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.screeniq.privacy.PrivacyPolicy
import com.screeniq.settings.model.ConfirmationBehavior
import com.screeniq.settings.model.HistoryRetention

/**
 * Modern Jetpack Compose Settings UI for ScreenIQ.
 *
 * Provides granular user controls over:
 * - Master ScreenIQ switch
 * - 4-Finger gesture detection
 * - Action confirmation mode
 * - Strict Privacy Controls (no screenshots by default)
 * - Local AI execution preference
 * - History retention window
 * - Total history clearing
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsState()
    val userMessage by viewModel.userMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showClearHistoryDialog by remember { mutableStateOf(false) }
    var showScreenshotWarningDialog by remember { mutableStateOf(false) }
    var showPrivacyPolicyDialog by remember { mutableStateOf(false) }

    LaunchedEffect(userMessage) {
        userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.dismissUserMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "ScreenIQ Settings",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                actions = {
                    IconButton(onClick = { showPrivacyPolicyDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Privacy Policy & Disclosure",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Section 1: Core System Controls
            SettingsSection(title = "General") {
                SettingSwitchItem(
                    title = "ScreenIQ Master Switch",
                    subtitle = "Enable or pause all AI screen analysis and action suggestions",
                    checked = settings.isScreenIqEnabled,
                    onCheckedChange = { viewModel.toggleScreenIqEnabled(it) }
                )

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                SettingSwitchItem(
                    title = "4-Finger Swipe Gesture",
                    subtitle = "Detect 4-finger swipe gesture on the iQOO display to trigger ScreenIQ",
                    checked = settings.isGestureEnabled,
                    enabled = settings.isScreenIqEnabled,
                    onCheckedChange = { viewModel.toggleGestureEnabled(it) }
                )
            }

            // Section 2: Confirmation Behavior
            SettingsSection(title = "Action Execution Behavior") {
                ConfirmationBehavior.values().forEach { behavior ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.setConfirmationBehavior(behavior) }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = settings.confirmationBehavior == behavior,
                            onClick = { viewModel.setConfirmationBehavior(behavior) }
                        )
                        Column(modifier = Modifier.padding(start = 8.dp)) {
                            Text(
                                text = behavior.displayName,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = behavior.description,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Section 3: Privacy & AI Controls
            SettingsSection(title = "Privacy & Local AI") {
                SettingSwitchItem(
                    title = "Allow Screen Processing",
                    subtitle = "Analyze visible screen contents upon intentional gesture trigger",
                    checked = settings.privacy.allowScreenProcessing,
                    onCheckedChange = { viewModel.toggleAllowScreenProcessing(it) }
                )

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                SettingSwitchItem(
                    title = "Prefer On-Device Local AI",
                    subtitle = "Process OCR and entities purely on-device (Google ML Kit); zero cloud transmission",
                    checked = settings.preferLocalAi,
                    onCheckedChange = { viewModel.togglePreferLocalAi(it) }
                )

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                SettingSwitchItem(
                    title = "Save Screenshots to Storage",
                    subtitle = "Default: OFF. ScreenIQ operates strictly in volatile RAM for privacy.",
                    checked = settings.privacy.storeScreenshots,
                    onCheckedChange = { willEnable ->
                        if (willEnable) {
                            showScreenshotWarningDialog = true
                        } else {
                            viewModel.toggleStoreScreenshots(false)
                        }
                    }
                )
            }

            // Section 4: History Retention
            SettingsSection(title = "Action History Retention") {
                Text(
                    text = "Automatically purge local action records after:",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                HistoryRetention.values().forEach { retention ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.setHistoryRetention(retention) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = settings.historyRetention == retention,
                            onClick = { viewModel.setHistoryRetention(retention) }
                        )
                        Text(
                            text = retention.displayName,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }

            // Section 5: Data Management & Reset
            SettingsSection(title = "Data Management") {
                Button(
                    onClick = { showClearHistoryDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Clear All Action History")
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = { viewModel.resetToDefaults() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Reset All Settings to Defaults")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Dialog 1: Clear History Confirmation
    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            title = { Text("Clear All Action History?") },
            text = { Text("This will permanently remove all action records from your device storage. This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllHistory()
                        showClearHistoryDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear Everything")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showClearHistoryDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dialog 2: Screenshot Persistence Warning
    if (showScreenshotWarningDialog) {
        AlertDialog(
            onDismissRequest = { showScreenshotWarningDialog = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Privacy Caution") },
            text = {
                Text(
                    "By default, ScreenIQ recycles all screen frames in memory immediately for maximum privacy. " +
                    "Storing screenshots on disk uses device storage and may persist personal information visible on your screen."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.toggleStoreScreenshots(true)
                        showScreenshotWarningDialog = false
                    }
                ) {
                    Text("Enable Anyway")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showScreenshotWarningDialog = false }) {
                    Text("Keep Safe (Disabled)")
                }
            }
        )
    }

    // Dialog 3: Full Privacy Policy & Disclosure Modal
    if (showPrivacyPolicyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyPolicyDialog = false },
            title = { Text(PrivacyPolicy.SUMMARY_TITLE, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = PrivacyPolicy.SHORT_DISCLOSURE,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    )

                    Divider(modifier = Modifier.padding(vertical = 4.dp))

                    PrivacyPolicy.PRIVACY_HIGHLIGHTS.forEach { highlight ->
                        Text(text = highlight, fontSize = 13.sp)
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showPrivacyPolicyDialog = false }) {
                    Text("Understood")
                }
            }
        )
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            content()
        }
    }
}

@Composable
fun SettingSwitchItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }
}
