package io.github.whoxamxl.aalyrics.translation.mlkit

import kotlin.test.Test
import kotlin.test.assertEquals

class MlKitModelPlannerTest {
    @Test
    fun japaneseToEnglishNeedsJapaneseModelOnly() {
        assertEquals(listOf("ja"), MlKitModelPlanner.requiredModelLanguages("ja", "en"))
    }

    @Test
    fun englishToJapaneseNeedsJapaneseModelOnly() {
        assertEquals(listOf("ja"), MlKitModelPlanner.requiredModelLanguages("en", "ja"))
    }

    @Test
    fun japaneseToFrenchNeedsBothNonEnglishModels() {
        assertEquals(listOf("ja", "fr"), MlKitModelPlanner.requiredModelLanguages("ja", "fr"))
    }

    @Test
    fun regionalTagsNormalizeBeforePlanning() {
        assertEquals(listOf("ja", "pt"), MlKitModelPlanner.requiredModelLanguages("ja-JP", "pt-BR"))
    }

    @Test
    fun sameLanguageNeedsNoTranslationModelPreparation() {
        assertEquals(emptyList(), MlKitModelPlanner.requiredModelLanguages("ko", "ko-KR"))
    }
}
