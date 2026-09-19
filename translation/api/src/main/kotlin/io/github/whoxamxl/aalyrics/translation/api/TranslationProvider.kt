package io.github.whoxamxl.aalyrics.translation.api

@JvmInline
value class TranslationProviderId(val value: String) {
    init {
        require(value.isNotBlank()) { "Translation provider id must not be blank" }
    }
}

data class TranslationRoute(
    val sourceLanguage: String,
    val targetLanguage: String,
) {
    init {
        require(sourceLanguage.isNotBlank()) { "Translation source language must not be blank" }
        require(targetLanguage.isNotBlank()) { "Translation target language must not be blank" }
    }
}

/** One prepared source/target route owned by a single Translation Provider. */
interface TranslationSession : AutoCloseable {
    suspend fun translate(text: String): String

    override fun close()
}

/** Translation-specific provider boundary, intentionally separate from Lyrics Providers. */
interface TranslationProvider {
    val id: TranslationProviderId

    /** Returns null when this provider cannot prepare the requested route. */
    suspend fun openSession(route: TranslationRoute): TranslationSession?
}
