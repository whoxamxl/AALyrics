package io.github.whoxamxl.aalyrics.translation.mlkit

import io.github.whoxamxl.aalyrics.translation.api.TranslationLanguages

internal object MlKitModelPlanner {
    fun requiredModelLanguages(
        sourceLanguage: String,
        targetLanguage: String,
    ): List<String> {
        val source = TranslationLanguages.normalizeLanguageTag(sourceLanguage)
        val target = TranslationLanguages.normalizeLanguageTag(targetLanguage)

        if (source == null || target == null || source == target) return emptyList()

        // ML Kit ships English translation support with the SDK. Other languages
        // are downloaded as language-specific packs and are reusable across routes.
        return listOf(source, target)
            .filter { it != TranslationLanguages.DEFAULT_TARGET_LANGUAGE }
            .distinct()
    }
}
