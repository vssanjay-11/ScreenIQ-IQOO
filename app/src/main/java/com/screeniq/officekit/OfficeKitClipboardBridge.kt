package com.screeniq.officekit

import com.screeniq.history.model.HistoryEntry

/**
 * Formats ScreenIQ data specifically for the Office Kit cross-device shared clipboard.
 *
 * When copied to the phone's Android clipboard, Office Kit automatically
 * synchronizes the text to the connected PC, allowing immediate pasting into
 * desktop applications (Excel, Notion, Google Docs, Slack, IDEs).
 */
object OfficeKitClipboardBridge {

    /**
     * Formats a single entry for quick paste.
     * Example:
     * "09:42 | EVENT: AI Workshop -> Added to Calendar ✓ (98% confidence)"
     */
    fun formatSingleEntry(entry: HistoryEntry): String {
        return "${entry.formattedTime} | ${entry.contentType}: ${entry.summary} -> ${entry.actionResult.displayStatus} (${entry.confidencePercentage}% confidence)"
    }

    /**
     * Formats multiple entries into a clean Markdown table for desktop documentation.
     */
    fun formatAsMarkdownTable(entries: List<HistoryEntry>): String {
        if (entries.isEmpty()) return "*No actions recorded.*"

        val sb = StringBuilder()
        sb.append("| Time | Type | Summary | Action Result | Confidence |\n")
        sb.append("| :--- | :--- | :--- | :--- | :--- |\n")
        for (entry in entries) {
            val status = if (entry.actionResult.wasSuccessful) "✓ ${entry.actionResult.statusMessage}" else "✗ ${entry.actionResult.statusMessage}"
            sb.append("| ${entry.formattedTime} | `${entry.contentType}` | ${entry.summary.replace("|", "/")} | $status | ${entry.confidencePercentage}% |\n")
        }
        return sb.toString()
    }

    /**
     * Formats entries into tab-separated text, which pastes cleanly
     * directly into Microsoft Excel or Google Sheets on the PC.
     */
    fun formatForSpreadsheetPaste(entries: List<HistoryEntry>): String {
        val sb = StringBuilder()
        sb.append("Time\tType\tSummary\tAction\tSuccess\tConfidence\n")
        for (entry in entries) {
            sb.append("${entry.formattedTime}\t")
            sb.append("${entry.contentType}\t")
            sb.append("${entry.summary}\t")
            sb.append("${entry.actionSelected}\t")
            sb.append("${entry.actionResult.wasSuccessful}\t")
            sb.append("${entry.confidencePercentage}%\n")
        }
        return sb.toString()
    }
}
