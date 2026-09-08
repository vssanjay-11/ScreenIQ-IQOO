package com.screeniq.history.storage

import com.screeniq.history.model.ActionResultSummary
import com.screeniq.history.model.HistoryEntry
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Interface for low-level persistence of history entries.
 */
interface HistoryStorage {
    suspend fun loadAll(): List<HistoryEntry>
    suspend fun saveAll(entries: List<HistoryEntry>)
    suspend fun append(entry: HistoryEntry)
    suspend fun clear()
}

/**
 * Thread-safe, file-backed JSON/delimited local storage for history entries.
 * Operates reliably on both Android internal storage (`context.filesDir`) and JVM test environments.
 *
 * Privacy guarantee: Never touches external/public storage. Never writes screen bitmaps.
 */
class PersistentFileHistoryStorage(
    private val storageDir: File = File(System.getProperty("java.io.tmpdir"), "screeniq_storage")
) : HistoryStorage {

    private val mutex = Mutex()
    private val historyFile: File by lazy {
        if (!storageDir.exists()) {
            storageDir.mkdirs()
        }
        File(storageDir, "action_history.tsv")
    }

    override suspend fun loadAll(): List<HistoryEntry> = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (!historyFile.exists()) return@withLock emptyList()
            try {
                val lines = historyFile.readLines(StandardCharsets.UTF_8)
                lines.mapNotNull { line ->
                    parseTsvLine(line)
                }.sortedByDescending { it.timestampMs }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    override suspend fun saveAll(entries: List<HistoryEntry>): Unit = withContext(Dispatchers.IO) {
        mutex.withLock {
            val content = entries.joinToString("\n") { serializeToTsvLine(it) }
            historyFile.writeText(content, StandardCharsets.UTF_8)
        }
    }

    override suspend fun append(entry: HistoryEntry): Unit = withContext(Dispatchers.IO) {
        mutex.withLock {
            val line = serializeToTsvLine(entry) + "\n"
            FileOutputStream(historyFile, true).use { stream ->
                stream.write(line.toByteArray(StandardCharsets.UTF_8))
            }
        }
    }

    override suspend fun clear(): Unit = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (historyFile.exists()) {
                historyFile.delete()
            }
        }
    }

    companion object {
        private const val DELIMITER = "\t"

        internal fun serializeToTsvLine(entry: HistoryEntry): String {
            // Escape delimiters and newlines
            val cleanSummary = entry.summary.replace("\t", " ").replace("\n", " ")
            val cleanAction = entry.actionSelected.replace("\t", " ").replace("\n", " ")
            val cleanStatus = entry.actionResult.statusMessage.replace("\t", " ").replace("\n", " ")
            val cleanError = entry.actionResult.errorMessage?.replace("\t", " ")?.replace("\n", " ") ?: ""
            val cleanPkg = entry.sourcePackage?.replace("\t", " ") ?: ""

            return listOf(
                entry.id,
                entry.timestampMs.toString(),
                entry.contentType,
                cleanSummary,
                cleanAction,
                entry.actionResult.wasSuccessful.toString(),
                cleanStatus,
                cleanError,
                entry.confidence.toString(),
                entry.isScreenshotStored.toString(),
                cleanPkg
            ).joinToString(DELIMITER)
        }

        internal fun parseTsvLine(line: String): HistoryEntry? {
            if (line.isBlank()) return null
            val parts = line.split(DELIMITER)
            if (parts.size < 9) return null

            return try {
                val id = parts[0]
                val timestampMs = parts[1].toLong()
                val contentType = parts[2]
                val summary = parts[3]
                val actionSelected = parts[4]
                val wasSuccessful = parts[5].toBoolean()
                val statusMessage = parts[6]
                val errorMessage = parts[7].ifEmpty { null }
                val confidence = parts[8].toFloat()
                val isScreenshotStored = if (parts.size > 9) parts[9].toBoolean() else false
                val sourcePackage = if (parts.size > 10) parts[10].ifEmpty { null } else null

                HistoryEntry(
                    id = id,
                    timestampMs = timestampMs,
                    contentType = contentType,
                    summary = summary,
                    actionSelected = actionSelected,
                    actionResult = ActionResultSummary(
                        wasSuccessful = wasSuccessful,
                        statusMessage = statusMessage,
                        errorMessage = errorMessage
                    ),
                    confidence = confidence,
                    isScreenshotStored = isScreenshotStored,
                    sourcePackage = sourcePackage
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}

/**
 * Fast in-memory implementation for unit testing or transient sessions.
 */
class InMemoryHistoryStorage : HistoryStorage {
    private val entries = mutableListOf<HistoryEntry>()
    private val mutex = Mutex()

    override suspend fun loadAll(): List<HistoryEntry> = mutex.withLock {
        entries.sortedByDescending { it.timestampMs }.toList()
    }

    override suspend fun saveAll(newEntries: List<HistoryEntry>): Unit = mutex.withLock {
        entries.clear()
        entries.addAll(newEntries)
    }

    override suspend fun append(entry: HistoryEntry): Unit = mutex.withLock {
        entries.add(entry)
    }

    override suspend fun clear(): Unit = mutex.withLock {
        entries.clear()
    }
}
