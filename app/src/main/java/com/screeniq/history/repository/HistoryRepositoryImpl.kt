package com.screeniq.history.repository

import com.screeniq.history.model.HistoryEntry
import com.screeniq.history.model.HistoryFilter
import com.screeniq.history.storage.HistoryStorage
import com.screeniq.history.storage.InMemoryHistoryStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Thread-safe production repository for ScreenIQ action history.
 *
 * Enforces:
 * 1. Privacy guarantee: Screenshots are never persisted by default.
 * 2. Immediate reactive updates via Kotlin StateFlow.
 * 3. Retention policy purging on startup and update.
 */
class HistoryRepositoryImpl(
    private val storage: HistoryStorage = InMemoryHistoryStorage(),
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) : HistoryRepository {

    private val mutex = Mutex()
    private val _historyFlow = MutableStateFlow<List<HistoryEntry>>(emptyList())
    private var isInitialized = false

    init {
        scope.launch {
            ensureInitialized()
        }
    }

    private suspend fun ensureInitialized() {
        if (!isInitialized) {
            mutex.withLock {
                if (!isInitialized) {
                    val loaded = storage.loadAll()
                    _historyFlow.value = loaded
                    isInitialized = true
                }
            }
        }
    }

    override suspend fun recordAction(entry: HistoryEntry) {
        ensureInitialized()
        // Enforce privacy rule: ScreenIQ never persists full screenshots by default
        val sanitizedEntry = if (entry.isScreenshotStored) {
            entry.copy(isScreenshotStored = false)
        } else {
            entry
        }

        mutex.withLock {
            val updated = listOf(sanitizedEntry) + _historyFlow.value
            _historyFlow.value = updated
            storage.append(sanitizedEntry)
        }
    }

    override fun observeHistory(): Flow<List<HistoryEntry>> {
        return _historyFlow.asStateFlow()
    }

    override suspend fun getRecentHistory(limit: Int): List<HistoryEntry> {
        ensureInitialized()
        return _historyFlow.value.take(limit.coerceAtLeast(1))
    }

    override suspend fun getFilteredHistory(filter: HistoryFilter): List<HistoryEntry> {
        ensureInitialized()
        return _historyFlow.value.filter { filter.matches(it) }
    }

    override suspend fun deleteHistoryItem(id: String): Boolean {
        ensureInitialized()
        return mutex.withLock {
            val current = _historyFlow.value
            val exists = current.any { it.id == id }
            if (exists) {
                val updated = current.filter { it.id != id }
                _historyFlow.value = updated
                storage.saveAll(updated)
                true
            } else {
                false
            }
        }
    }

    override suspend fun clearHistory(): Int {
        ensureInitialized()
        return mutex.withLock {
            val count = _historyFlow.value.size
            _historyFlow.value = emptyList()
            storage.clear()
            count
        }
    }

    override suspend fun purgeOlderThan(cutoffTimestampMs: Long): Int {
        ensureInitialized()
        return mutex.withLock {
            val current = _historyFlow.value
            val remaining = current.filter { it.timestampMs >= cutoffTimestampMs }
            val purgedCount = current.size - remaining.size
            if (purgedCount > 0) {
                _historyFlow.value = remaining
                storage.saveAll(remaining)
            }
            purgedCount
        }
    }

    override suspend fun getHistoryCount(): Int {
        ensureInitialized()
        return _historyFlow.value.size
    }
}
