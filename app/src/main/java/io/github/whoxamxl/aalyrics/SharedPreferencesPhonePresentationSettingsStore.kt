package io.github.whoxamxl.aalyrics

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.Closeable

/**
 * Application-owned persistence for Phone presentation preferences.
 *
 * These preferences may change what the Phone UI presents, but must not alter
 * provider lookup, candidate selection, timing, Translation execution, or
 * MediaSession ownership.
 */
internal class SharedPreferencesPhonePresentationSettingsStore(
    context: Context,
) : Closeable {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    private val mutableVerboseDetailsEnabled = MutableStateFlow(readVerboseDetailsEnabled())
    val verboseDetailsEnabled: StateFlow<Boolean> =
        mutableVerboseDetailsEnabled.asStateFlow()

    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == VERBOSE_DETAILS_ENABLED_KEY) {
            mutableVerboseDetailsEnabled.value = readVerboseDetailsEnabled()
        }
    }

    init {
        preferences.registerOnSharedPreferenceChangeListener(listener)
    }

    fun setVerboseDetailsEnabled(enabled: Boolean) {
        preferences.edit()
            .putBoolean(VERBOSE_DETAILS_ENABLED_KEY, enabled)
            .apply()
    }

    override fun close() {
        preferences.unregisterOnSharedPreferenceChangeListener(listener)
    }

    private fun readVerboseDetailsEnabled(): Boolean =
        preferences.getBoolean(
            VERBOSE_DETAILS_ENABLED_KEY,
            DEFAULT_VERBOSE_DETAILS_ENABLED,
        )

    private companion object {
        private const val PREFERENCES_NAME = "aalyrics_preferences"
        private const val VERBOSE_DETAILS_ENABLED_KEY = "phone_verbose_details_enabled"
        private const val DEFAULT_VERBOSE_DETAILS_ENABLED = false
    }
}
