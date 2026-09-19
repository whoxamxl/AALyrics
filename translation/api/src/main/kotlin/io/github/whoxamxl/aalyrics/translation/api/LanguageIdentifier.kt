package io.github.whoxamxl.aalyrics.translation.api

data class IdentifiedLanguage(
    val languageTag: String,
    val confidence: Float,
) {
    init {
        require(languageTag.isNotBlank()) { "Identified language tag must not be blank" }
        require(confidence in 0f..1f) { "Language confidence must be between zero and one" }
    }
}

/** Engine-independent language evidence consumed by complete-document profiling. */
fun interface LanguageIdentifier {
    suspend fun identifyPossibleLanguages(text: String): List<IdentifiedLanguage>
}
