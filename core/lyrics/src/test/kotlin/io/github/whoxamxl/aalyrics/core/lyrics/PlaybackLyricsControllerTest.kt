package io.github.whoxamxl.aalyrics.core.lyrics

import io.github.whoxamxl.aalyrics.core.model.PlaybackSnapshot
import io.github.whoxamxl.aalyrics.core.model.PlaybackSource
import io.github.whoxamxl.aalyrics.core.model.PlaybackStatus
import io.github.whoxamxl.aalyrics.core.model.Track
import io.github.whoxamxl.aalyrics.core.model.TrackReference
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PlaybackLyricsControllerTest {
    @Test
    fun `first playable snapshot starts one lookup`() {
        val lifecycle = RecordingLifecycle()
        val controller = PlaybackLyricsController(lifecycle)
        val track = Track(title = "Song", artists = listOf("Artist"))

        controller.onPlayback(PlaybackSnapshot(track = track))

        assertEquals(listOf(track), lifecycle.startedTracks)
        assertEquals(0, lifecycle.clearCount)
    }

    @Test
    fun `timeline and duration updates do not restart current lookup`() {
        val lifecycle = RecordingLifecycle()
        val controller = PlaybackLyricsController(lifecycle)
        val track = Track(title = "Song", artists = listOf("Artist"))
        val first = PlaybackSnapshot(
            track = track,
            status = PlaybackStatus.PAUSED,
            source = PlaybackSource(id = "com.example.player", mediaId = "item-1"),
        )

        controller.onPlayback(first)
        controller.onPlayback(
            first.copy(
                track = track.copy(durationMs = 200_000L),
                status = PlaybackStatus.PLAYING,
                positionMs = 42_000L,
                playbackRate = 1.0f,
            ),
        )

        assertEquals(listOf(track), lifecycle.startedTracks)
    }

    @Test
    fun `stable reference prevents corrected metadata from restarting lookup`() {
        val lifecycle = RecordingLifecycle()
        val controller = PlaybackLyricsController(lifecycle)
        val reference = TrackReference("spotify", "6rqhFgbbKwnb9MLmUQDhG6")
        val firstTrack = Track(
            title = "Initial title",
            artists = listOf("Artist"),
            references = setOf(reference),
        )
        val correctedTrack = firstTrack.copy(title = "Corrected title")
        val source = PlaybackSource(id = "com.spotify.music")

        controller.onPlayback(PlaybackSnapshot(track = firstTrack, source = source))
        controller.onPlayback(PlaybackSnapshot(track = correctedTrack, source = source))

        assertEquals(listOf(firstTrack), lifecycle.startedTracks)
    }

    @Test
    fun `new playback identity supersedes with a fresh lookup`() {
        val lifecycle = RecordingLifecycle()
        val controller = PlaybackLyricsController(lifecycle)
        val firstTrack = Track(title = "First", artists = listOf("Artist"))
        val secondTrack = Track(title = "Second", artists = listOf("Artist"))

        controller.onPlayback(
            PlaybackSnapshot(
                track = firstTrack,
                source = PlaybackSource(id = "com.example.player", mediaId = "item-1"),
            ),
        )
        controller.onPlayback(
            PlaybackSnapshot(
                track = secondTrack,
                source = PlaybackSource(id = "com.example.player", mediaId = "item-2"),
            ),
        )

        assertEquals(listOf(firstTrack, secondTrack), lifecycle.startedTracks)
        assertEquals(2, lifecycle.lookups.size)
        assertTrue(lifecycle.lookups[0].id != lifecycle.lookups[1].id)
    }

    @Test
    fun `losing the current track clears lookup ownership once`() {
        val lifecycle = RecordingLifecycle()
        val controller = PlaybackLyricsController(lifecycle)
        val track = Track(title = "Song", artists = listOf("Artist"))

        controller.onPlayback(PlaybackSnapshot(track = track))
        controller.onPlayback(PlaybackSnapshot())
        controller.onPlayback(PlaybackSnapshot())

        assertEquals(1, lifecycle.clearCount)
    }

    private class RecordingLifecycle : LyricsLookupLifecycle {
        val startedTracks = mutableListOf<Track>()
        val lookups = mutableListOf<LyricsLookup>()
        var clearCount = 0
        private var nextId = 0L

        override fun startLookup(
            track: Track,
            preferences: CandidateSelectionPreferences,
        ): LyricsLookup {
            startedTracks += track
            return LyricsLookup(
                id = LyricsLookupId(nextId++),
                track = track,
            ).also(lookups::add)
        }

        override fun clear() {
            clearCount += 1
        }
    }
}
