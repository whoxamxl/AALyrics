package io.github.whoxamxl.aalyrics.ui.automotive.state

import io.github.whoxamxl.aalyrics.core.lyrics.LyricsLookup
import io.github.whoxamxl.aalyrics.core.lyrics.LyricsLookupId
import io.github.whoxamxl.aalyrics.core.lyrics.LyricsState
import io.github.whoxamxl.aalyrics.core.model.LyricsDocument
import io.github.whoxamxl.aalyrics.core.model.PlainLyricLine
import io.github.whoxamxl.aalyrics.core.model.PlaybackSnapshot
import io.github.whoxamxl.aalyrics.core.model.PlaybackStatus
import io.github.whoxamxl.aalyrics.core.model.PlaybackTrackIdentity
import io.github.whoxamxl.aalyrics.core.model.TimedLyricLine
import io.github.whoxamxl.aalyrics.core.model.TimedWord
import io.github.whoxamxl.aalyrics.core.model.Track
import io.github.whoxamxl.aalyrics.core.timing.LyricsTimingOffset
import io.github.whoxamxl.aalyrics.translation.api.TranslationProviderId
import io.github.whoxamxl.aalyrics.translation.api.TranslationSettings
import io.github.whoxamxl.aalyrics.translation.core.CanonicalLyrics
import io.github.whoxamxl.aalyrics.translation.core.LanguageProfile
import io.github.whoxamxl.aalyrics.translation.core.TranslationArtifact
import io.github.whoxamxl.aalyrics.translation.core.TranslationArtifactLine
import io.github.whoxamxl.aalyrics.translation.core.TranslationRequestId
import io.github.whoxamxl.aalyrics.translation.core.TranslationRequestIdentity
import io.github.whoxamxl.aalyrics.translation.core.TranslationState
import io.github.whoxamxl.aalyrics.translation.core.SecondaryActivation
import kotlin.test.Test
import kotlin.test.assertEquals

class AutomotiveLyricsUiStateMapperTest {
    @Test
    fun `playing snapshot advances current line using elapsed playback time`() {
        val track = Track(
            title = "Song",
            artists = listOf("Artist"),
            durationMs = 180_000L,
        )
        val playback = PlaybackSnapshot(
            track = track,
            status = PlaybackStatus.PLAYING,
            positionMs = 9_500L,
            playbackRate = 1f,
            positionUpdatedAtMonotonicMs = 1_000L,
        )
        val lyrics = LyricsDocument(
            lines = listOf(
                TimedLyricLine("First", startMs = 0L),
                TimedLyricLine("Second", startMs = 10_000L),
                TimedLyricLine("Third", startMs = 20_000L),
            ),
        )

        val state = AutomotiveLyricsUiStateMapper.project(
            playback = playback,
            lyricsState = ready(track, lyrics),
            currentMonotonicTimeMs = 1_750L,
        )

        assertEquals(10_250L, state.positionMs)
        assertEquals("Song — Artist", state.displayTitle)
        assertEquals("Second", state.subtitle)
    }

    @Test
    fun `supplied 75ms audio latency compensation shifts lyrics only and not playback position`() {
        val track = Track(title = "Song", artists = listOf("Artist"))
        val lyrics = ready(
            track,
            LyricsDocument(
                lines = listOf(
                    TimedLyricLine("First", startMs = 0L),
                    TimedLyricLine("Second", startMs = 10_000L),
                ),
            ),
        )

        val beforeBoundary = AutomotiveLyricsUiStateMapper.project(
            playback = PlaybackSnapshot(track = track, positionMs = 10_050L),
            lyricsState = lyrics,
            currentMonotonicTimeMs = 0L,
            lyricsTimingOffset = LyricsTimingOffset(-75L),
        )
        val atCompensatedBoundary = AutomotiveLyricsUiStateMapper.project(
            playback = PlaybackSnapshot(track = track, positionMs = 10_075L),
            lyricsState = lyrics,
            currentMonotonicTimeMs = 0L,
            lyricsTimingOffset = LyricsTimingOffset(-75L),
        )

        assertEquals(10_050L, beforeBoundary.positionMs)
        assertEquals("First", beforeBoundary.subtitle)
        assertEquals(10_075L, atCompensatedBoundary.positionMs)
        assertEquals("Second", atCompensatedBoundary.subtitle)
    }

