package com.screeniq.history.ui

import com.screeniq.history.model.HistoryEntry
import com.screeniq.history.model.HistoryFilter
import com.screeniq.history.repository.HistoryRepository
import com.screeniq.officekit.ExportResult
import com.screeniq.officekit.OfficeKitClipboardBridge
import com.screeniq.officekit.OfficeKitExportFormat
import com.screeniq.officekit.OfficeKitExporter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * State and business logic for the ScreenIQ Action History UI.
 */
class HistoryViewModel(
    private val repository: HistoryRepository,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) {
    private val _filter = MutableStateFlow(HistoryFilter())
    val filter: StateFlow<HistoryFilter> = _filter.asStateFlow()

    private val _lastExportResult = MutableStateFlow<ExportResult?>(null)
    val lastExportResult: StateFlow<ExportResult?> = _lastExportResult.asStateFlow()

    private val _clipboardNotice = MutableStateFlow<String?>(null)
    val clipboardNotice: StateFlow<String?> = _clipboardNotice.asStateFlow()

    /**
     * Reactive list of history entries matching the current filter.
     */
    val entries: StateFlow<List<HistoryEntry>> = combine(
        repository.observeHistory(),
        _filter
    ) { allEntries, currentFilter ->
        allEntries.filter { currentFilter.matches(it) }
    }.stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateSearch(query: String) {
        _filter.value = _filter.value.copy(searchQuery = query.ifBlank { null })
    }

    fun filterByCategory(category: String?) {
        _filter.value = _filter.value.copy(contentType = category)
    }

    fun toggleOnlySuccessful() {
        _filter.value = _filter.value.copy(onlySuccessful = !_filter.value.onlySuccessful)
    }

    fun clearFilter() {
        _filter.value = HistoryFilter()
    }

    fun deleteItem(id: String) {
        scope.launch {
            repository.deleteHistoryItem(id)
        }
    }

    fun clearAllHistory() {
        scope.launch {
            repository.clearHistory()
        }
    }

    fun exportHistory(format: OfficeKitExportFormat) {
        scope.launch(Dispatchers.IO) {
            val currentList = entries.value
            val result = OfficeKitExporter.saveExportFile(currentList, format)
            _lastExportResult.value = result
        }
    }

    fun clearExportResult() {
        _lastExportResult.value = null
    }

    fun prepareSingleEntryClipboard(entry: HistoryEntry): String {
        return OfficeKitClipboardBridge.formatSingleEntry(entry)
    }

    fun prepareMarkdownTableClipboard(): String {
        return OfficeKitClipboardBridge.formatAsMarkdownTable(entries.value)
    }

    fun prepareSpreadsheetClipboard(): String {
        return OfficeKitClipboardBridge.formatForSpreadsheetPaste(entries.value)
    }

    fun setClipboardNotice(message: String?) {
        _clipboardNotice.value = message
    }
}
