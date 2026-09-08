package com.screeniq.history.model

/**
 * Filter specification for querying history entries.
 */
data class HistoryFilter(
    val searchQuery: String? = null,
    val contentType: String? = null,
    val onlySuccessful: Boolean = false,
    val startTimeMs: Long? = null,
    val endTimeMs: Long? = null
) {
    /**
     * Checks whether a given [HistoryEntry] satisfies this filter.
     */
    fun matches(entry: HistoryEntry): Boolean {
        if (!searchQuery.isNullOrBlank()) {
            val query = searchQuery.trim().lowercase()
            val matchesSummary = entry.summary.lowercase().contains(query)
            val matchesAction = entry.actionSelected.lowercase().contains(query)
            val matchesType = entry.contentType.lowercase().contains(query)
            if (!matchesSummary && !matchesAction && !matchesType) return false
        }

        if (!contentType.isNullOrBlank() && !entry.contentType.equals(contentType, ignoreCase = true)) {
            return false
        }

        if (onlySuccessful && !entry.actionResult.wasSuccessful) {
            return false
        }

        if (startTimeMs != null && entry.timestampMs < startTimeMs) {
            return false
        }

        if (endTimeMs != null && entry.timestampMs > endTimeMs) {
            return false
        }

        return true
    }
}