    @Test
    fun `sample timestamp drives both sides of a line boundary when source time is absent`() {
        val track = Track(title = "Song", artists = listOf("Artist"))
        val playback = PlaybackSnapshot(
            track = track,
            status = PlaybackStatus.PLAYING,
            positionMs = 9_500L,
            positionSampledAtMonotonicMs = 1_000L,
        )
        val lyrics = ready(
            track,
            LyricsDocument(
                lines = listOf(
                    TimedLyricLine("First", startMs = 0L),
                    TimedLyricLine("Second", startMs = 10_000L),
                ),
            ),
        )

        assertEquals("First", AutomotiveLyricsUiStateMapper.project(
            playback, lyrics, currentMonotonicTimeMs = 1_400L,
        ).subtitle)
        assertEquals("Second", AutomotiveLyricsUiStateMapper.project(
            playback, lyrics, currentMonotonicTimeMs = 1_600L,
        ).subtitle)
        assertEquals("First", AutomotiveLyricsUiStateMapper.project(
            playback.copy(positionMs = 8_000L), lyrics, currentMonotonicTimeMs = 1_600L,
        ).subtitle)
    }

    @Test
    fun `plain lyrics do not synthesize a timed Now Playing line`() {
        val track = Track(title = "Plain song", artists = listOf("Artist"))
        val lyrics = LyricsDocument(
            lines = listOf(PlainLyricLine("One"), PlainLyricLine("Two")),
        )

        val state = AutomotiveLyricsUiStateMapper.project(
            playback = PlaybackSnapshot(track = track, status = PlaybackStatus.PLAYING),
            lyricsState = ready(track, lyrics),
            currentMonotonicTimeMs = 5_000L,
        )

        assertEquals("Synced lyrics unavailable", state.subtitle)
    }

    @Test
    fun `stale lyrics from previous track never reach Now Playing subtitle`() {
        val oldTrack = Track(title = "Old", artists = listOf("Artist"))
        val newTrack = Track(title = "New", artists = listOf("Artist"))
        val oldLyrics = LyricsDocument(
            lines = listOf(TimedLyricLine("Old lyric", startMs = 0L)),
        )

        val state = AutomotiveLyricsUiStateMapper.project(
            playback = PlaybackSnapshot(
                track = newTrack,
                status = PlaybackStatus.PLAYING,
                positionMs = 30_000L,
            ),
            lyricsState = ready(oldTrack, oldLyrics),
            currentMonotonicTimeMs = 0L,
        )

        assertEquals("New — Artist", state.displayTitle)
        assertEquals("Loading lyrics.", state.subtitle)
    }

    @Test
    fun `paused playback does not advance projected position`() {
        val track = Track(title = "Paused", artists = listOf("Artist"))
        val state = AutomotiveLyricsUiStateMapper.project(
            playback = PlaybackSnapshot(
                track = track,
                status = PlaybackStatus.PAUSED,
                positionMs = 12_345L,
            ),
            lyricsState = LyricsState.Loading(
                LyricsLookup(LyricsLookupId(1L), track),
            ),
            currentMonotonicTimeMs = 10_000L,
        )

        assertEquals(12_345L, state.positionMs)
        assertEquals("Loading lyrics..", state.subtitle)
        assertEquals(true, state.lyrics.isAnimatedLoading)
    }

    @Test
    fun `no media and waiting have distinct stable copy`() {
        val track = Track(title = "Song", artists = listOf("Artist"))
        assertEquals(
            AutomotiveLyricsUiState.NO_MEDIA_MESSAGE,
            AutomotiveLyricsUiStateMapper.project(
                PlaybackSnapshot(), LyricsState.Idle, currentMonotonicTimeMs = 0L,
            ).subtitle,
        )
        assertEquals(
            "Waiting for lyrics…",
            AutomotiveLyricsUiStateMapper.project(
                PlaybackSnapshot(track = track), LyricsState.Idle, currentMonotonicTimeMs = 0L,
            ).subtitle,
        )
    }

