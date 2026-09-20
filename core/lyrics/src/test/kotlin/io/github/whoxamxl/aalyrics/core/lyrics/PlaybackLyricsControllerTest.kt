package io.github.whoxamxl.aalyrics.core.lyrics

import io.github.whoxamxl.aalyrics.core.model.LyricsSyncType
import io.github.whoxamxl.aalyrics.core.model.PlaybackSnapshot
import io.github.whoxamxl.aalyrics.core.model.PlaybackTrackIdentity
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
        assertEquals(
            listOf<PlaybackTrackIdentity>(
                PlaybackTrackIdentity.Metadata(
                    sourceId = null,
                    title = track.title,
                    artists = track.artists,
                    album = track.album,
                ),
            ),
            lifecycle.startedIdentities,
        )
        assertEquals(listOf(CandidateSelectionPreferences()), lifecycle.startedPreferences)
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
        assertEquals(
            listOf<PlaybackTrackIdentity>(
                PlaybackTrackIdentity.SourceMedia(
                    sourceId = "com.example.player",
                    mediaId = "item-1",
                ),
            ),
            lifecycle.startedIdentities,
        )
    }

    @Test
    fun `same track and same preferences do not restart current lookup`() {
        val lifecycle = RecordingLifecycle()
        val controller = PlaybackLyricsController(lifecycle)
        val track = Track(title = "Song", artists = listOf("Artist"))
        val preferences = CandidateSelectionPreferences(preferredSyncType = LyricsSyncType.WORD)

        controller.onPlayback(PlaybackSnapshot(track = track), preferences)
        controller.onPlayback(PlaybackSnapshot(track = track), preferences.copy())

        assertEquals(listOf(track), lifecycle.startedTracks)
        assertEquals(listOf(preferences), lifecycle.startedPreferences)
    }

    @Test
    fun `same track and changed preferences start a fresh lookup`() {
        val lifecycle = RecordingLifecycle()
        val controller = PlaybackLyricsController(lifecycle)
        val track = Track(title = "Song", artists = listOf("Artist"))
        val standard = CandidateSelectionPreferences()
        val karaoke = CandidateSelectionPreferences(preferredSyncType = LyricsSyncType.WORD)

        controller.onPlayback(PlaybackSnapshot(track = track), standard)
        controller.onPlayback(PlaybackSnapshot(track = track), karaoke)

        assertEquals(listOf(track, track), lifecycle.startedTracks)
        assertEquals(listOf(standard, karaoke), lifecycle.startedPreferences)
        assertTrue(lifecycle.lookups[0].id != lifecycle.lookups[1].id)
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
        assertEquals(
            listOf<PlaybackTrackIdentity>(
                PlaybackTrackIdentity.Referenced(
                    sourceId = "com.spotify.music",
                    references = setOf(reference),
                ),
            ),
            lifecycle.startedIdentities,
        )
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
        val startedPreferences = mutableListOf<CandidateSelectionPreferences>()
        val startedIdentities = mutableListOf<PlaybackTrackIdentity>()
        val lookups = mutableListOf<LyricsLookup>()
        var clearCount = 0
        private var nextId = 0L

        override fun startLookup(
            track: Track,
            preferences: CandidateSelectionPreferences,
            playbackIdentity: PlaybackTrackIdentity?,
        ): LyricsLookup {
            startedTracks += track
            startedPreferences += preferences
            val identity = requireNotNull(playbackIdentity)
            startedIdentities += identity
            return LyricsLookup(
                id = LyricsLookupId(nextId++),
                track = track,
                playbackIdentity = identity,
            ).also(lookups::add)
        }

        override fun clear() {
            clearCount += 1
        }
    }
}
