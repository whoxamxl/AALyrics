package io.github.whoxamxl.aalyrics.translation.mlkit

import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import io.github.whoxamxl.aalyrics.translation.api.TranslationLanguages
import io.github.whoxamxl.aalyrics.translation.api.TranslationModelManager
import io.github.whoxamxl.aalyrics.translation.api.TranslationProvider
import io.github.whoxamxl.aalyrics.translation.api.TranslationProviderId
import io.github.whoxamxl.aalyrics.translation.api.TranslationRoute
import io.github.whoxamxl.aalyrics.translation.api.TranslationSession
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Local Translation Provider that reuses the scaffold's model lifecycle. */
class MlKitTranslationProvider internal constructor(
    private val modelManager: TranslationModelManager,
    private val translatorFactory: MlKitTranslatorFactory,
) : TranslationProvider {
    constructor(modelManager: TranslationModelManager) : this(
        modelManager = modelManager,
        translatorFactory = ProductionMlKitTranslatorFactory,
    )

    override val id: TranslationProviderId = TranslationProviderId("mlkit")

    override suspend fun openSession(route: TranslationRoute): TranslationSession? {
        val source = TranslationLanguages.normalizeLanguageTag(route.sourceLanguage) ?: return null
        val target = TranslationLanguages.normalizeLanguageTag(route.targetLanguage) ?: return null
        if (
            source == target ||
            !TranslationLanguages.isModelSupported(source) ||
            !TranslationLanguages.isModelSupported(target)
        ) {
            return null
        }

        val sourceMlLanguage = TranslateLanguage.fromLanguageTag(source) ?: return null
        val targetMlLanguage = TranslateLanguage.fromLanguageTag(target) ?: return null
        if (!modelManager.ensureRouteAvailable(source, target)) return null

        val translator = translatorFactory.create(sourceMlLanguage, targetMlLanguage)
        return object : TranslationSession {
            override suspend fun translate(text: String): String = translator.translate(text)

            override fun close() {
                translator.close()
            }
        }
    }
}

internal interface MlKitTextTranslator {
    suspend fun translate(text: String): String

    fun close()
}

internal fun interface MlKitTranslatorFactory {
    fun create(sourceLanguage: String, targetLanguage: String): MlKitTextTranslator
}

private object ProductionMlKitTranslatorFactory : MlKitTranslatorFactory {
    override fun create(
        sourceLanguage: String,
        targetLanguage: String,
    ): MlKitTextTranslator {
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(sourceLanguage)
            .setTargetLanguage(targetLanguage)
            .build()
        return ProductionMlKitTextTranslator(Translation.getClient(options))
    }
}

private class ProductionMlKitTextTranslator(
    private val translator: Translator,
) : MlKitTextTranslator {
    override suspend fun translate(text: String): String =
        suspendCancellableCoroutine { continuation ->
            translator.translate(text)
                .addOnSuccessListener { translated ->
                    if (continuation.isActive) continuation.resume(translated)
                }
                .addOnFailureListener { error ->
                    if (continuation.isActive) continuation.resumeWithException(error)
                }
                .addOnCanceledListener { continuation.cancel() }
        }

    override fun close() {
        translator.close()
    }
}
