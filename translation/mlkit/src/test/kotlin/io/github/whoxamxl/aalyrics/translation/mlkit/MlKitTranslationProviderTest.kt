package io.github.whoxamxl.aalyrics.translation.mlkit

import io.github.whoxamxl.aalyrics.translation.api.TranslationModelManager
import io.github.whoxamxl.aalyrics.translation.api.TranslationModelState
import io.github.whoxamxl.aalyrics.translation.api.TranslationRoute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MlKitTranslationProviderTest {
    @Test
    fun `session prepares model route before creating translator`() = runTest {
        val modelManager = RecordingModelManager(routeAvailable = true)
        val translator = RecordingTranslator()
        val routes = mutableListOf<Pair<String, String>>()
        val provider = MlKitTranslationProvider(
            modelManager = modelManager,
            translatorFactory = MlKitTranslatorFactory { source, target ->
                routes += source to target
                translator
            },
        )

        val session = provider.openSession(TranslationRoute("en-US", "ja-JP"))

        assertNotNull(session)
        assertEquals(listOf("en" to "ja"), modelManager.routes)
        assertEquals(listOf("en" to "ja"), routes)
        assertEquals("translated:synthetic input", session.translate("synthetic input"))
        session.close()
        assertTrue(translator.closed)
    }

    @Test
    fun `model preparation failure rejects route without creating translator`() = runTest {
        val modelManager = RecordingModelManager(routeAvailable = false)
        var created = false
        val provider = MlKitTranslationProvider(
            modelManager = modelManager,
            translatorFactory = MlKitTranslatorFactory { _, _ ->
                created = true
                RecordingTranslator()
            },
        )

        val session = provider.openSession(TranslationRoute("ko", "ja"))

        assertNull(session)
        assertEquals(listOf("ko" to "ja"), modelManager.routes)
        assertFalse(created)
    }

    @Test
    fun `same source and target is a no-op before model preparation`() = runTest {
        val modelManager = RecordingModelManager(routeAvailable = true)
        val provider = MlKitTranslationProvider(
            modelManager = modelManager,
            translatorFactory = MlKitTranslatorFactory { _, _ -> RecordingTranslator() },
        )

        assertNull(provider.openSession(TranslationRoute("ja-JP", "ja")))
        assertTrue(modelManager.routes.isEmpty())
    }

    private class RecordingModelManager(
        private val routeAvailable: Boolean,
    ) : TranslationModelManager {
        override val states: StateFlow<Map<String, TranslationModelState>> = MutableStateFlow(emptyMap())
        val routes = mutableListOf<Pair<String, String>>()

        override suspend fun ensureAvailable(languageTag: String): Boolean = routeAvailable

        override suspend fun retry(languageTag: String): Boolean = routeAvailable

        override suspend fun clearDownloadedModels(): Boolean = true

        override suspend fun ensureRouteAvailable(
            sourceLanguage: String,
            targetLanguage: String,
        ): Boolean {
            routes += sourceLanguage to targetLanguage
            return routeAvailable
        }
    }

    private class RecordingTranslator : MlKitTextTranslator {
        var closed = false

        override suspend fun translate(text: String): String = "translated:$text"

        override fun close() {
            closed = true
        }
    }
}
