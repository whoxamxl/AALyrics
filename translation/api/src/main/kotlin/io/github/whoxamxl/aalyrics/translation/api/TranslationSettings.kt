package io.github.whoxamxl.aalyrics.translation.api

import kotlinx.coroutines.flow.StateFlow

data class TranslationSettings(
    val enabled: Boolean = false,
    val targetLanguage: String = TranslationLanguages.DEFAULT_TARGET_LANGUAGE,
) {
    init {
        require(targetLanguage in TranslationLanguages.supportedTargets) {
            "Translation target must be one of the supported target languages"
        }
        require(TranslationLanguages.normalizeLanguageTag(targetLanguage) == targetLanguage) {
            "Translation target must use its normalized language tag"
        }
    }
}

/**
 * Capability-owned settings boundary.
 *
 * Foreground Settings surfaces may update this boundary later; they must not own
 * persistence keys or concrete SharedPreferences directly.
 */
interface TranslationSettingsStore {
    val settings: StateFlow<TranslationSettings>

    fun setEnabled(enabled: Boolean)

    fun setTargetLanguage(languageTag: String)
}
