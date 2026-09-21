package io.github.whoxamxl.aalyrics.translation.mlkit

import java.util.concurrent.ConcurrentHashMap

/**
 * Serializes model preparation registration against explicit model cleanup.
 *
 * A preparation captures the current generation before an asynchronous
 * availability check. Cleanup advances the generation, so a stale preparation
 * cannot later publish readiness or register a new monitor/download.
 */
internal class ModelCleanupBarrier {
    private val lock = Any()
    private var generation = 0L
    private val pendingDeletions = ConcurrentHashMap.newKeySet<String>()

    fun capturePreparation(languageTag: String): Long? = synchronized(lock) {
        if (languageTag in pendingDeletions) null else generation
    }

    fun beginCleanup(activeLanguages: Set<String>) {
        synchronized(lock) {
            generation += 1
            pendingDeletions.addAll(activeLanguages)
        }
    }

    fun isCurrentPreparation(
        languageTag: String,
        preparationGeneration: Long,
    ): Boolean = synchronized(lock) {
        generation == preparationGeneration &&
            languageTag !in pendingDeletions
    }

    fun <T> runIfCurrentPreparation(
        languageTag: String,
        preparationGeneration: Long,
        block: () -> T,
    ): T? = synchronized(lock) {
        if (
            generation != preparationGeneration ||
            languageTag in pendingDeletions
        ) {
            null
        } else {
            block()
        }
    }

    fun isPendingDeletion(languageTag: String): Boolean =
        languageTag in pendingDeletions

    fun removePendingDeletion(languageTag: String) {
        pendingDeletions.remove(languageTag)
    }
}
