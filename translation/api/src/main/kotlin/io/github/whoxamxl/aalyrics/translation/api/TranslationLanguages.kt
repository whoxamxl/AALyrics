package io.github.whoxamxl.aalyrics.translation.api

import java.util.Locale

/**
 * Product-level Translation language configuration.
 *
 * The exposed target set intentionally stays smaller than the underlying engine's
 * full language catalog. Engine-specific model planning belongs in its adapter.
 */
object TranslationLanguages {
    const val DEFAULT_TARGET_LANGUAGE = "en"

    val supportedTargets: List<String> = listOf(
        "en",
        "ja",
        "fr",
        "de",
        "es",
        "ko",
        "zh",
        "it",
        "pt",
    )

    val supportedModelLanguages: Set<String> = supportedTargets.toSet()

    fun normalizeLanguageTag(languageTag: String?): String? {
        if (languageTag.isNullOrBlank()) return null
        val normalized = Locale.forLanguageTag(languageTag).language
            .ifBlank { languageTag.substringBefore('-') }
            .lowercase(Locale.US)
        return normalized.takeIf { it.isNotBlank() }
    }

    fun normalizeTargetLanguage(languageTag: String?): String {
        val normalized = normalizeLanguageTag(languageTag)
        return normalized?.takeIf { it in supportedTargets } ?: DEFAULT_TARGET_LANGUAGE
    }

    /**
     * Product-level model support. Language identification may return languages
     * outside this set; that does not make them eligible for model preparation
     * or Translation routing.
     */
    fun isModelSupported(languageTag: String?): Boolean =
        normalizeLanguageTag(languageTag) in supportedModelLanguages

    fun displayName(
        languageTag: String?,
        locale: Locale = Locale.ENGLISH,
    ): String {
        val normalized = normalizeLanguageTag(languageTag) ?: DEFAULT_TARGET_LANGUAGE
        return Locale.forLanguageTag(normalized)
            .getDisplayLanguage(locale)
            .takeIf { it.isNotBlank() }
            ?: normalized.uppercase(locale)
    }
}
