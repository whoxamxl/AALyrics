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
import io.github.whoxamxl.aalyrics.ui.phone.lyrics.LyricsViewportInteractionMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PhoneLyricsMapperTest {
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
