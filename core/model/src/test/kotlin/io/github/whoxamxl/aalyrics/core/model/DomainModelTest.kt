package io.github.whoxamxl.aalyrics.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
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
    fun `playback track identity prefers stable references over mutable metadata`() {
        val spotifyReference = TrackReference("spotify", "6rqhFgbbKwnb9MLmUQDhG6")
        val initial = PlaybackSnapshot(
            track = Track(
                title = "Initial title",
                artists = listOf("Artist"),
                durationMs = 100_000L,
                references = setOf(spotifyReference),
            ),
            source = PlaybackSource(
                id = "com.spotify.music",
                mediaId = "opaque-session-id",
                mediaUri = "spotify:track:6rqhFgbbKwnb9MLmUQDhG6",
            ),
        )
        val updated = initial.copy(
            track = initial.track!!.copy(
                title = "Corrected title",
                durationMs = 101_000L,
            ),
            status = PlaybackStatus.PLAYING,
            positionMs = 40_000L,
        )

        assertEquals(initial.trackIdentity, updated.trackIdentity)
        assertEquals(
            PlaybackTrackIdentity.Referenced(
                sourceId = "com.spotify.music",
                references = setOf(spotifyReference),
            ),
            initial.trackIdentity,
        )
    }

    @Test
    fun `source media identity ignores timeline and duration changes`() {
        val initial = PlaybackSnapshot(
            track = Track(
                title = "Track",
                artists = listOf("Artist"),
                durationMs = null,
            ),
            source = PlaybackSource(
                id = "com.example.player",
                mediaId = "queue-item-42",
            ),
        )
        val updated = initial.copy(
            track = initial.track!!.copy(durationMs = 200_000L),
            status = PlaybackStatus.PAUSED,
            positionMs = 15_000L,
        )

        assertEquals(initial.trackIdentity, updated.trackIdentity)
    }

    @Test
    fun `metadata fallback changes only when identifying metadata changes`() {
        val initial = PlaybackSnapshot(
            track = Track(
                title = "Track A",
                artists = listOf("Artist"),
                album = "Album",
            ),
        )
        val timelineUpdate = initial.copy(
            track = initial.track!!.copy(durationMs = 180_000L),
            status = PlaybackStatus.PLAYING,
            positionMs = 50_000L,
        )
        val nextTrack = initial.copy(
            track = initial.track!!.copy(title = "Track B"),
        )

        assertEquals(initial.trackIdentity, timelineUpdate.trackIdentity)
        assertNotEquals(initial.trackIdentity, nextTrack.trackIdentity)
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
