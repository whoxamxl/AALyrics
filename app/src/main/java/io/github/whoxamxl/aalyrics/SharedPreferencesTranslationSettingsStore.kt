package io.github.whoxamxl.aalyrics

import android.content.Context
import android.content.SharedPreferences
import io.github.whoxamxl.aalyrics.translation.api.TranslationLanguages
import io.github.whoxamxl.aalyrics.translation.api.TranslationSettings
import io.github.whoxamxl.aalyrics.translation.api.TranslationSettingsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.Closeable

internal class SharedPreferencesTranslationSettingsStore(
    context: Context,
) : TranslationSettingsStore, Closeable {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    private val _settings = MutableStateFlow(readSettings())
    override val settings: StateFlow<TranslationSettings> = _settings.asStateFlow()

    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == ENABLED_KEY || key == TARGET_LANGUAGE_KEY) {
            _settings.value = readSettings()
        }
    }

    init {
        preferences.registerOnSharedPreferenceChangeListener(listener)
    }

    override fun setEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(ENABLED_KEY, enabled).apply()
    }

    override fun setTargetLanguage(languageTag: String) {
        val normalized = TranslationLanguages.normalizeTargetLanguage(languageTag)
        preferences.edit().putString(TARGET_LANGUAGE_KEY, normalized).apply()
    }

    override fun close() {
        preferences.unregisterOnSharedPreferenceChangeListener(listener)
    }

    private fun readSettings(): TranslationSettings = TranslationSettings(
        enabled = preferences.getBoolean(ENABLED_KEY, DEFAULT_ENABLED),
        targetLanguage = TranslationLanguages.normalizeTargetLanguage(
            preferences.getString(
                TARGET_LANGUAGE_KEY,
                TranslationLanguages.DEFAULT_TARGET_LANGUAGE,
            ),
        ),
    )

    private companion object {
        private const val PREFERENCES_NAME = "aalyrics_preferences"

        // Preserve the mature fork's stable key semantics without preserving its View ownership.
        private const val ENABLED_KEY = "translation_enabled"
        private const val TARGET_LANGUAGE_KEY = "translation_target_language"
        private const val DEFAULT_ENABLED = true
    }
}
