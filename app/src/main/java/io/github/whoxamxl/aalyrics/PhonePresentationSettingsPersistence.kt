package io.github.whoxamxl.aalyrics

internal data class PhonePresentationSettingsSnapshot(
    val verboseDetailsEnabled: Boolean = false,
    val ignoreNonAudioApps: Boolean = true,
    val allowUnclassifiedApps: Boolean = false,
)

internal class PhonePresentationSettingsPersistence(
    private val readBoolean: (key: String, defaultValue: Boolean) -> Boolean,
    private val writeBooleans: (Map<String, Boolean>) -> Unit,
) {
    fun read(): PhonePresentationSettingsSnapshot =
        PhonePresentationSettingsSnapshot(
            verboseDetailsEnabled = readBoolean(
                VERBOSE_DETAILS_ENABLED_KEY,
                DEFAULT_VERBOSE_DETAILS_ENABLED,
            ),
            ignoreNonAudioApps = readBoolean(
                IGNORE_NON_AUDIO_APPS_KEY,
                DEFAULT_IGNORE_NON_AUDIO_APPS,
            ),
            allowUnclassifiedApps = readBoolean(
                ALLOW_UNCLASSIFIED_APPS_KEY,
                DEFAULT_ALLOW_UNCLASSIFIED_APPS,
            ),
        )

    fun setVerboseDetailsEnabled(enabled: Boolean) {
        writeBooleans(mapOf(VERBOSE_DETAILS_ENABLED_KEY to enabled))
    }

    fun setIgnoreNonAudioApps(enabled: Boolean) {
        writeBooleans(mapOf(IGNORE_NON_AUDIO_APPS_KEY to enabled))
    }

    fun setAllowUnclassifiedApps(enabled: Boolean) {
        writeBooleans(mapOf(ALLOW_UNCLASSIFIED_APPS_KEY to enabled))
    }

    fun resetToDefaults() {
        writeBooleans(
            mapOf(
                VERBOSE_DETAILS_ENABLED_KEY to DEFAULT_VERBOSE_DETAILS_ENABLED,
                IGNORE_NON_AUDIO_APPS_KEY to DEFAULT_IGNORE_NON_AUDIO_APPS,
                ALLOW_UNCLASSIFIED_APPS_KEY to DEFAULT_ALLOW_UNCLASSIFIED_APPS,
            ),
        )
    }

    internal companion object {
        const val VERBOSE_DETAILS_ENABLED_KEY = "phone_verbose_details_enabled"
        const val IGNORE_NON_AUDIO_APPS_KEY = "phone_ignore_non_audio_apps"
        const val ALLOW_UNCLASSIFIED_APPS_KEY = "phone_allow_unclassified_apps"

        const val DEFAULT_VERBOSE_DETAILS_ENABLED = false
        const val DEFAULT_IGNORE_NON_AUDIO_APPS = true
        const val DEFAULT_ALLOW_UNCLASSIFIED_APPS = false
    }
}
