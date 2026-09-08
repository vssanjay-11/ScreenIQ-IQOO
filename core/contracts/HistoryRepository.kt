package com.screeniq.core.contracts

import com.screeniq.core.model.ActionHistoryItem

/**
 * Agent 9: History and Privacy Storage Repository
 * Provides privacy-compliant local persistence for action audit logs and Office Kit summaries.
 */
interface HistoryRepository {
    suspend fun recordAction(item: ActionHistoryItem)
    suspend fun getRecentHistory(limit: Int): List<ActionHistoryItem>
    suspend fun clearHistory()
}
