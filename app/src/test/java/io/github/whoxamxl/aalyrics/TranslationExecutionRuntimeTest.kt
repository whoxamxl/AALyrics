package io.github.whoxamxl.aalyrics

import io.github.whoxamxl.aalyrics.core.lyrics.LyricsLookup
import io.github.whoxamxl.aalyrics.core.lyrics.LyricsLookupId
import io.github.whoxamxl.aalyrics.core.lyrics.LyricsState
import io.github.whoxamxl.aalyrics.core.model.LyricsDocument
import io.github.whoxamxl.aalyrics.core.model.PlainLyricLine
import io.github.whoxamxl.aalyrics.core.model.Track
import io.github.whoxamxl.aalyrics.translation.api.TranslationLanguages
import io.github.whoxamxl.aalyrics.translation.api.TranslationSettings
import io.github.whoxamxl.aalyrics.translation.api.TranslationSettingsStore
import io.github.whoxamxl.aalyrics.translation.core.CanonicalLyrics
import io.github.whoxamxl.aalyrics.translation.core.LanguageProfile
import io.github.whoxamxl.aalyrics.translation.core.ProfiledLineRole
import io.github.whoxamxl.aalyrics.translation.core.ProfiledLyricLine
import io.github.whoxamxl.aalyrics.translation.core.SecondaryActivation
import io.github.whoxamxl.aalyrics.translation.core.TranslationFailureReason
import io.github.whoxamxl.aalyrics.translation.core.TranslationRequestId
import io.github.whoxamxl.aalyrics.translation.core.TranslationRequestIdentity
import io.github.whoxamxl.aalyrics.translation.core.TranslationState
import io.github.whoxamxl.aalyrics.translation.core.TranslationLifecycle
import io.github.whoxamxl.aalyrics.translation.core.TranslationState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertSame

@OptIn(ExperimentalCoroutinesApi::class)
class TranslationExecutionRuntimeTest {
    @Test
    fun `retry model selection stays on current failed route`() {
        val canonical = CanonicalLyrics.create(
            ownerId = "retry-route",
            document = LyricsDocument(listOf(PlainLyricLine("Line"))),
        )
        val request = TranslationRequestIdentity(
            id = TranslationRequestId(7L),
            canonicalLyrics = canonical.identity,
            targetLanguage = "ja",
        )
        val state = TranslationState.Failed(
            request = request,
            profile = LanguageProfile(
                primary = "en",
                secondaryCandidate = "es",
                secondaryActivation = SecondaryActivation.ACTIVE,
                lines = listOf(
                    ProfiledLyricLine(
                        index = 0,
                        languageTag = "en",
                        confidence = 0.99f,
                        role = ProfiledLineRole.PRIMARY,
                    ),
                ),
            ),
            reason = TranslationFailureReason.PROVIDER_EXECUTION_FAILED,
        )

        assertEquals(
            setOf("ja", "es"),
            translationRetryModelLanguages(
                state = state,
                settings = TranslationSettings(enabled = true, targetLanguage = "ja"),
            ),
        )
    }

    @Test
    fun `retry model selection ignores stale route profile and built in English`() {
        val canonical = CanonicalLyrics.create(
            ownerId = "stale-retry-route",
            document = LyricsDocument(listOf(PlainLyricLine("Line"))),
        )
        val staleState = TranslationState.Failed(
            request = TranslationRequestIdentity(
                id = TranslationRequestId(8L),
                canonicalLyrics = canonical.identity,
                targetLanguage = "fr",
            ),
            profile = LanguageProfile(
                primary = "es",
                secondaryCandidate = null,
                secondaryActivation = SecondaryActivation.NONE,
                lines = emptyList(),
            ),
        )

        assertEquals(
            setOf("ja"),
            translationRetryModelLanguages(
                state = staleState,
                settings = TranslationSettings(enabled = true, targetLanguage = "ja"),
            ),
        )
        assertEquals(
            emptySet(),
            translationRetryModelLanguages(
                state = TranslationState.Idle,
                settings = TranslationSettings(enabled = true, targetLanguage = "en"),
            ),
        )
    }

