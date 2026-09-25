package io.github.whoxamxl.aalyrics

import io.github.whoxamxl.aalyrics.core.lyrics.LyricsLookup
import io.github.whoxamxl.aalyrics.core.lyrics.LyricsLookupId
import io.github.whoxamxl.aalyrics.core.lyrics.LyricsState
import io.github.whoxamxl.aalyrics.core.model.LyricsAttribution
import io.github.whoxamxl.aalyrics.core.model.LyricsDocument
import io.github.whoxamxl.aalyrics.core.model.LyricsSyncType
import io.github.whoxamxl.aalyrics.core.model.PlaybackSnapshot
import io.github.whoxamxl.aalyrics.core.model.PlaybackSource
import io.github.whoxamxl.aalyrics.core.model.PlaybackStatus
import io.github.whoxamxl.aalyrics.core.model.TimedLyricLine
import io.github.whoxamxl.aalyrics.core.model.TimedWord
import io.github.whoxamxl.aalyrics.core.model.Track
import io.github.whoxamxl.aalyrics.core.model.TrackReference
import io.github.whoxamxl.aalyrics.core.timing.LyricsTimingOffset
import io.github.whoxamxl.aalyrics.translation.api.TranslationModelPhase
import io.github.whoxamxl.aalyrics.translation.api.TranslationModelState
import io.github.whoxamxl.aalyrics.translation.api.TranslationProviderId
import io.github.whoxamxl.aalyrics.translation.api.TranslationSettings
import io.github.whoxamxl.aalyrics.translation.core.LanguageProfile
import io.github.whoxamxl.aalyrics.translation.core.SecondaryActivation
import io.github.whoxamxl.aalyrics.translation.core.TranslationArtifact
import io.github.whoxamxl.aalyrics.translation.core.TranslationArtifactLine
import io.github.whoxamxl.aalyrics.translation.core.TranslationRequestId
import io.github.whoxamxl.aalyrics.translation.core.TranslationRequestIdentity
import io.github.whoxamxl.aalyrics.translation.core.TranslationState
import io.github.whoxamxl.aalyrics.ui.phone.lyrics.LyricsViewportInteractionMode
import io.github.whoxamxl.aalyrics.ui.phone.lyrics.TrackCardLyricsStatus
import io.github.whoxamxl.aalyrics.ui.phone.lyrics.TrackCardTranslationUiState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PhoneLyricsMapperTest {
    private val enabledTranslation = TranslationSettings(enabled = true, targetLanguage = "en")

    @Test
    fun `matching Ready projects only translated lines without changing canonical presentation`() {
        val playback = PlaybackSnapshot(
            track = track(),
            source = PlaybackSource("com.spotify.music"),
            positionMs = 6_000L,
        )
        val lyrics = ready(
            playback,
            listOf(TimedLyricLine("First", 0L), TimedLyricLine("Second", 5_000L)),
        )
        val baseline = mapPhoneLyricsState(
            playback, lyrics, true, LyricsViewportInteractionMode.FOLLOW, 1_000L,
        )
        val projected = mapPhoneLyricsState(
            playback, lyrics, true, LyricsViewportInteractionMode.FOLLOW, 1_000L,
            translationState = translated(lyrics, listOf("Translated first" to true, "Second" to false)),
            translationSettings = enabledTranslation,
        )

        assertEquals(listOf("Translated first", null), projected.viewport.lines.map { it.translatedText })
        assertEquals(
            baseline.copy(
                trackCard = baseline.trackCard.copy(
                    translation = projected.trackCard.translation,
                ),
                viewport = baseline.viewport.copy(
                    lines = projected.viewport.lines.map { it.copy(translatedText = null) },
                ),
            ),
            projected.copy(
                viewport = projected.viewport.copy(
                    lines = projected.viewport.lines.map { it.copy(translatedText = null) },
                ),
            ),
        )
        assertEquals(
            TrackCardTranslationUiState.Ready(
                sourceLanguageLabel = "JA",
                targetLanguageLabel = "EN",
            ),
            projected.trackCard.translation,
        )
        assertEquals(1, projected.viewport.currentLineIndex)
    }

    @Test
    fun `Ready is rejected for stale lyrics target and disabled settings`() {
        val playback = PlaybackSnapshot(track = track(), source = PlaybackSource("com.spotify.music"))
        val lyrics = ready(playback, listOf(TimedLyricLine("First", 0L)))
        val artifact = translated(lyrics, listOf("Translated" to true))
        val staleLyrics = lyrics.copy(lyrics = lyrics.lyrics.copy(
            lines = listOf(TimedLyricLine("Changed", 0L)),
        ))
        val newLookup = lyrics.copy(lookup = lyrics.lookup.copy(id = LyricsLookupId(99L)))
        val cases = listOf(
            Triple(staleLyrics, artifact, enabledTranslation),
            Triple(newLookup, artifact, enabledTranslation),
            Triple(lyrics, artifact, TranslationSettings(enabled = true, targetLanguage = "ja")),
            Triple(lyrics, artifact, enabledTranslation.copy(enabled = false)),
        )
        cases.forEach { (source, state, settings) ->
            val result = mapPhoneLyricsState(
                playback, source, true, LyricsViewportInteractionMode.FOLLOW, 1_000L,
                translationState = state,
                translationSettings = settings,
            )
            assertNull(result.viewport.lines.single().translatedText)
        }
    }

    @Test
    fun `blank translated text leaves a plain canonical row unchanged`() {
        val playback = PlaybackSnapshot(track = track(), source = PlaybackSource("com.spotify.music"))
        val lyrics = ready(playback, listOf(TimedLyricLine("Original", 0L)))
        val result = mapPhoneLyricsState(
            playback, lyrics, true, LyricsViewportInteractionMode.FOLLOW, 1_000L,
            translationState = translated(lyrics, listOf("  " to true)),
            translationSettings = enabledTranslation,
        )
        assertEquals("Original", result.viewport.lines.single().text)
        assertNull(result.viewport.lines.single().translatedText)
    }

    @Test
    fun `non Ready Translation states preserve Ready and Degraded original lyrics`() {
        val playback = PlaybackSnapshot(track = track(), source = PlaybackSource("com.spotify.music"))
        val ready = ready(playback, listOf(TimedLyricLine("Original", 0L)))
        val request = (translated(ready, listOf("Translated" to true)) as TranslationState.Ready)
            .artifact.request
        val profile = LanguageProfile(null, null, SecondaryActivation.NONE, emptyList())
        val states = listOf(
            TranslationState.Disabled,
            TranslationState.Idle,
            TranslationState.Translating(request),
            TranslationState.NotRequired(request, profile),
            TranslationState.Failed(request),
        )
        val canonicalStates = listOf(
            ready,
            LyricsState.Degraded(ready.lookup, ready.lyrics, failedAttempts = 1),
        )
        canonicalStates.forEach { lyrics ->
            states.forEach { translation ->
                val result = mapPhoneLyricsState(
                    playback, lyrics, true, LyricsViewportInteractionMode.FOLLOW, 1_000L,
                    translationState = translation,
                    translationSettings = enabledTranslation,
                )
                assertEquals("Original", result.viewport.lines.single().text)
                assertNull(result.viewport.lines.single().translatedText)
                assertEquals(TrackCardLyricsStatus.READY, result.trackCard.lyricsStatus)
            }
            val result = mapPhoneLyricsState(
                playback, lyrics, true, LyricsViewportInteractionMode.FOLLOW, 1_000L,
                translationState = translated(ready, listOf("Translated" to true)),
                translationSettings = enabledTranslation,
            )
            assertEquals("Translated", result.viewport.lines.single().translatedText)
        }
    }

    @Test
    fun `Track Card Translation state maps disabled enabled and target model preparation`() {
        val playback = PlaybackSnapshot()
        val off = mapPhoneLyricsState(
            playback = playback,
            lyricsState = LyricsState.Idle,
            plainLyricsAutoScrollEnabled = true,
            interactionMode = LyricsViewportInteractionMode.FOLLOW,
            currentMonotonicTimeMs = 1_000L,
        )
        assertEquals(TrackCardTranslationUiState.Off, off.trackCard.translation)

        val enabled = mapPhoneLyricsState(
            playback = playback,
            lyricsState = LyricsState.Idle,
            plainLyricsAutoScrollEnabled = true,
            interactionMode = LyricsViewportInteractionMode.FOLLOW,
            currentMonotonicTimeMs = 1_000L,
            translationSettings = enabledTranslation,
        )
        assertEquals(TrackCardTranslationUiState.Enabled, enabled.trackCard.translation)

        val downloading = mapPhoneLyricsState(
            playback = playback,
            lyricsState = LyricsState.Idle,
            plainLyricsAutoScrollEnabled = true,
            interactionMode = LyricsViewportInteractionMode.FOLLOW,
            currentMonotonicTimeMs = 1_000L,
            translationSettings = enabledTranslation,
            translationModelStates = mapOf(
                "en" to TranslationModelState("en", TranslationModelPhase.DOWNLOADING),
            ),
        )
        assertEquals(
            TrackCardTranslationUiState.Enabled,
            downloading.trackCard.translation,
        )

        val failed = mapPhoneLyricsState(
            playback = playback,
            lyricsState = LyricsState.Idle,
            plainLyricsAutoScrollEnabled = true,
            interactionMode = LyricsViewportInteractionMode.FOLLOW,
            currentMonotonicTimeMs = 1_000L,
            translationSettings = enabledTranslation,
            translationModelStates = mapOf(
                "en" to TranslationModelState("en", TranslationModelPhase.TIMED_OUT),
            ),
        )
        assertEquals(TrackCardTranslationUiState.Enabled, failed.trackCard.translation)
    }

    @Test
    fun `Ready Track Card source prefers an actually translated Secondary route`() {
        val playback = PlaybackSnapshot(
            track = track(),
            source = PlaybackSource("com.spotify.music"),
        )
        val lyrics = ready(
            playback = playback,
            lines = listOf(
                TimedLyricLine("English primary", 0L),
                TimedLyricLine("Spanish secondary", 5_000L),
            ),
        )
        val canonical = requireNotNull(lyrics.canonicalLyricsOrNull())
        val artifact = TranslationArtifact(
            request = TranslationRequestIdentity(
                id = TranslationRequestId(42L),
                canonicalLyrics = canonical.identity,
                targetLanguage = "en",
            ),
            providerId = TranslationProviderId("mlkit"),
            profile = LanguageProfile(
                primary = "en",
                secondaryCandidate = "es",
                secondaryActivation = SecondaryActivation.ACTIVE,
                lines = emptyList(),
            ),
            lines = listOf(
                TranslationArtifactLine(
                    canonicalLineIndex = 0,
                    text = "English primary",
                    sourceLanguage = "en",
                    translated = false,
                ),
                TranslationArtifactLine(
                    canonicalLineIndex = 1,
                    text = "Spanish translated",
                    sourceLanguage = "es",
                    translated = true,
                ),
            ),
        )

        val state = mapPhoneLyricsState(
            playback = playback,
            lyricsState = lyrics,
            plainLyricsAutoScrollEnabled = true,
            interactionMode = LyricsViewportInteractionMode.FOLLOW,
            currentMonotonicTimeMs = 1_000L,
            translationState = TranslationState.Ready(artifact),
            translationSettings = TranslationSettings(
                enabled = true,
                targetLanguage = "en",
            ),
        )

        assertEquals(
            TrackCardTranslationUiState.Ready("ES", "EN"),
            state.trackCard.translation,
        )
    }

    @Test
    fun `Track Card Translation state distinguishes translating model download ready not required and failed`() {
        val playback = PlaybackSnapshot(
            track = track(),
            source = PlaybackSource("com.spotify.music"),
        )
        val lyrics = ready(playback, listOf(TimedLyricLine("Original", 0L)))
        val readyState = translated(lyrics, listOf("Translated" to true)) as TranslationState.Ready
        val request = readyState.artifact.request

        val translating = mapPhoneLyricsState(
            playback = playback,
            lyricsState = lyrics,
            plainLyricsAutoScrollEnabled = true,
            interactionMode = LyricsViewportInteractionMode.FOLLOW,
            currentMonotonicTimeMs = 1_000L,
            translationState = TranslationState.Translating(request),
            translationSettings = enabledTranslation,
        )
        assertEquals(TrackCardTranslationUiState.Translating, translating.trackCard.translation)

        val downloading = mapPhoneLyricsState(
            playback = playback,
            lyricsState = lyrics,
            plainLyricsAutoScrollEnabled = true,
            interactionMode = LyricsViewportInteractionMode.FOLLOW,
            currentMonotonicTimeMs = 1_000L,
            translationState = TranslationState.Translating(
                request = request,
                profile = LanguageProfile(
                    primary = "ja",
                    secondaryCandidate = null,
                    secondaryActivation = SecondaryActivation.NONE,
                    lines = emptyList(),
                ),
            ),
            translationSettings = enabledTranslation,
            translationModelStates = mapOf(
                "ja" to TranslationModelState("ja", TranslationModelPhase.DOWNLOADING),
            ),
        )
        assertEquals(
            TrackCardTranslationUiState.DownloadingModels,
            downloading.trackCard.translation,
        )

        val ready = mapPhoneLyricsState(
            playback = playback,
            lyricsState = lyrics,
            plainLyricsAutoScrollEnabled = true,
            interactionMode = LyricsViewportInteractionMode.FOLLOW,
            currentMonotonicTimeMs = 1_000L,
            translationState = readyState,
            translationSettings = enabledTranslation,
        )
        assertEquals(
            TrackCardTranslationUiState.Ready("JA", "EN"),
            ready.trackCard.translation,
        )

        val profile = readyState.artifact.profile
        val notRequired = mapPhoneLyricsState(
            playback = playback,
            lyricsState = lyrics,
            plainLyricsAutoScrollEnabled = true,
            interactionMode = LyricsViewportInteractionMode.FOLLOW,
            currentMonotonicTimeMs = 1_000L,
            translationState = TranslationState.NotRequired(request, profile),
            translationSettings = enabledTranslation,
        )
        assertEquals(
            TrackCardTranslationUiState.NotRequired,
            notRequired.trackCard.translation,
        )

        val failed = mapPhoneLyricsState(
            playback = playback,
            lyricsState = lyrics,
            plainLyricsAutoScrollEnabled = true,
            interactionMode = LyricsViewportInteractionMode.FOLLOW,
            currentMonotonicTimeMs = 1_000L,
            translationState = TranslationState.Failed(request),
            translationSettings = enabledTranslation,
        )
        assertEquals(TrackCardTranslationUiState.Failed, failed.trackCard.translation)

        val unrelatedDownloadWhileTranslating = mapPhoneLyricsState(
            playback = playback,
            lyricsState = lyrics,
            plainLyricsAutoScrollEnabled = true,
            interactionMode = LyricsViewportInteractionMode.FOLLOW,
            currentMonotonicTimeMs = 1_000L,
            translationState = TranslationState.Translating(
                request = request,
                profile = LanguageProfile(
                    primary = "ja",
                    secondaryCandidate = null,
                    secondaryActivation = SecondaryActivation.NONE,
                    lines = emptyList(),
                ),
            ),
            translationSettings = enabledTranslation,
            translationModelStates = mapOf(
                "es" to TranslationModelState("es", TranslationModelPhase.DOWNLOADING),
            ),
        )
        assertEquals(
            TrackCardTranslationUiState.Translating,
            unrelatedDownloadWhileTranslating.trackCard.translation,
        )

        val unsupportedSecondaryDownload = mapPhoneLyricsState(
            playback = playback,
            lyricsState = lyrics,
            plainLyricsAutoScrollEnabled = true,
            interactionMode = LyricsViewportInteractionMode.FOLLOW,
            currentMonotonicTimeMs = 1_000L,
            translationState = TranslationState.Translating(
                request = request,
                profile = LanguageProfile(
                    primary = "ja",
                    secondaryCandidate = "ar",
                    secondaryActivation = SecondaryActivation.ACTIVE,
                    lines = emptyList(),
                ),
            ),
            translationSettings = enabledTranslation,
            translationModelStates = mapOf(
                "ar" to TranslationModelState("ar", TranslationModelPhase.DOWNLOADING),
            ),
        )
        assertEquals(
            TrackCardTranslationUiState.Translating,
            unsupportedSecondaryDownload.trackCard.translation,
        )

        val unrelatedDownloadWhileFailed = mapPhoneLyricsState(
            playback = playback,
            lyricsState = lyrics,
            plainLyricsAutoScrollEnabled = true,
            interactionMode = LyricsViewportInteractionMode.FOLLOW,
            currentMonotonicTimeMs = 1_000L,
            translationState = TranslationState.Failed(
                request = request,
                profile = LanguageProfile(
                    primary = "ja",
                    secondaryCandidate = null,
                    secondaryActivation = SecondaryActivation.NONE,
                    lines = emptyList(),
                ),
            ),
            translationSettings = enabledTranslation,
            translationModelStates = mapOf(
                "es" to TranslationModelState("es", TranslationModelPhase.WAITING_FOR_SYSTEM),
            ),
        )
        assertEquals(
            TrackCardTranslationUiState.Failed,
            unrelatedDownloadWhileFailed.trackCard.translation,
        )

        val currentRouteDownloadWhileFailed = mapPhoneLyricsState(
            playback = playback,
            lyricsState = lyrics,
            plainLyricsAutoScrollEnabled = true,
            interactionMode = LyricsViewportInteractionMode.FOLLOW,
            currentMonotonicTimeMs = 1_000L,
            translationState = TranslationState.Failed(
                request = request,
                profile = LanguageProfile(
                    primary = "ja",
                    secondaryCandidate = null,
                    secondaryActivation = SecondaryActivation.NONE,
                    lines = emptyList(),
                ),
            ),
            translationSettings = enabledTranslation,
            translationModelStates = mapOf(
                "ja" to TranslationModelState("ja", TranslationModelPhase.DOWNLOADING),
            ),
        )
        assertEquals(
            TrackCardTranslationUiState.Failed,
            currentRouteDownloadWhileFailed.trackCard.translation,
        )
    }

    @Test
    fun `no active track maps to stable empty Phone state`() {
        val state = mapPhoneLyricsState(
            playback = PlaybackSnapshot(),
            lyricsState = LyricsState.Idle,
            plainLyricsAutoScrollEnabled = true,
            interactionMode = LyricsViewportInteractionMode.FOLLOW,
            currentMonotonicTimeMs = 1_000L,
        )

        assertEquals("No active track", state.trackCard.title)
        assertTrue(state.viewport.lines.isEmpty())
        assertEquals(LyricsSyncType.PLAIN, state.viewport.syncType)
        assertNull(state.viewport.currentLineIndex)
    }

    @Test
    fun `matching loading lookup maps Track Card loading state`() {
        val track = track()
        val playback = PlaybackSnapshot(
            track = track,
            source = PlaybackSource("com.spotify.music"),
        )
        val lookup = LyricsLookup(
            id = LyricsLookupId(1L),
            track = track,
            playbackIdentity = requireNotNull(playback.trackIdentity),
        )

        val state = mapPhoneLyricsState(
            playback = playback,
            lyricsState = LyricsState.Loading(lookup),
            plainLyricsAutoScrollEnabled = true,
            interactionMode = LyricsViewportInteractionMode.FOLLOW,
            currentMonotonicTimeMs = 1_000L,
        )

        assertEquals(TrackCardLyricsStatus.LOADING, state.trackCard.lyricsStatus)
        assertNull(state.trackCard.providerLabel)
        assertNull(state.trackCard.syncLabel)
        assertTrue(state.viewport.lines.isEmpty())
    }

    @Test
    fun `matching not found lookup maps Track Card not found state`() {
        val track = track()
        val playback = PlaybackSnapshot(
            track = track,
            source = PlaybackSource("com.spotify.music"),
        )
        val lookup = LyricsLookup(
            id = LyricsLookupId(2L),
            track = track,
            playbackIdentity = requireNotNull(playback.trackIdentity),
        )

        val state = mapPhoneLyricsState(
            playback = playback,
            lyricsState = LyricsState.NotFound(lookup),
            plainLyricsAutoScrollEnabled = true,
            interactionMode = LyricsViewportInteractionMode.FOLLOW,
            currentMonotonicTimeMs = 1_000L,
        )

        assertEquals(TrackCardLyricsStatus.NOT_FOUND, state.trackCard.lyricsStatus)
        assertNull(state.trackCard.providerLabel)
        assertNull(state.trackCard.syncLabel)
        assertTrue(state.viewport.lines.isEmpty())
    }

    @Test
    fun `matching failed lookup maps Track Card failed state`() {
        val track = track()
        val playback = PlaybackSnapshot(
            track = track,
            source = PlaybackSource("com.spotify.music"),
        )
        val lookup = LyricsLookup(
            id = LyricsLookupId(3L),
            track = track,
            playbackIdentity = requireNotNull(playback.trackIdentity),
        )

        val state = mapPhoneLyricsState(
            playback = playback,
            lyricsState = LyricsState.Failed(
                lookup = lookup,
                failedAttempts = 1,
            ),
            plainLyricsAutoScrollEnabled = true,
            interactionMode = LyricsViewportInteractionMode.FOLLOW,
            currentMonotonicTimeMs = 1_000L,
        )

        assertEquals(TrackCardLyricsStatus.FAILED, state.trackCard.lyricsStatus)
        assertTrue(state.viewport.lines.isEmpty())
    }

    @Test
    fun `line lyrics project current line from monotonic playback position`() {
        val track = track()
        val playback = PlaybackSnapshot(
            track = track,
            status = PlaybackStatus.PLAYING,
            positionMs = 4_000L,
            playbackRate = 1f,
            source = PlaybackSource("com.spotify.music"),
            positionUpdatedAtMonotonicMs = 10_000L,
        )
        val state = mapPhoneLyricsState(
            playback = playback,
            lyricsState = ready(
                playback = playback,
                lines = listOf(
                    TimedLyricLine("First", 0L),
                    TimedLyricLine("Second", 5_000L),
                    TimedLyricLine("Third", 10_000L),
                ),
            ),
            plainLyricsAutoScrollEnabled = true,
            interactionMode = LyricsViewportInteractionMode.FOLLOW,
            currentMonotonicTimeMs = 12_000L,
        )

        assertEquals(1, state.viewport.currentLineIndex)
        assertEquals(LyricsSyncType.LINE, state.viewport.syncType)
        assertEquals("Musixmatch", state.trackCard.providerLabel)
        assertEquals("Line synced", state.trackCard.syncLabel)
        assertEquals(TrackCardLyricsStatus.READY, state.trackCard.lyricsStatus)
    }

    @Test
    fun `lyrics timing offset shifts line selection across canonical boundaries`() {
        val track = track()
        val lyricsLines = listOf(
            TimedLyricLine("First", 0L),
            TimedLyricLine("Second", 5_000L),
            TimedLyricLine("Third", 10_000L),
        )

        val advancedPlayback = PlaybackSnapshot(
            track = track,
            positionMs = 4_500L,
            source = PlaybackSource("com.spotify.music"),
        )
        val advanced = mapPhoneLyricsState(
            playback = advancedPlayback,
            lyricsState = ready(advancedPlayback, lyricsLines),
            plainLyricsAutoScrollEnabled = true,
            interactionMode = LyricsViewportInteractionMode.FOLLOW,
            currentMonotonicTimeMs = 1_000L,
            lyricsTimingOffset = LyricsTimingOffset(750L),
        )

        val delayedPlayback = advancedPlayback.copy(positionMs = 5_500L)
        val delayed = mapPhoneLyricsState(
            playback = delayedPlayback,
            lyricsState = ready(delayedPlayback, lyricsLines),
            plainLyricsAutoScrollEnabled = true,
            interactionMode = LyricsViewportInteractionMode.FOLLOW,
            currentMonotonicTimeMs = 1_000L,
            lyricsTimingOffset = LyricsTimingOffset(-750L),
        )

        assertEquals(1, advanced.viewport.currentLineIndex)
        assertEquals(0, delayed.viewport.currentLineIndex)
    }

    @Test
    fun `word source stays line-oriented while Karaoke is unavailable`() {
        val track = track()
        val playback = PlaybackSnapshot(
            track = track,
            source = PlaybackSource("com.spotify.music"),
        )
        val state = mapPhoneLyricsState(
            playback = playback,
            lyricsState = ready(
                playback = playback,
                lines = listOf(
                    TimedLyricLine(
                        text = "Hello world",
                        startMs = 0L,
                        words = listOf(
                            TimedWord("Hello", 0L, 300L),
                            TimedWord(" world", 300L, 700L),
                        ),
                    ),
                ),
            ),
            plainLyricsAutoScrollEnabled = true,
            interactionMode = LyricsViewportInteractionMode.FOLLOW,
            currentMonotonicTimeMs = 1_000L,
        )

        assertEquals("Word synced", state.trackCard.syncLabel)
        assertEquals(LyricsSyncType.LINE, state.viewport.syncType)
        assertTrue(state.viewport.lines.single().words.isEmpty())
        assertNull(state.viewport.currentWordIndex)
    }

    @Test
    fun `lyrics from another canonical playback identity are not exposed`() {
        val currentTrack = track()
        val currentPlayback = PlaybackSnapshot(
            track = currentTrack,
            source = PlaybackSource("com.spotify.music"),
        )
        val staleTrack = currentTrack.copy(
            references = setOf(TrackReference("spotify", "previous-id")),
        )
        val stalePlayback = currentPlayback.copy(track = staleTrack)

        val state = mapPhoneLyricsState(
            playback = currentPlayback,
            lyricsState = ready(
                playback = stalePlayback,
                lines = listOf(TimedLyricLine("Stale", 0L)),
            ),
            plainLyricsAutoScrollEnabled = true,
            interactionMode = LyricsViewportInteractionMode.FOLLOW,
            currentMonotonicTimeMs = 1_000L,
        )

        assertTrue(state.viewport.lines.isEmpty())
        assertNull(state.trackCard.providerLabel)
    }

    private fun track() = Track(
        title = "Midnight Signals",
        artists = listOf("The Northbound Lights"),
        durationMs = 20_000L,
        references = setOf(TrackReference("spotify", "track-id")),
    )

    private fun translated(
        lyrics: LyricsState.Ready,
        lines: List<Pair<String, Boolean>>,
    ): TranslationState = TranslationState.Ready(
        TranslationArtifact(
            request = TranslationRequestIdentity(
                id = TranslationRequestId(1L),
                canonicalLyrics = requireNotNull(lyrics.canonicalLyricsOrNull()).identity,
                targetLanguage = "en",
            ),
            providerId = TranslationProviderId("mlkit"),
            profile = LanguageProfile("ja", null, SecondaryActivation.NONE, emptyList()),
            lines = lines.mapIndexed { index, (text, isTranslated) ->
                TranslationArtifactLine(index, text, "ja", isTranslated)
            },
        ),
    )

    private fun ready(
        playback: PlaybackSnapshot,
        lines: List<TimedLyricLine>,
    ) = LyricsState.Ready(
        lookup = LyricsLookup(
            id = LyricsLookupId(1L),
            track = requireNotNull(playback.track),
            playbackIdentity = requireNotNull(playback.trackIdentity),
        ),
        lyrics = LyricsDocument(
            lines = lines,
            languageTag = "en",
            attribution = LyricsAttribution(
                providerId = "musixmatch",
                displayName = "Musixmatch",
            ),
        ),
    )
}
