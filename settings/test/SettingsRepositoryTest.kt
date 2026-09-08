package com.screeniq.settings.test

import com.screeniq.settings.model.ConfirmationBehavior
import com.screeniq.settings.model.HistoryRetention
import com.screeniq.settings.repository.SettingsRepositoryImpl
import java.io.File
import kotlinx.coroutines.runBlocking

/**
 * Unit test for SettingsRepository persistence and mutation.
 */
class SettingsRepositoryTest {

    fun testSettingsDefaultsAndUpdates() = runBlocking {
        val testDir = File(System.getProperty("java.io.tmpdir"), "screeniq_test_settings_${System.currentTimeMillis()}")
        testDir.mkdirs()

        try {
            val repo = SettingsRepositoryImpl(storageDir = testDir)

            // Verify defaults
            val defaults = repo.currentSettings()
            assert(defaults.isScreenIqEnabled) { "Expected ScreenIQ enabled by default" }
            assert(defaults.isGestureEnabled) { "Expected gesture enabled by default" }
            assert(!defaults.privacy.storeScreenshots) { "Expected store screenshots to be false by default" }
            assert(defaults.preferLocalAi) { "Expected local AI preferred by default" }

            // Mutate settings
            repo.setConfirmationBehavior(ConfirmationBehavior.ALWAYS_ASK)
            repo.setHistoryRetention(HistoryRetention.RETENTION_30_DAYS)
            repo.setPreferLocalAi(false)

            val updated = repo.currentSettings()
            assert(updated.confirmationBehavior == ConfirmationBehavior.ALWAYS_ASK)
            assert(updated.historyRetention == HistoryRetention.RETENTION_30_DAYS)
            assert(!updated.preferLocalAi)

            // Reset to defaults
            repo.resetToDefaults()
            val reset = repo.currentSettings()
            assert(reset.confirmationBehavior == ConfirmationBehavior.CONFIRM_SENSITIVE_ONLY)
            assert(reset.preferLocalAi)
        } finally {
            testDir.deleteRecursively()
        }
    }
}
