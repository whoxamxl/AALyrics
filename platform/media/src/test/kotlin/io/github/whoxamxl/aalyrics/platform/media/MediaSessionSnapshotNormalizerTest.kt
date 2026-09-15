package io.github.whoxamxl.aalyrics.platform.media

import io.github.whoxamxl.aalyrics.core.model.PlaybackStatus
import io.github.whoxamxl.aalyrics.core.model.TrackReference
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MediaSessionSnapshotNormalizerTest {
    private val spotifyId = "6rqhFgbbKwnb9MLmUQDhG6"

    @Test
    fun `spotify track uri becomes a stable track reference`() {
        val snapshot = MediaSessionSnapshotNormalizer.normalize(
            MediaSessionSnapshotInput(
                sourceId = SpotifyPlaybackReference.SOURCE_ID,
                title = "  Example Song  ",
                artist = "  Example Artist  ",
                album = "  Example Album  ",
                durationMs = 200_000L,
                mediaUri = "spotify:track:$spotifyId",
                status = PlaybackStatus.PLAYING,
                positionMs = 12_000L,
            ),
        )

        assertEquals("Example Song", snapshot.track?.title)
        assertEquals(listOf("Example Artist"), snapshot.track?.artists)
        assertEquals("Example Album", snapshot.track?.album)
        assertEquals(200_000L, snapshot.track?.durationMs)
        assertEquals(
            setOf(TrackReference("spotify", spotifyId)),
            snapshot.track?.references,
        )
        assertEquals(PlaybackStatus.PLAYING, snapshot.status)
        assertEquals(12_000L, snapshot.positionMs)
    }

    @Test
    fun `spotify track URL in media id is accepted as explicit resource`() {
        val snapshot = MediaSessionSnapshotNormalizer.normalize(
            MediaSessionSnapshotInput(
                sourceId = SpotifyPlaybackReference.SOURCE_ID,
                title = "Example Song",
                mediaId = "https://open.spotify.com/track/$spotifyId?si=test",
            ),
        )

        assertEquals(
            setOf(TrackReference("spotify", spotifyId)),
            snapshot.track?.references,
        )
    }

    @Test
    fun `bare spotify media id is not promoted to a spotify track reference`() {
        val snapshot = MediaSessionSnapshotNormalizer.normalize(
            MediaSessionSnapshotInput(
                sourceId = SpotifyPlaybackReference.SOURCE_ID,
                title = "Example Song",
                mediaId = spotifyId,
            ),
        )

        assertTrue(snapshot.track?.references.orEmpty().isEmpty())
        assertEquals(spotifyId, snapshot.source?.mediaId)
    }

    @Test
    fun `spotify looking resource from another source is not promoted`() {
        val snapshot = MediaSessionSnapshotNormalizer.normalize(
            MediaSessionSnapshotInput(
                sourceId = "com.example.player",
                title = "Example Song",
                mediaUri = "spotify:track:$spotifyId",
            ),
        )

        assertTrue(snapshot.track?.references.orEmpty().isEmpty())
    }

    @Test
    fun `display metadata fallbacks produce a track without inventing duration`() {
        val snapshot = MediaSessionSnapshotNormalizer.normalize(
            MediaSessionSnapshotInput(
                sourceId = "com.example.player",
                displayTitle = "Display title",
                albumArtist = "Album artist",
                durationMs = 0L,
                positionMs = -10L,
                playbackRate = Float.NaN,
            ),
        )

        assertEquals("Display title", snapshot.track?.title)
        assertEquals(listOf("Album artist"), snapshot.track?.artists)
        assertNull(snapshot.track?.durationMs)
        assertEquals(0L, snapshot.positionMs)
        assertEquals(1.0f, snapshot.playbackRate)
    }

    @Test
    fun `missing title keeps playback snapshot but exposes no track`() {
        val snapshot = MediaSessionSnapshotNormalizer.normalize(
            MediaSessionSnapshotInput(
                sourceId = "com.example.player",
                artist = "Artist",
                mediaId = "item-1",
                status = PlaybackStatus.PAUSED,
            ),
        )

        assertNull(snapshot.track)
        assertEquals("com.example.player", snapshot.source?.id)
        assertEquals("item-1", snapshot.source?.mediaId)
        assertEquals(PlaybackStatus.PAUSED, snapshot.status)
    }
}
