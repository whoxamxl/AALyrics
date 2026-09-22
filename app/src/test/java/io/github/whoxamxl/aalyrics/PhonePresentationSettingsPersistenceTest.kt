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
            ),
            persistence.read(),
        )
    }

    @Test
    fun `playback source preferences persist independently`() {
        val stored = mutableMapOf<String, Boolean>()
        val persistence = persistence(stored)

        persistence.setIgnoreNonAudioApps(false)
        persistence.setAllowUnclassifiedApps(true)

        assertEquals(false, persistence.read().ignoreNonAudioApps)
        assertEquals(true, persistence.read().allowUnclassifiedApps)
    }

    @Test
    fun `reset restores strict playback source defaults and existing presentation default`() {
        val stored = mutableMapOf<String, Boolean>()
        val persistence = persistence(stored)

        persistence.setVerboseDetailsEnabled(true)
        persistence.setIgnoreNonAudioApps(false)
        persistence.setAllowUnclassifiedApps(true)
        persistence.resetToDefaults()

        assertEquals(
            PhonePresentationSettingsSnapshot(
                verboseDetailsEnabled = false,
                ignoreNonAudioApps = true,
                allowUnclassifiedApps = false,
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
