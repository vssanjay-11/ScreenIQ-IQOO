package com.screeniq.history.test

import com.screeniq.history.model.ActionResultSummary
import com.screeniq.history.model.HistoryEntry
import com.screeniq.history.model.HistoryFilter
import com.screeniq.history.repository.HistoryRepositoryImpl
import com.screeniq.history.storage.InMemoryHistoryStorage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/**
 * Unit test for HistoryRepository and persistence behavior.
 */
class HistoryRepositoryTest {

    fun testRecordAndRetrieveHistory() = runBlocking {
        val storage = InMemoryHistoryStorage()
        val repo = HistoryRepositoryImpl(storage)

        val entry1 = HistoryEntry(
            id = "test-1",
            timestampMs = 1725800000000L,
            contentType = "EVENT",
            summary = "AI Workshop",
            actionSelected = "Add to Calendar",
            actionResult = ActionResultSummary.success("Added to Calendar"),
            confidence = 0.98f,
            isScreenshotStored = false
        )

        val entry2 = HistoryEntry(
            id = "test-2",
            timestampMs = 1725800300000L,
            contentType = "LOCATION",
            summary = "IIT Madras",
            actionSelected = "Open Maps",
            actionResult = ActionResultSummary.success("Opened in Maps"),
            confidence = 0.95f,
            isScreenshotStored = false
        )

        repo.recordAction(entry1)
        repo.recordAction(entry2)

        val recent = repo.getRecentHistory(10)
        assert(recent.size == 2) { "Expected 2 entries, got ${recent.size}" }
        assert(recent[0].id == "test-2") { "Expected newest entry first" }

        // Test filtering
        val eventFilter = HistoryFilter(contentType = "EVENT")
        val filtered = repo.getFilteredHistory(eventFilter)
        assert(filtered.size == 1) { "Expected 1 EVENT entry, got ${filtered.size}" }
        assert(filtered[0].summary == "AI Workshop")

        // Test Clear History
        val cleared = repo.clearHistory()
        assert(cleared == 2) { "Expected 2 cleared entries, got $cleared" }
        assert(repo.getHistoryCount() == 0) { "Expected 0 entries remaining" }
    }

    fun testNoScreenshotStoredGuarantee() = runBlocking {
        val storage = InMemoryHistoryStorage()
        val repo = HistoryRepositoryImpl(storage)

        // Attempt to pass an entry with isScreenshotStored = true
        val entry = HistoryEntry(
            id = "leak-test",
            contentType = "DOCUMENT",
            summary = "Confidential memo",
            actionSelected = "Summarize",
            actionResult = ActionResultSummary.success("Summarized"),
            confidence = 0.85f,
            isScreenshotStored = true // Simulating accidental flag
        )

        repo.recordAction(entry)

        val retrieved = repo.getRecentHistory(1).first()
        // Repository must sanitize and force isScreenshotStored to false
        assert(!retrieved.isScreenshotStored) { "VIOLATION: Screenshot persistence flag was not forced to false!" }
    }
}
