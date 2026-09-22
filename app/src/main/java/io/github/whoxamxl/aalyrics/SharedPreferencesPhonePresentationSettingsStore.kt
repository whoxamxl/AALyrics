package io.github.whoxamxl.aalyrics

import android.content.Context
import android.content.SharedPreferences
import io.github.whoxamxl.aalyrics.PhonePresentationSettingsPersistence.Companion.ALLOW_UNCLASSIFIED_APPS_KEY
import io.github.whoxamxl.aalyrics.PhonePresentationSettingsPersistence.Companion.IGNORE_NON_AUDIO_APPS_KEY
import io.github.whoxamxl.aalyrics.PhonePresentationSettingsPersistence.Companion.VERBOSE_DETAILS_ENABLED_KEY
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.Closeable

/**
 * Application-owned persistence for Phone presentation and playback-source preferences.
 *
 * Runtime policy may consume the playback-source eligibility settings, but Android framework
 * ownership and Settings presentation remain outside this store.
 */
internal class SharedPreferencesPhonePresentationSettingsStore(
    context: Context,
) : Closeable {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )
    private val persistence = PhonePresentationSettingsPersistence(
        readBoolean = preferences::getBoolean,
        writeBooleans = { values ->
            preferences.edit()
                .also { editor ->
                    values.forEach { (key, value) ->
                        editor.putBoolean(key, value)
                    }
                }
                .apply()
        },
    )

    private val initialSettings = persistence.read()

    private val mutableVerboseDetailsEnabled =
        MutableStateFlow(initialSettings.verboseDetailsEnabled)
    val verboseDetailsEnabled: StateFlow<Boolean> =
        mutableVerboseDetailsEnabled.asStateFlow()

    private val mutableIgnoreNonAudioApps =
        MutableStateFlow(initialSettings.ignoreNonAudioApps)
    val ignoreNonAudioApps: StateFlow<Boolean> =
        mutableIgnoreNonAudioApps.asStateFlow()

    private val mutableAllowUnclassifiedApps =
        MutableStateFlow(initialSettings.allowUnclassifiedApps)
    val allowUnclassifiedApps: StateFlow<Boolean> =
        mutableAllowUnclassifiedApps.asStateFlow()

    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        val settings = persistence.read()
        when (key) {
            VERBOSE_DETAILS_ENABLED_KEY ->
                mutableVerboseDetailsEnabled.value = settings.verboseDetailsEnabled
            IGNORE_NON_AUDIO_APPS_KEY ->
                mutableIgnoreNonAudioApps.value = settings.ignoreNonAudioApps
            ALLOW_UNCLASSIFIED_APPS_KEY ->
                mutableAllowUnclassifiedApps.value = settings.allowUnclassifiedApps
        }
    }

    init {
        preferences.registerOnSharedPreferenceChangeListener(listener)
    }

    fun setVerboseDetailsEnabled(enabled: Boolean) {
        mutableVerboseDetailsEnabled.value = enabled
        persistence.setVerboseDetailsEnabled(enabled)
    }

    fun setIgnoreNonAudioApps(enabled: Boolean) {
        mutableIgnoreNonAudioApps.value = enabled
        persistence.setIgnoreNonAudioApps(enabled)
    }

    fun setAllowUnclassifiedApps(enabled: Boolean) {
        mutableAllowUnclassifiedApps.value = enabled
        persistence.setAllowUnclassifiedApps(enabled)
    }

    fun resetToDefaults() {
        val defaults = PhonePresentationSettingsSnapshot()
        mutableVerboseDetailsEnabled.value = defaults.verboseDetailsEnabled
        mutableIgnoreNonAudioApps.value = defaults.ignoreNonAudioApps
        mutableAllowUnclassifiedApps.value = defaults.allowUnclassifiedApps
        persistence.resetToDefaults()
    }

    override fun close() {
        preferences.unregisterOnSharedPreferenceChangeListener(listener)
    }

    private companion object {
        private const val PREFERENCES_NAME = "aalyrics_preferences"
    }
}
