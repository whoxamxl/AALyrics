package io.github.whoxamxl.aalyrics.ui.automotive.state

import io.github.whoxamxl.aalyrics.core.lyrics.LyricsLookup
import io.github.whoxamxl.aalyrics.core.lyrics.LyricsLookupId
import io.github.whoxamxl.aalyrics.core.lyrics.LyricsState
import io.github.whoxamxl.aalyrics.core.model.LyricsDocument
import io.github.whoxamxl.aalyrics.core.model.PlainLyricLine
import io.github.whoxamxl.aalyrics.core.model.PlaybackSnapshot
import io.github.whoxamxl.aalyrics.core.model.PlaybackStatus
import io.github.whoxamxl.aalyrics.core.model.TimedLyricLine
import io.github.whoxamxl.aalyrics.core.model.Track
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

        assertEquals("Unsynced lyrics", state.subtitle)
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
        assertEquals("Loading lyrics…", state.subtitle)
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
        assertEquals("Loading lyrics…", state.subtitle)
    }

    private fun ready(track: Track, lyrics: LyricsDocument): LyricsState.Ready =
        LyricsState.Ready(
            lookup = LyricsLookup(LyricsLookupId(1L), track),
            lyrics = lyrics,
        )
}
