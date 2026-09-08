package com.screeniq.officekit.test

import com.screeniq.history.model.ActionResultSummary
import com.screeniq.history.model.HistoryEntry
import com.screeniq.officekit.OfficeKitClipboardBridge
import com.screeniq.officekit.OfficeKitExporter

/**
 * Unit test verifying Office Kit export generation (CSV, JSON, and Clipboard formatting).
 */
class OfficeKitExporterTest {

    private val sampleEntries = listOf(
        HistoryEntry(
            id = "101",
            timestampMs = 1725800000000L,
            contentType = "EVENT",
            summary = "AI Workshop",
            actionSelected = "Added to Calendar",
            actionResult = ActionResultSummary.success("Added to Calendar"),
            confidence = 0.98f,
            isScreenshotStored = false
        ),
        HistoryEntry(
            id = "102",
            timestampMs = 1725800300000L,
            contentType = "LOCATION",
            summary = "IIT Madras",
            actionSelected = "Opened in Maps",
            actionResult = ActionResultSummary.success("Opened in Maps"),
            confidence = 0.95f,
            isScreenshotStored = false
        )
    )

    fun testCsvExport() {
        val csv = OfficeKitExporter.exportToCsv(sampleEntries)
        assert(csv.contains("ID,TimestampMs,FormattedTime")) { "CSV header missing" }
        assert(csv.contains("AI Workshop")) { "Entry 1 missing from CSV" }
        assert(csv.contains("IIT Madras")) { "Entry 2 missing from CSV" }
        assert(csv.contains("98")) { "Confidence missing from CSV" }
    }

    fun testJsonExport() {
        val json = OfficeKitExporter.exportToJson(sampleEntries)
        assert(json.startsWith("[")) { "JSON array start missing" }
        assert(json.endsWith("]\n")) { "JSON array end missing" }
        assert(json.contains("\"contentType\": \"EVENT\"")) { "Category missing from JSON" }
        assert(json.contains("\"summary\": \"AI Workshop\"")) { "Summary missing from JSON" }
        assert(json.contains("\"isScreenshotStored\": false")) { "Privacy flag missing from JSON" }
    }

    fun testClipboardFormatting() {
        val single = OfficeKitClipboardBridge.formatSingleEntry(sampleEntries[0])
        assert(single.contains("EVENT: AI Workshop")) { "Single entry clipboard text invalid" }

        val markdown = OfficeKitClipboardBridge.formatAsMarkdownTable(sampleEntries)
        assert(markdown.contains("| Time | Type | Summary | Action Result | Confidence |")) { "Markdown table header missing" }
        assert(markdown.contains("`EVENT`")) { "Markdown badge formatting missing" }

        val spreadsheet = OfficeKitClipboardBridge.formatForSpreadsheetPaste(sampleEntries)
        assert(spreadsheet.contains("\tEVENT\t")) { "Spreadsheet tab delimiters missing" }
    }
}
