package io.github.whoxamxl.aalyrics.translation.core

import io.github.whoxamxl.aalyrics.core.model.LyricsDocument
import io.github.whoxamxl.aalyrics.core.model.PlainLyricLine
import io.github.whoxamxl.aalyrics.translation.api.IdentifiedLanguage
import io.github.whoxamxl.aalyrics.translation.api.LanguageIdentifier
import io.github.whoxamxl.aalyrics.translation.api.TranslationProvider
import io.github.whoxamxl.aalyrics.translation.api.TranslationProviderId
import io.github.whoxamxl.aalyrics.translation.api.TranslationRoute
import io.github.whoxamxl.aalyrics.translation.api.TranslationSession
import io.github.whoxamxl.aalyrics.translation.api.TranslationSettings
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class TranslationCoordinatorTest {
    @Test
    fun `target change cancels and replaces active Translation`() = runTest {
        var cancelled = false
        val closedTargets = mutableListOf<String>()
        val provider = provider("mlkit") { route ->
            object : TranslationSession {
                override suspend fun translate(text: String): String {
                    return if (route.targetLanguage == "ja") {
                        suspendCancellableCoroutine { continuation ->
                            continuation.invokeOnCancellation { cancelled = true }
                        }
                    } else {
                        alignedOutput(text, route.targetLanguage)
                    }
                }

                override fun close() {
                    closedTargets += route.targetLanguage
                }
            }
        }
        val coordinator = coordinator(provider)
        val canonical = canonical(2)

        coordinator.update(canonical, TranslationSettings(enabled = true, targetLanguage = "ja"))
        runCurrent()
        assertIs<TranslationState.Translating>(coordinator.state.value)

        coordinator.update(canonical, TranslationSettings(enabled = true, targetLanguage = "fr"))
        advanceUntilIdle()

        assertTrue(cancelled)
        assertTrue("ja" in closedTargets)
        val ready = assertIs<TranslationState.Ready>(coordinator.state.value)
        assertEquals("fr", ready.artifact.request.targetLanguage)
        assertTrue(ready.artifact.lines.all { it.text.startsWith("fr-") })
    }

    @Test
    fun `late obsolete result cannot replace newer target artifact`() = runTest {
        var obsoleteContinuation: Continuation<String>? = null
        var obsoleteInput: String? = null
        val provider = provider("mlkit") { route ->
            session { input ->
                if (route.targetLanguage == "ja") {
                    obsoleteInput = input
                    suspendCoroutine { continuation -> obsoleteContinuation = continuation }
                } else {
                    alignedOutput(input, route.targetLanguage)
                }
            }
        }
        val coordinator = coordinator(provider)
        val canonical = canonical(2)

        coordinator.update(canonical, TranslationSettings(targetLanguage = "ja"))
        runCurrent()
        coordinator.update(canonical, TranslationSettings(targetLanguage = "fr"))
        advanceUntilIdle()
        assertEquals(
            "fr",
            assertIs<TranslationState.Ready>(coordinator.state.value).artifact.request.targetLanguage,
        )

        obsoleteContinuation?.resume(alignedOutput(requireNotNull(obsoleteInput), "obsolete"))
        advanceUntilIdle()

        val ready = assertIs<TranslationState.Ready>(coordinator.state.value)
        assertEquals("fr", ready.artifact.request.targetLanguage)
        assertTrue(ready.artifact.lines.none { it.text.startsWith("obsolete-") })
    }

    @Test
    fun `artifact publishes only after every block completes`() = runTest {
        val finalBlockGate = CompletableDeferred<Unit>()
        var callCount = 0
        val provider = provider("mlkit") {
            session { input ->
                callCount++
                if (callCount == 2) finalBlockGate.await()
                alignedOutput(input, "translated")
            }
        }
        val coordinator = coordinator(
            provider,
            planner = TranslationBlockPlanner(
                TranslationBlockPolicy(preferredCoreLines = 2, maximumCoreCharacters = 1_000),
            ),
        )

        coordinator.update(canonical(4), TranslationSettings(targetLanguage = "ja"))
        runCurrent()

        assertEquals(2, callCount)
        assertIs<TranslationState.Translating>(coordinator.state.value)

        finalBlockGate.complete(Unit)
        advanceUntilIdle()

        val ready = assertIs<TranslationState.Ready>(coordinator.state.value)
        assertEquals(4, ready.artifact.lines.size)
        assertTrue(ready.artifact.lines.all { it.translated })
    }

    @Test
    fun `provider task cancellation fails the current request instead of sticking Translating`() = runTest {
        val provider = provider("mlkit") {
            session { throw CancellationException("synthetic provider cancellation") }
        }
        val coordinator = coordinator(provider)

        coordinator.update(canonical(2), TranslationSettings(targetLanguage = "ja"))
        advanceUntilIdle()

        assertIs<TranslationState.Failed>(coordinator.state.value)
    }

    @Test
    fun `provider fallback keeps one provenance for the complete artifact`() = runTest {
        val unavailable = object : TranslationProvider {
            override val id = TranslationProviderId("unavailable")
            override suspend fun openSession(route: TranslationRoute): TranslationSession? = null
        }
        val fallback = provider("fallback") {
            session { input -> alignedOutput(input, "fallback") }
        }
        val coordinator = coordinator(unavailable, fallback)

        coordinator.update(canonical(3), TranslationSettings(targetLanguage = "ja"))
        advanceUntilIdle()

        val artifact = assertIs<TranslationState.Ready>(coordinator.state.value).artifact
        assertEquals("fallback", artifact.providerId.value)
        assertTrue(artifact.lines.all { it.text.startsWith("fallback-") })
    }

    @Test
    fun `provider failure leaves canonical lyrics unchanged`() = runTest {
        val canonical = canonical(2)
        val originalDocument = canonical.document
        val unavailable = object : TranslationProvider {
            override val id = TranslationProviderId("unavailable")
            override suspend fun openSession(route: TranslationRoute): TranslationSession? = null
        }
        val coordinator = coordinator(unavailable)

        coordinator.update(canonical, TranslationSettings(targetLanguage = "ja"))
        advanceUntilIdle()

        assertIs<TranslationState.Failed>(coordinator.state.value)
        assertSame(originalDocument, canonical.document)
        assertEquals(listOf("Original line 0", "Original line 1"), canonical.document.lines.map { it.text })
    }

    @Test
    fun `language identification failure publishes Failed without opening provider`() = runTest {
        var opened = false
        val provider = provider("unused") {
            opened = true
            session { it }
        }
        val coordinator = TranslationCoordinator(
            profiler = LanguageProfiler(
                LanguageIdentifier { error("synthetic language-id failure") },
            ),
            planner = TranslationBlockPlanner(),
            providers = listOf(provider),
            scope = this,
        )

        coordinator.update(canonical(2), TranslationSettings(targetLanguage = "ja"))
        advanceUntilIdle()

        assertIs<TranslationState.Failed>(coordinator.state.value)
        assertTrue(!opened)
    }

    @Test
    fun `target-language lyrics are a no-op without opening a provider`() = runTest {
        var opened = false
        val provider = provider("unused") {
            opened = true
            session { it }
        }
        val coordinator = coordinator(provider)

        coordinator.update(canonical(2), TranslationSettings(targetLanguage = "en"))
        advanceUntilIdle()

        assertIs<TranslationState.NotRequired>(coordinator.state.value)
        assertTrue(!opened)
    }

    private fun CoroutineScope.coordinator(
        vararg provider: TranslationProvider,
        planner: TranslationBlockPlanner = TranslationBlockPlanner(),
    ): TranslationCoordinator = TranslationCoordinator(
        profiler = LanguageProfiler(
            LanguageIdentifier { listOf(IdentifiedLanguage("en", 0.99f)) },
        ),
        planner = planner,
        providers = provider.toList(),
        scope = this,
    )

    private fun canonical(lineCount: Int): CanonicalLyrics = CanonicalLyrics.create(
        ownerId = "lookup-1",
        document = LyricsDocument(
            List(lineCount) { index -> PlainLyricLine("Original line $index") },
        ),
    )

    private fun provider(
        id: String,
        createSession: suspend (TranslationRoute) -> TranslationSession,
    ): TranslationProvider = object : TranslationProvider {
        override val id = TranslationProviderId(id)

        override suspend fun openSession(route: TranslationRoute): TranslationSession =
            createSession(route)
    }

    private fun session(
        translate: suspend (String) -> String,
    ): TranslationSession = object : TranslationSession {
        override suspend fun translate(text: String): String = translate(text)
        override fun close() = Unit
    }

    private fun alignedOutput(input: String, prefix: String): String =
        Regex("\\[\\[AALYRICS_LINE_(\\d+)]]")
            .findAll(input)
            .joinToString("\n") { match ->
                "${match.value} $prefix-${match.groupValues[1]}"
            }
}
