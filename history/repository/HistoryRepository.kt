package com.screeniq.history.repository

import com.screeniq.history.model.HistoryEntry
import com.screeniq.history.model.HistoryFilter
import kotlinx.coroutines.flow.Flow

/**
 * Core interface for recording and querying ScreenIQ action history.
 *
 * Adheres strictly to the Privacy by Design specification:
 * - Persists metadata, categories, summaries, results, and confidence scores locally.
 * - Does NOT persist screen bitmaps.
 * - Supports instant, total clearing of history.
 */
interface HistoryRepository {

    /**
     * Records a new action event into local history.
     */
    suspend fun recordAction(entry: HistoryEntry)

    /**
     * Continuous reactive stream of all history items ordered by newest first.
     */
    fun observeHistory(): Flow<List<HistoryEntry>>

    /**
     * Retrieves the most recent history entries up to [limit].
     */
    suspend fun getRecentHistory(limit: Int = 50): List<HistoryEntry>

    /**
     * Retrieves history entries matching the given [filter].
     */
    suspend fun getFilteredHistory(filter: HistoryFilter): List<HistoryEntry>

    /**
     * Deletes a single history item by [id].
     * @return true if an item was found and deleted, false otherwise.
     */
    suspend fun deleteHistoryItem(id: String): Boolean

    /**
     * Clears all local history records immediately.
     * @return number of records deleted.
     */
    suspend fun clearHistory(): Int

    /**
     * Purges history entries older than [cutoffTimestampMs].
     * @return number of purged records.
     */
    suspend fun purgeOlderThan(cutoffTimestampMs: Long): Int

    /**
     * Returns the total count of recorded history items.
     */
    suspend fun getHistoryCount(): Int
}
