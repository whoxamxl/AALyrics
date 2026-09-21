package io.github.whoxamxl.aalyrics.translation.api

import kotlinx.coroutines.flow.StateFlow

enum class TranslationModelPhase {
    CHECKING,
    DOWNLOADING,
    WAITING_FOR_SYSTEM,
    READY,
    FAILED,
    TIMED_OUT,
}

data class TranslationModelState(
    val languageTag: String,
    val phase: TranslationModelPhase,
    val error: String? = null,
)

/**
 * Replaceable lifecycle boundary for translation-engine language models.
 *
 * Model preparation is background infrastructure. It does not translate lyrics,
 * own canonical lyrics, or publish presentation state.
 */
interface TranslationModelManager {
    val states: StateFlow<Map<String, TranslationModelState>>

    suspend fun ensureAvailable(languageTag: String): Boolean

    /**
     * Clears a latched model failure and explicitly retries preparation.
     * Ordinary ensureAvailable calls must not spin on a model that already
     * failed or timed out in the current process.
     */
    suspend fun retry(languageTag: String): Boolean

    /**
     * Deletes downloaded engine-managed translation models.
     *
     * Built-in capabilities such as English are not downloadable models and
     * therefore remain available.
     */
    suspend fun clearDownloadedModels(): Boolean

    suspend fun ensureRouteAvailable(
        sourceLanguage: String,
        targetLanguage: String,
    ): Boolean
}
