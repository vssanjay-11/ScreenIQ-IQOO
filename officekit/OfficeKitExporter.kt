package com.screeniq.officekit

import com.screeniq.history.model.HistoryEntry
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Supported file export formats for Office Kit File Transfer.
 */
enum class OfficeKitExportFormat(val extension: String, val mimeType: String) {
    CSV("csv", "text/csv"),
    JSON("json", "application/json")
}

/**
 * Result of an export operation.
 */
data class ExportResult(
    val success: Boolean,
    val exportedFile: File? = null,
    val entryCount: Int = 0,
    val format: OfficeKitExportFormat,
    val errorMessage: String? = null
)

/**
 * Utilities for exporting ScreenIQ action history for use with
 * Office Kit file transfer and desktop productivity tools (Excel, Sheets, Docs).
 *
 * NOTE: Office Kit provides screen mirroring, shared clipboard, file transfer,
 * and remote control. No custom cloud backend or private APIs are assumed.
 */
object OfficeKitExporter {

    /**
     * Serializes history entries into standard RFC 4180 CSV.
     */
    fun exportToCsv(entries: List<HistoryEntry>): String {
        val sb = StringBuilder()
        // Header
        sb.append("ID,TimestampMs,FormattedTime,FormattedDateTime,ContentType,Summary,ActionSelected,Success,StatusMessage,ConfidencePercent,SourcePackage\n")

        for (entry in entries) {
            sb.append(escapeCsv(entry.id)).append(",")
            sb.append(entry.timestampMs).append(",")
            sb.append(escapeCsv(entry.formattedTime)).append(",")
            sb.append(escapeCsv(entry.formattedDateTime)).append(",")
            sb.append(escapeCsv(entry.contentType)).append(",")
            sb.append(escapeCsv(entry.summary)).append(",")
            sb.append(escapeCsv(entry.actionSelected)).append(",")
            sb.append(entry.actionResult.wasSuccessful).append(",")
            sb.append(escapeCsv(entry.actionResult.statusMessage)).append(",")
            sb.append(entry.confidencePercentage).append(",")
            sb.append(escapeCsv(entry.sourcePackage ?: "N/A"))
            sb.append("\n")
        }
        return sb.toString()
    }

    /**
     * Serializes history entries into pretty-printed JSON.
     */
    fun exportToJson(entries: List<HistoryEntry>): String {
        val sb = StringBuilder()
        sb.append("[\n")
        entries.forEachIndexed { index, entry ->
            sb.append("  {\n")
            sb.append("    \"id\": \"${escapeJson(entry.id)}\",\n")
            sb.append("    \"timestampMs\": ${entry.timestampMs},\n")
            sb.append("    \"time\": \"${escapeJson(entry.formattedTime)}\",\n")
            sb.append("    \"dateTime\": \"${escapeJson(entry.formattedDateTime)}\",\n")
            sb.append("    \"contentType\": \"${escapeJson(entry.contentType)}\",\n")
            sb.append("    \"summary\": \"${escapeJson(entry.summary)}\",\n")
            sb.append("    \"actionSelected\": \"${escapeJson(entry.actionSelected)}\",\n")
            sb.append("    \"wasSuccessful\": ${entry.actionResult.wasSuccessful},\n")
            sb.append("    \"statusMessage\": \"${escapeJson(entry.actionResult.statusMessage)}\",\n")
            sb.append("    \"confidence\": ${entry.confidence},\n")
            sb.append("    \"confidencePercentage\": ${entry.confidencePercentage},\n")
            sb.append("    \"isScreenshotStored\": ${entry.isScreenshotStored},\n")
            sb.append("    \"sourcePackage\": ${if (entry.sourcePackage != null) "\"${escapeJson(entry.sourcePackage)}\"" else "null"}\n")
            sb.append("  }")
            if (index < entries.size - 1) {
                sb.append(",")
            }
            sb.append("\n")
        }
        sb.append("]\n")
        return sb.toString()
    }

    /**
     * Saves history entries to a local export file ready for Office Kit File Transfer.
     */
    fun saveExportFile(
        entries: List<HistoryEntry>,
        format: OfficeKitExportFormat,
        outputDirectory: File = File(System.getProperty("java.io.tmpdir"), "screeniq_exports")
    ): ExportResult {
        return try {
            if (!outputDirectory.exists()) {
                outputDirectory.mkdirs()
            }
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val file = File(outputDirectory, "screeniq_history_$timestamp.${format.extension}")

            val content = when (format) {
                OfficeKitExportFormat.CSV -> exportToCsv(entries)
                OfficeKitExportFormat.JSON -> exportToJson(entries)
            }
            file.writeText(content, Charsets.UTF_8)

            ExportResult(
                success = true,
                exportedFile = file,
                entryCount = entries.size,
                format = format
            )
        } catch (e: Exception) {
            ExportResult(
                success = false,
                entryCount = 0,
                format = format,
                errorMessage = e.localizedMessage
            )
        }
    }

    private fun escapeCsv(value: String): String {
        val containsSpecial = value.contains(",") || value.contains("\"") || value.contains("\n")
        return if (containsSpecial) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }

    private fun escapeJson(value: String): String {
        return value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\b", "\\b")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }
}
