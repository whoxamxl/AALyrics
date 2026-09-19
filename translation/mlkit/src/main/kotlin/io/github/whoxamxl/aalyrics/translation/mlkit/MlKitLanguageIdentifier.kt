package io.github.whoxamxl.aalyrics.translation.mlkit

import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.languageid.LanguageIdentificationOptions
import io.github.whoxamxl.aalyrics.translation.api.IdentifiedLanguage
import io.github.whoxamxl.aalyrics.translation.api.LanguageIdentifier
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** ML Kit language evidence adapter used by the pure complete-document profiler. */
class MlKitLanguageIdentifier : LanguageIdentifier, AutoCloseable {
    private val client = LanguageIdentification.getClient(
        LanguageIdentificationOptions.Builder()
            .setConfidenceThreshold(MINIMUM_ENGINE_CONFIDENCE)
            .build(),
    )

    override suspend fun identifyPossibleLanguages(text: String): List<IdentifiedLanguage> {
        if (text.isBlank()) return emptyList()
        return suspendCancellableCoroutine { continuation ->
            client.identifyPossibleLanguages(text)
                .addOnSuccessListener { candidates ->
                    if (continuation.isActive) {
                        continuation.resume(
                            candidates.map { candidate ->
                                IdentifiedLanguage(
                                    languageTag = candidate.languageTag,
                                    confidence = candidate.confidence,
                                )
                            },
                        )
                    }
                }
                .addOnFailureListener { error ->
                    if (continuation.isActive) continuation.resumeWithException(error)
                }
                .addOnCanceledListener { continuation.cancel() }
        }
    }

    override fun close() {
        client.close()
    }

    private companion object {
        const val MINIMUM_ENGINE_CONFIDENCE = 0.01f
    }
}