    @Test
    fun `runtime maps completed canonical lyrics and settings into Translation ownership`() = runTest {
        val lyricsState = MutableStateFlow<LyricsState>(LyricsState.Idle)
        val settings = FakeSettingsStore()
        val lifecycle = RecordingLifecycle()
        val runtime = TranslationExecutionRuntime(
            lyricsState = lyricsState,
            settingsStore = settings,
            lifecycle = lifecycle,
            applicationScope = this,
        )

        runtime.start()
        runCurrent()
        val document = LyricsDocument(listOf(PlainLyricLine("Synthetic lyric line")))
        lyricsState.value = LyricsState.Ready(lookup(1), document)
        runCurrent()

        val firstCanonical = requireNotNull(lifecycle.updates.last().first)
        assertSame(document, firstCanonical.document)
        assertEquals(TranslationLanguages.DEFAULT_TARGET_LANGUAGE, lifecycle.updates.last().second.targetLanguage)

        settings.setTargetLanguage("ja-JP")
        runCurrent()
        val targetUpdate = lifecycle.updates.last()
        assertEquals(firstCanonical.identity, targetUpdate.first?.identity)
        assertEquals("ja", targetUpdate.second.targetLanguage)

        val replacement = document.copy(lines = listOf(PlainLyricLine("Replacement synthetic line")))
        lyricsState.value = LyricsState.Degraded(lookup(2), replacement, failedAttempts = 1)
        runCurrent()
        assertNotEquals(firstCanonical.identity, lifecycle.updates.last().first?.identity)

        runtime.stop()
        assertEquals(1, lifecycle.clearCalls)
    }

    @Test
    fun `retry republishes current canonical lyrics and settings after clearing lifecycle`() = runTest {
        val document = LyricsDocument(listOf(PlainLyricLine("Synthetic lyric line")))
        val lyricsState = MutableStateFlow<LyricsState>(LyricsState.Ready(lookup(1), document))
        val settings = FakeSettingsStore().apply {
            setEnabled(true)
            setTargetLanguage("ja")
        }
        val lifecycle = RecordingLifecycle()
        val runtime = TranslationExecutionRuntime(
            lyricsState = lyricsState,
            settingsStore = settings,
            lifecycle = lifecycle,
            applicationScope = this,
        )

        runtime.retry()

        assertEquals(1, lifecycle.clearCalls)
        val (canonical, retrySettings) = lifecycle.updates.single()
        assertSame(document, canonical?.document)
        assertEquals(true, retrySettings.enabled)
        assertEquals("ja", retrySettings.targetLanguage)
    }

    @Test
    fun `disabling Translation retains canonical input for lifecycle cancellation`() = runTest {
        val document = LyricsDocument(listOf(PlainLyricLine("Synthetic lyric line")))
        val lyricsState = MutableStateFlow<LyricsState>(LyricsState.Ready(lookup(1), document))
        val settings = FakeSettingsStore()
        val lifecycle = RecordingLifecycle()
        val runtime = TranslationExecutionRuntime(
            lyricsState = lyricsState,
            settingsStore = settings,
            lifecycle = lifecycle,
            applicationScope = this,
        )

        runtime.start()
        runCurrent()
        settings.setEnabled(false)
        runCurrent()

        val (canonical, disabledSettings) = lifecycle.updates.last()
        assertSame(document, canonical?.document)
        assertEquals(false, disabledSettings.enabled)
        runtime.stop()
    }

    private fun lookup(id: Long): LyricsLookup = LyricsLookup(
        id = LyricsLookupId(id),
        track = Track(title = "Synthetic track", artists = listOf("Synthetic artist")),
    )

    private class FakeSettingsStore : TranslationSettingsStore {
        private val mutableSettings = MutableStateFlow(TranslationSettings())
        override val settings: StateFlow<TranslationSettings> = mutableSettings

        override fun setEnabled(enabled: Boolean) {
            mutableSettings.value = mutableSettings.value.copy(enabled = enabled)
        }

        override fun setTargetLanguage(languageTag: String) {
            mutableSettings.value = mutableSettings.value.copy(
                targetLanguage = TranslationLanguages.normalizeTargetLanguage(languageTag),
            )
        }
    }

    private class RecordingLifecycle : TranslationLifecycle {
        override val state: StateFlow<TranslationState> = MutableStateFlow(TranslationState.Idle)
        val updates = mutableListOf<Pair<CanonicalLyrics?, TranslationSettings>>()
        var clearCalls = 0

        override fun update(
            canonical: CanonicalLyrics?,
            settings: TranslationSettings,
        ) {
            updates += canonical to settings
        }

        override fun clear() {
            clearCalls++
        }
    }
}
