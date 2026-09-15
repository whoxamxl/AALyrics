package io.github.whoxamxl.aalyrics.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DomainModelTest {

    @Test
    fun `lyrics sync type is derived from strongest available timing`() {
        val plain = LyricsDocument(
            lines = listOf(PlainLyricLine("plain")),
        )
        val lineSynced = LyricsDocument(
            lines = listOf(TimedLyricLine(text = "line", startMs = 1_000L)),
        )
        val wordSynced = LyricsDocument(
            lines = listOf(
                TimedLyricLine(
                    text = "word sync",
                    startMs = 2_000L,
                    words = listOf(
                        TimedWord("word", startMs = 2_000L, endMs = 2_300L),
                        TimedWord("sync", startMs = 2_300L, endMs = 2_700L),
                    ),
                ),
            ),
        )

        assertEquals(LyricsSyncType.PLAIN, plain.syncType)
        assertEquals(LyricsSyncType.LINE, lineSynced.syncType)
        assertEquals(LyricsSyncType.WORD, wordSynced.syncType)
    }

    @Test
    fun `timed lines reject reversed word order`() {
        assertFailsWith<IllegalArgumentException> {
            TimedLyricLine(
                text = "invalid",
                startMs = 1_000L,
                words = listOf(
                    TimedWord("later", startMs = 1_500L),
                    TimedWord("earlier", startMs = 1_200L),
                ),
            )
        }
    }

    @Test
    fun `track uses null for unknown duration and rejects negative duration`() {
        val unknownDuration = Track(title = "Track")
        assertEquals(null, unknownDuration.durationMs)

        assertFailsWith<IllegalArgumentException> {
            Track(title = "Track", durationMs = -1L)
        }
    }

    @Test
    fun `playback snapshot exposes domain playing state`() {
        val playing = PlaybackSnapshot(
            track = Track(title = "Track", artists = listOf("Artist")),
            status = PlaybackStatus.PLAYING,
            positionMs = 1_234L,
        )
        val paused = playing.copy(status = PlaybackStatus.PAUSED)

        assertTrue(playing.isPlaying)
        assertFalse(paused.isPlaying)
    }

    @Test
    fun `playback snapshot rejects invalid timeline values`() {
        assertFailsWith<IllegalArgumentException> {
            PlaybackSnapshot(positionMs = -1L)
        }
        assertFailsWith<IllegalArgumentException> {
            PlaybackSnapshot(playbackRate = Float.NaN)
        }
    }
}