    @Test
    fun `lyrics loading cycles all three frames while paused`() {
        val track = Track(title = "Song", artists = listOf("Artist"))
        val paused = PlaybackSnapshot(track = track, status = PlaybackStatus.PAUSED)
        val loading = LyricsState.Loading(LyricsLookup(LyricsLookupId(1L), track))

        assertEquals("Loading lyrics.", AutomotiveLyricsUiStateMapper.project(
            paused, loading, currentMonotonicTimeMs = 0L,
        ).subtitle)
        assertEquals("Loading lyrics..", AutomotiveLyricsUiStateMapper.project(
            paused, loading, currentMonotonicTimeMs = 250L,
        ).subtitle)
        assertEquals("Loading lyrics...", AutomotiveLyricsUiStateMapper.project(
            paused, loading, currentMonotonicTimeMs = 500L,
        ).subtitle)
        assertEquals("Loading lyrics.", AutomotiveLyricsUiStateMapper.project(
            paused, loading, currentMonotonicTimeMs = 750L,
        ).subtitle)
        assertEquals(true, shouldRenderProjectionTick(paused, isAnimatedLoading = true))
        assertEquals(false, shouldRenderProjectionTick(paused, isAnimatedLoading = false))
    }

    @Test
    fun `not found and failed have distinct copy`() {
        val track = Track(title = "Song", artists = listOf("Artist"))
        val playback = PlaybackSnapshot(track = track)
        val lookup = LyricsLookup(LyricsLookupId(1L), track)

        assertEquals("No synced lyrics found", AutomotiveLyricsUiStateMapper.project(
            playback, LyricsState.NotFound(lookup), currentMonotonicTimeMs = 0L,
        ).subtitle)
        assertEquals("Unable to load lyrics", AutomotiveLyricsUiStateMapper.project(
            playback, LyricsState.Failed(lookup, failedAttempts = 1), currentMonotonicTimeMs = 0L,
        ).subtitle)
    }

    @Test
    fun `line and word documents show current line only and interlude note`() {
        val track = Track(title = "Song", artists = listOf("Artist"))
        val lineDocument = LyricsDocument(lines = listOf(
            TimedLyricLine("First", startMs = 1_000L),
            TimedLyricLine("", startMs = 2_000L),
            TimedLyricLine("Third", startMs = 3_000L),
        ))
        val wordDocument = LyricsDocument(lines = listOf(
            TimedLyricLine(
                "Current words", startMs = 1_000L,
                words = listOf(TimedWord("Current", startMs = 1_000L)),
            ),
        ))
        fun textAt(document: LyricsDocument, positionMs: Long): String =
            AutomotiveLyricsUiStateMapper.project(
                PlaybackSnapshot(track = track, positionMs = positionMs),
                ready(track, document),
                currentMonotonicTimeMs = 0L,
            ).subtitle

        assertEquals("♪", textAt(lineDocument, 500L))
        assertEquals("First", textAt(lineDocument, 1_500L))
        assertEquals("♪", textAt(lineDocument, 2_500L))
        assertEquals("Third", textAt(lineDocument, 3_500L))
        assertEquals("Current words", textAt(wordDocument, 1_500L))
    }

    @Test
    fun `artwork arrival null and track transition preserve current identity only`() {
        val first = PlaybackTrackIdentity.Metadata(null, "First", listOf("Artist"), null)
        val second = PlaybackTrackIdentity.Metadata(null, "Second", listOf("Artist"), null)

        assertEquals(null, artworkForTrack(first, first, null as String?))
        assertEquals("jacket", artworkForTrack(first, first, "jacket"))
        assertEquals(null, artworkForTrack(first, first, null as String?))
        assertEquals(null, artworkForTrack(second, first, "jacket"))
        assertEquals("new jacket", artworkForTrack(second, second, "new jacket"))
    }

    @Test
    fun `translating heartbeat keeps source first while paused`() {
        val track = Track(title = "Song", artists = listOf("Artist"))
        val document = LyricsDocument(lines = listOf(TimedLyricLine("Source", startMs = 0L)))
        val identity = CanonicalLyrics.create("lyrics-lookup-1", document).identity
        val request = TranslationRequestIdentity(TranslationRequestId(1L), identity, "en")
        val playback = PlaybackSnapshot(track = track, status = PlaybackStatus.PAUSED)

        listOf("Translating.", "Translating..", "Translating...")
            .forEachIndexed { frame, expected ->
                val state = AutomotiveLyricsUiStateMapper.project(
                    playback = playback,
                    lyricsState = ready(track, document),
                    currentMonotonicTimeMs = frame * 250L,
                    translationSettings = TranslationSettings(enabled = true),
                    translationState = TranslationState.Translating(request),
                    canonicalLyricsIdentity = identity,
                )
                assertEquals("Source", state.lyrics.primaryText)
                assertEquals(expected, state.lyrics.secondaryText)
                assertEquals(true, state.lyrics.isAnimatedLoading)
                assertEquals(true, shouldRenderProjectionTick(playback, state.lyrics.isAnimatedLoading))
            }
        val stale = AutomotiveLyricsUiStateMapper.project(
            playback = playback,
            lyricsState = ready(track, document),
            currentMonotonicTimeMs = 0L,
            translationSettings = TranslationSettings(enabled = true),
            translationState = TranslationState.Translating(
                request.copy(canonicalLyrics = identity.copy(ownerId = "old")),
            ),
            canonicalLyricsIdentity = identity,
        )
        assertEquals(null, stale.lyrics.secondaryText)
        assertEquals(false, stale.lyrics.isAnimatedLoading)
    }

