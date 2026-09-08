package com.screeniq.settings.ui

import com.screeniq.history.repository.HistoryRepository
import com.screeniq.settings.model.ConfirmationBehavior
import com.screeniq.settings.model.HistoryRetention
import com.screeniq.settings.model.ScreenIqSettings
import com.screeniq.settings.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel managing ScreenIQ Settings state and user interactions.
 */
class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val historyRepository: HistoryRepository,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) {
    val settings: StateFlow<ScreenIqSettings> = settingsRepository.settings

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    fun toggleScreenIqEnabled(enabled: Boolean) {
        scope.launch {
            settingsRepository.setScreenIqEnabled(enabled)
        }
    }

    fun toggleGestureEnabled(enabled: Boolean) {
        scope.launch {
            settingsRepository.setGestureEnabled(enabled)
        }
    }

    fun setConfirmationBehavior(behavior: ConfirmationBehavior) {
        scope.launch {
            settingsRepository.setConfirmationBehavior(behavior)
        }
    }

    fun toggleAllowScreenProcessing(allowed: Boolean) {
        scope.launch {
            settingsRepository.setAllowScreenProcessing(allowed)
        }
    }

    fun toggleStoreScreenshots(store: Boolean) {
        scope.launch {
            settingsRepository.setStoreScreenshots(store)
        }
    }

    fun setHistoryRetention(retention: HistoryRetention) {
        scope.launch {
            settingsRepository.setHistoryRetention(retention)
            retention.calculateCutoffMs()?.let { cutoff ->
                historyRepository.purgeOlderThan(cutoff)
            }
        }
    }

    fun togglePreferLocalAi(preferLocal: Boolean) {
        scope.launch {
            settingsRepository.setPreferLocalAi(preferLocal)
        }
    }

    fun clearAllHistory() {
        scope.launch {
            val clearedCount = historyRepository.clearHistory()
            _userMessage.value = "Cleared $clearedCount history items."
        }
    }

    fun resetToDefaults() {
        scope.launch {
            settingsRepository.resetToDefaults()
            _userMessage.value = "Settings reset to defaults."
        }
    }

    fun dismissUserMessage() {
        _userMessage.value = null
    }
}
