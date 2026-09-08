package com.screeniq.history.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.screeniq.history.model.HistoryEntry
import com.screeniq.officekit.OfficeKitExportFormat

/**
 * Full phone-first Action History screen for ScreenIQ.
 *
 * Displays local action logs:
 * Example:
 * 09:42
 * EVENT
 * AI Workshop
 * Added to Calendar ✓
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onNavigateBack: () -> Unit = {},
    onCopyToClipboard: (String, String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val entries by viewModel.entries.collectAsState()
    val filter by viewModel.filter.collectAsState()
    val exportResult by viewModel.lastExportResult.collectAsState()

    var showClearConfirmationDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Action History",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                        Text(
                            text = "${entries.size} actions recorded locally",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showExportDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Office Kit Export",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (entries.isNotEmpty()) {
                        IconButton(onClick = { showClearConfirmationDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Clear History",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Privacy Assurance Banner
            PrivacyAssuranceBanner()

            // Search and Category Filter Bar
            HistoryFilterHeader(
                searchQuery = filter.searchQuery ?: "",
                onSearchChange = { viewModel.updateSearch(it) },
                selectedCategory = filter.contentType,
                onSelectCategory = { viewModel.filterByCategory(it) }
            )

            // Main List
            if (entries.isEmpty()) {
                EmptyHistoryView(isFiltered = filter.searchQuery != null || filter.contentType != null)
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(entries, key = { it.id }) { entry ->
                        HistoryCardItem(
                            entry = entry,
                            onDelete = { viewModel.deleteItem(entry.id) },
                            onCopy = {
                                val text = viewModel.prepareSingleEntryClipboard(entry)
                                onCopyToClipboard(text, "Action summary copied to clipboard")
                            }
                        )
                    }
                }
            }
        }
    }

    // Clear History Confirmation Dialog
    if (showClearConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmationDialog = false },
            title = { Text("Clear All History?") },
            text = {
                Text("This will permanently delete all recorded action summaries from this device. This action cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllHistory()
                        showClearConfirmationDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showClearConfirmationDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Office Kit Export Dialog
    if (showExportDialog) {
        OfficeKitExportModal(
            onDismiss = { showExportDialog = false },
            onExportCsv = {
                viewModel.exportHistory(OfficeKitExportFormat.CSV)
                showExportDialog = false
            },
            onExportJson = {
                viewModel.exportHistory(OfficeKitExportFormat.JSON)
                showExportDialog = false
            },
            onCopyMarkdown = {
                val md = viewModel.prepareMarkdownTableClipboard()
                onCopyToClipboard(md, "Office Kit Markdown table copied")
                showExportDialog = false
            },
            onCopySpreadsheet = {
                val tsv = viewModel.prepareSpreadsheetClipboard()
                onCopyToClipboard(tsv, "Excel / Spreadsheet data copied")
                showExportDialog = false
            }
        )
    }

    // Export Feedback Notification
    exportResult?.let { result ->
        AlertDialog(
            onDismissRequest = { viewModel.clearExportResult() },
            title = { Text(if (result.success) "Export Ready" else "Export Failed") },
            text = {
                if (result.success) {
                    Text("Exported ${result.entryCount} entries as ${result.format.name}.\n\nSaved to:\n${result.exportedFile?.absolutePath}\n\nReady for transfer via Office Kit!")
                } else {
                    Text("Failed to export: ${result.errorMessage}")
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.clearExportResult() }) {
                    Text("OK")
                }
            }
        )
    }
}

/**
 * Individual History Entry Card formatted cleanly as specified in the hackathon brief:
 *
 * 09:42
 * EVENT
 * AI Workshop
 * Added to Calendar ✓
 */
@Composable
fun HistoryCardItem(
    entry: HistoryEntry,
    onDelete: () -> Unit,
    onCopy: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onCopy() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Row 1: Timestamp & Category Badge & Confidence
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = entry.formattedTime,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    ContentTypeBadge(contentType = entry.contentType)
                }

                // Confidence Score Pill
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.padding(start = 4.dp)
                ) {
                    Text(
                        text = "${entry.confidencePercentage}% conf",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Row 2: Summary (Headline)
            Text(
                text = entry.summary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Row 3: Action Result
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val statusColor = if (entry.actionResult.wasSuccessful) Color(0xFF10B981) else Color(0xFFEF4444)
                    Text(
                        text = entry.actionResult.displayStatus,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = statusColor
                    )
                }

                Text(
                    text = "Tap to copy",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
fun ContentTypeBadge(contentType: String) {
    val (bgColor, textColor) = when (contentType.uppercase()) {
        "EVENT" -> Color(0xFF6366F1).copy(alpha = 0.2f) to Color(0xFF4F46E5)
        "LOCATION" -> Color(0xFF10B981).copy(alpha = 0.2f) to Color(0xFF059669)
        "COMMUNICATION", "PHONE", "EMAIL" -> Color(0xFFF59E0B).copy(alpha = 0.2f) to Color(0xFFD97706)
        "URL", "WEB_LINK" -> Color(0xFF3B82F6).copy(alpha = 0.2f) to Color(0xFF2563EB)
        "PRODUCTIVITY_TASK", "TASK" -> Color(0xFF8B5CF6).copy(alpha = 0.2f) to Color(0xFF7C3AED)
        else -> Color.Gray.copy(alpha = 0.2f) to Color.DarkGray
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = contentType.uppercase(),
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            color = textColor
        )
    }
}

@Composable
fun PrivacyAssuranceBanner() {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🛡️ Privacy Guard: No screenshots are saved. Only action text & results are kept on-device.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun HistoryFilterHeader(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    selectedCategory: String?,
    onSelectCategory: (String?) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("Search actions (e.g., Workshop, Maps)...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        val categories = listOf("EVENT", "LOCATION", "COMMUNICATION", "URL", "TASK")
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = { onSelectCategory(null) },
                    label = { Text("All") }
                )
            }
            items(categories) { cat ->
                FilterChip(
                    selected = selectedCategory.equals(cat, ignoreCase = true),
                    onClick = {
                        if (selectedCategory.equals(cat, ignoreCase = true)) {
                            onSelectCategory(null)
                        } else {
                            onSelectCategory(cat)
                        }
                    },
                    label = { Text(cat) }
                )
            }
        }
    }
}

@Composable
fun EmptyHistoryView(isFiltered: Boolean) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (isFiltered) "No matching actions" else "No actions recorded yet",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isFiltered)
                    "Try clearing your search filter"
                else
                    "Perform a 4-finger swipe on any screen to trigger ScreenIQ!",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
fun OfficeKitExportModal(
    onDismiss: () -> Unit,
    onExportCsv: () -> Unit,
    onExportJson: () -> Unit,
    onCopyMarkdown: () -> Unit,
    onCopySpreadsheet: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Office Kit Productivity Export",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Transfer action history to your laptop or productivity tools via Office Kit.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Button(
                    onClick = onExportCsv,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Export as CSV (Excel / Sheets)")
                }

                Button(
                    onClick = onExportJson,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Export as JSON (Data Feed)")
                }

                OutlinedButton(
                    onClick = onCopySpreadsheet,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Copy for Shared Clipboard (Excel)")
                }

                OutlinedButton(
                    onClick = onCopyMarkdown,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Copy Markdown Table (Docs / Notion)")
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
