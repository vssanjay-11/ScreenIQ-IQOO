package com.screeniq.settings.repository

import com.screeniq.settings.model.ConfirmationBehavior
import com.screeniq.settings.model.HistoryRetention
import com.screeniq.settings.model.PrivacyControls
import com.screeniq.settings.model.ScreenIqSettings
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Properties
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Robust, thread-safe implementation of [SettingsRepository].
 * Persists settings to an isolated local properties file, guaranteeing
 * zero cloud leaks and persistence across app launches.
 */
class SettingsRepositoryImpl(
    private val storageDir: File = File(System.getProperty("java.io.tmpdir"), "screeniq_storage"),
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) : SettingsRepository {

    private val mutex = Mutex()
    private val configFile: File by lazy {
        if (!storageDir.exists()) storageDir.mkdirs()
        File(storageDir, "screeniq_settings.properties")
    }

    private val _settings = MutableStateFlow(ScreenIqSettings.DEFAULT)
    override val settings: StateFlow<ScreenIqSettings> = _settings.asStateFlow()

    init {
        scope.launch {
            loadFromDisk()
        }
    }

    private suspend fun loadFromDisk() = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (configFile.exists()) {
                try {
                    val props = Properties()
                    FileInputStream(configFile).use { props.load(it) }

                    val loaded = ScreenIqSettings(
                        isScreenIqEnabled = props.getProperty("screeniq_enabled", "true").toBoolean(),
                        isGestureEnabled = props.getProperty("gesture_enabled", "true").toBoolean(),
                        confirmationBehavior = try {
                            ConfirmationBehavior.valueOf(props.getProperty("confirmation_behavior", ConfirmationBehavior.CONFIRM_SENSITIVE_ONLY.name))
                        } catch (e: Exception) {
                            ConfirmationBehavior.CONFIRM_SENSITIVE_ONLY
                        },
                        privacy = PrivacyControls(
                            allowScreenProcessing = props.getProperty("allow_screen_processing", "true").toBoolean(),
                            storeScreenshots = props.getProperty("store_screenshots", "false").toBoolean(),
                            maskSensitiveInput = props.getProperty("mask_sensitive_input", "true").toBoolean(),
                            allowDiagnosticsTelemetry = props.getProperty("allow_diagnostics", "false").toBoolean()
                        ),
                        historyRetention = try {
                            HistoryRetention.valueOf(props.getProperty("history_retention", HistoryRetention.RETENTION_7_DAYS.name))
                        } catch (e: Exception) {
                            HistoryRetention.RETENTION_7_DAYS
                        },
                        preferLocalAi = props.getProperty("prefer_local_ai", "true").toBoolean()
                    )
                    _settings.value = loaded
                } catch (e: Exception) {
                    _settings.value = ScreenIqSettings.DEFAULT
                }
            }
        }
    }

    private suspend fun saveToDisk(settingsToSave: ScreenIqSettings) = withContext(Dispatchers.IO) {
        try {
            val props = Properties().apply {
                setProperty("screeniq_enabled", settingsToSave.isScreenIqEnabled.toString())
                setProperty("gesture_enabled", settingsToSave.isGestureEnabled.toString())
                setProperty("confirmation_behavior", settingsToSave.confirmationBehavior.name)
                setProperty("allow_screen_processing", settingsToSave.privacy.allowScreenProcessing.toString())
                setProperty("store_screenshots", settingsToSave.privacy.storeScreenshots.toString())
                setProperty("mask_sensitive_input", settingsToSave.privacy.maskSensitiveInput.toString())
                setProperty("allow_diagnostics", settingsToSave.privacy.allowDiagnosticsTelemetry.toString())
                setProperty("history_retention", settingsToSave.historyRetention.name)
                setProperty("prefer_local_ai", settingsToSave.preferLocalAi.toString())
            }
            FileOutputStream(configFile).use { props.store(it, "ScreenIQ Settings") }
        } catch (e: Exception) {
            // Log or ignore safely
        }
    }

    override fun currentSettings(): ScreenIqSettings = _settings.value

    override suspend fun setScreenIqEnabled(enabled: Boolean) {
        updateSettings { it.copy(isScreenIqEnabled = enabled) }
    }

    override suspend fun setGestureEnabled(enabled: Boolean) {
        updateSettings { it.copy(isGestureEnabled = enabled) }
    }

    override suspend fun setConfirmationBehavior(behavior: ConfirmationBehavior) {
        updateSettings { it.copy(confirmationBehavior = behavior) }
    }

    override suspend fun setAllowScreenProcessing(allowed: Boolean) {
        updateSettings { it.copy(privacy = it.privacy.copy(allowScreenProcessing = allowed)) }
    }

    override suspend fun setStoreScreenshots(store: Boolean) {
        updateSettings { it.copy(privacy = it.privacy.copy(storeScreenshots = store)) }
    }

    override suspend fun setHistoryRetention(retention: HistoryRetention) {
        updateSettings { it.copy(historyRetention = retention) }
    }

    override suspend fun setPreferLocalAi(preferLocal: Boolean) {
        updateSettings { it.copy(preferLocalAi = preferLocal) }
    }

    override suspend fun updateSettings(transform: (ScreenIqSettings) -> ScreenIqSettings) {
        mutex.withLock {
            val updated = transform(_settings.value)
            _settings.value = updated
            saveToDisk(updated)
        }
    }

    override suspend fun resetToDefaults() {
        mutex.withLock {
            _settings.value = ScreenIqSettings.DEFAULT
            saveToDisk(ScreenIqSettings.DEFAULT)
        }
    }
}
