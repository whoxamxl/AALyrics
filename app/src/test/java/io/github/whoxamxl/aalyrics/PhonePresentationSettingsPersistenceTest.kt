package io.github.whoxamxl.aalyrics

import kotlin.test.Test
import kotlin.test.assertEquals

class PhonePresentationSettingsPersistenceTest {
    @Test
    fun `fresh preferences use strict playback source defaults`() {
        val stored = mutableMapOf<String, Boolean>()
        val persistence = persistence(stored)

        assertEquals(
            PhonePresentationSettingsSnapshot(
                verboseDetailsEnabled = false,
                ignoreNonAudioApps = true,
                allowUnclassifiedApps = false,
                automaticallyCheckForUpdates = true,
                karaokeFeatureEnabled = false,
                karaokeModeEnabled = false,
            ),
            persistence.read(),
        )
    }

    @Test
    fun `presentation preferences persist independently`() {
        val stored = mutableMapOf<String, Boolean>()
        val persistence = persistence(stored)

        persistence.setIgnoreNonAudioApps(false)
        persistence.setAllowUnclassifiedApps(true)
        persistence.setAutomaticallyCheckForUpdates(false)

        assertEquals(false, persistence.read().ignoreNonAudioApps)
        assertEquals(true, persistence.read().allowUnclassifiedApps)
        assertEquals(false, persistence.read().automaticallyCheckForUpdates)
    }

    @Test
    fun `karaoke mode requires feature gate and clearing gate clears stored mode`() {
        val stored = mutableMapOf<String, Boolean>()
        val persistence = persistence(stored)

        persistence.setKaraokeModeEnabled(true)
        assertEquals(false, persistence.read().karaokeModeEnabled)

        persistence.setKaraokeFeatureEnabled(true)
        persistence.setKaraokeModeEnabled(true)
        assertEquals(true, persistence.read().karaokeModeEnabled)

        persistence.setKaraokeFeatureEnabled(false)
        assertEquals(false, persistence.read().karaokeModeEnabled)
        assertEquals(false, stored[PhonePresentationSettingsPersistence.KARAOKE_MODE_ENABLED_KEY])
    }

    @Test
    fun `reset restores strict playback source defaults and existing presentation default`() {
        val stored = mutableMapOf<String, Boolean>()
        val persistence = persistence(stored)

        persistence.setVerboseDetailsEnabled(true)
        persistence.setIgnoreNonAudioApps(false)
        persistence.setAllowUnclassifiedApps(true)
        persistence.setAutomaticallyCheckForUpdates(false)
        persistence.setKaraokeFeatureEnabled(true)
        persistence.setKaraokeModeEnabled(true)
        persistence.resetToDefaults()

        assertEquals(
            PhonePresentationSettingsSnapshot(
                verboseDetailsEnabled = false,
                ignoreNonAudioApps = true,
                allowUnclassifiedApps = false,
                automaticallyCheckForUpdates = true,
                karaokeFeatureEnabled = false,
                karaokeModeEnabled = false,
            ),
            persistence.read(),
        )
    }

    private fun persistence(
        stored: MutableMap<String, Boolean>,
    ) = PhonePresentationSettingsPersistence(
        readBoolean = { key, defaultValue -> stored[key] ?: defaultValue },
        writeBooleans = { values -> stored.putAll(values) },
    )
}
