package com.screeniq.core.model

/**
 * Privacy-compliant local record of actions performed.
 * Note: Screenshots are NOT stored here; only metadata and summary text.
 */
data class ActionHistoryItem(
    val id: String,
    val timestampMs: Long,
    val category: ContentCategory,
    val actionType: ActionType,
    val summarySnippet: String,
    val wasSuccessful: Boolean
)
