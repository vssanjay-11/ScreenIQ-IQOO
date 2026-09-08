package com.screeniq.settings.repository

import com.screeniq.settings.model.ConfirmationBehavior
import com.screeniq.settings.model.HistoryRetention
import com.screeniq.settings.model.ScreenIqSettings
import kotlinx.coroutines.flow.StateFlow

/**
 * Repository interface for managing local ScreenIQ configuration and privacy settings.
 */
interface SettingsRepository {

    /**
     * Observable reactive stream of current settings.
     */
    val settings: StateFlow<ScreenIqSettings>

    /**
     * Synchronous/one-shot snapshot of current settings.
     */
    fun currentSettings(): ScreenIqSettings

    /**
     * Toggles ScreenIQ master switch.
     */
    suspend fun setScreenIqEnabled(enabled: Boolean)

    /**
     * Toggles 4-finger gesture trigger detection.
     */
    suspend fun setGestureEnabled(enabled: Boolean)

    /**
     * Updates action confirmation strategy.
     */
    suspend fun setConfirmationBehavior(behavior: ConfirmationBehavior)

    /**
     * Sets whether ScreenIQ is permitted to process on-screen content.
     */
    suspend fun setAllowScreenProcessing(allowed: Boolean)

    /**
     * Toggles screenshot storage.
     * Note: Enabling this presents a strict warning dialog.
     */
    suspend fun setStoreScreenshots(store: Boolean)

    /**
     * Updates local action history retention window.
     */
    suspend fun setHistoryRetention(retention: HistoryRetention)

    /**
     * Sets preference for on-device/local AI processing over remote fallbacks.
     */
    suspend fun setPreferLocalAi(preferLocal: Boolean)

    /**
     * Functional updater for transactional settings modification.
     */
    suspend fun updateSettings(transform: (ScreenIqSettings) -> ScreenIqSettings)

    /**
     * Resets all settings to factory default safe values.
     */
    suspend fun resetToDefaults()
}
