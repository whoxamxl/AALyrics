package io.github.whoxamxl.aalyrics.translation.api

import kotlin.test.Test
import kotlin.test.assertEquals

class TranslationLanguagesTest {
    @Test
    fun regionalTagsNormalizeToLanguage() {
        assertEquals("ja", TranslationLanguages.normalizeLanguageTag("ja-JP"))
        assertEquals("pt", TranslationLanguages.normalizeLanguageTag("pt-BR"))
    }

    @Test
    fun unsupportedTargetFallsBackToEnglish() {
        assertEquals("en", TranslationLanguages.normalizeTargetLanguage("xx"))
    }

    @Test
    fun supportedTargetIsPreserved() {
        assertEquals("ko", TranslationLanguages.normalizeTargetLanguage("ko-KR"))
    }

    @Test
    fun modelSupportUsesTheNineLanguageProductScope() {
        assertEquals(true, TranslationLanguages.isModelSupported("pt-BR"))
        assertEquals(true, TranslationLanguages.isModelSupported("en"))
        assertEquals(false, TranslationLanguages.isModelSupported("ar"))
        assertEquals(false, TranslationLanguages.isModelSupported("ru"))
        assertEquals(false, TranslationLanguages.isModelSupported(null))
        assertEquals(
            setOf("en", "ja", "fr", "de", "es", "ko", "zh", "it", "pt"),
            TranslationLanguages.supportedModelLanguages,
        )
    }

    @Test
    fun supportedTargetSetMatchesProductScope() {
        assertEquals(
            listOf("en", "ja", "fr", "de", "es", "ko", "zh", "it", "pt"),
            TranslationLanguages.supportedTargets,
        )
    }
}
