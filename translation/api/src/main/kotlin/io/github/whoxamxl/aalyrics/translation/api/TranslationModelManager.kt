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

    /**
     * True after persisted remote-model inventory has been successfully
     * reconciled for this process. Before that, a missing state entry is not
     * authoritative evidence that a remote model is absent.
     */
    val inventoryReconciled: StateFlow<Boolean>

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
     * therefore remain available. Implementations must also prevent models that
     * were already downloading when cleanup began from surviving after they
     * complete.
     *
     * Returns true when the current inventory/delete pass succeeded and any
     * in-flight models have been handed off to the same cleanup request. A
     * false result means the caller should surface a retryable cleanup failure.
     */
    suspend fun clearDownloadedModels(): Boolean

    suspend fun ensureRouteAvailable(
        sourceLanguage: String,
        targetLanguage: String,
    ): Boolean
}