    @Test
    fun `ready translation requires exact identity target and translated nonblank line`() {
        val track = Track(title = "Song", artists = listOf("Artist"))
        val document = LyricsDocument(lines = listOf(TimedLyricLine("Source", startMs = 0L)))
        val identity = CanonicalLyrics.create("lyrics-lookup-1", document).identity
        val request = TranslationRequestIdentity(TranslationRequestId(1L), identity, "en")
        val line = TranslationArtifactLine(0, "Translated", "ja", translated = true)
        fun readyTranslation(
            requestIdentity: TranslationRequestIdentity = request,
            artifactLine: TranslationArtifactLine = line,
        ) = TranslationState.Ready(TranslationArtifact(
            request = requestIdentity,
            providerId = TranslationProviderId("test"),
            profile = LanguageProfile(null, null, SecondaryActivation.NONE, emptyList()),
            lines = listOf(artifactLine),
        ))
        fun secondary(
            settings: TranslationSettings = TranslationSettings(enabled = true),
            state: TranslationState = readyTranslation(),
        ) = AutomotiveLyricsUiStateMapper.project(
            playback = PlaybackSnapshot(track = track),
            lyricsState = ready(track, document),
            currentMonotonicTimeMs = 0L,
            translationSettings = settings,
            translationState = state,
            canonicalLyricsIdentity = identity,
        ).lyrics.secondaryText

        assertEquals("Translated", secondary())
        assertEquals(null, secondary(settings = TranslationSettings(enabled = false)))
        assertEquals(null, secondary(state = readyTranslation(
            requestIdentity = request.copy(canonicalLyrics = identity.copy(ownerId = "old")),
        )))
        assertEquals(null, secondary(state = readyTranslation(
            requestIdentity = request.copy(targetLanguage = "ja"),
        )))
        assertEquals(null, secondary(state = readyTranslation(
            artifactLine = line.copy(translated = false),
        )))
        assertEquals(null, secondary(state = readyTranslation(
            artifactLine = line.copy(text = " "),
        )))
        assertEquals(null, secondary(state = TranslationState.Failed(request)))
        assertEquals(null, secondary(state = TranslationState.NotRequired(
            request,
            LanguageProfile(null, null, SecondaryActivation.NONE, emptyList()),
        )))
    }

    @Test
    fun `lyrics lifecycle and plain content take precedence over translation`() {
        val track = Track(title = "Song", artists = listOf("Artist"))
        val lookup = LyricsLookup(LyricsLookupId(1L), track)
        val document = LyricsDocument(lines = listOf(TimedLyricLine("Source", startMs = 0L)))
        val identity = CanonicalLyrics.create("lyrics-lookup-1", document).identity
        val translating = TranslationState.Translating(
            TranslationRequestIdentity(TranslationRequestId(1L), identity, "en"),
        )
        fun presentation(lyrics: LyricsState) = AutomotiveLyricsUiStateMapper.project(
            playback = PlaybackSnapshot(track = track),
            lyricsState = lyrics,
            currentMonotonicTimeMs = 0L,
            translationSettings = TranslationSettings(enabled = true),
            translationState = translating,
            canonicalLyricsIdentity = identity,
        ).lyrics

        assertEquals(null, presentation(LyricsState.Loading(lookup)).secondaryText)
        assertEquals(null, presentation(LyricsState.Failed(lookup, failedAttempts = 1)).secondaryText)
        assertEquals(null, presentation(LyricsState.NotFound(lookup)).secondaryText)
        assertEquals(null, presentation(ready(track, LyricsDocument(
            lines = listOf(PlainLyricLine("Plain")),
        ))).secondaryText)
    }

    private fun ready(track: Track, lyrics: LyricsDocument): LyricsState.Ready =
        LyricsState.Ready(
            lookup = LyricsLookup(LyricsLookupId(1L), track),
            lyrics = lyrics,
        )
}
