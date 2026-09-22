package io.github.whoxamxl.aalyrics

import io.github.whoxamxl.aalyrics.core.model.PlaybackSnapshot
import io.github.whoxamxl.aalyrics.core.model.PlaybackSource
import io.github.whoxamxl.aalyrics.core.model.PlaybackStatus
import io.github.whoxamxl.aalyrics.core.model.Track
import io.github.whoxamxl.aalyrics.platform.media.PlaybackControlCapabilities
import io.github.whoxamxl.aalyrics.platform.media.PlaybackControlState
import io.github.whoxamxl.aalyrics.platform.media.PlaybackQueueItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PhonePlaybackSurfaceMapperTest {
    @Test
    fun `matching selected session maps capabilities queue and Translation state`() {
        val playback = playback()
        val controls = PlaybackControlState(
            sourcePackageName = "com.example.player",
            capabilities = PlaybackControlCapabilities(
                canPlay = true,
                canPause = true,
                canSkipPrevious = true,
                canSkipNext = true,
                canSkipToQueueItem = true,
                canSeek = true,
            ),
            queue = listOf(
                PlaybackQueueItem(
                    id = 7L,
                    title = "Next Track",
                    subtitle = "Next Artist",
                    artworkUri = "content://com.example.player/artwork/7",
                ),
            ),
            hasSessionActivity = true,
        )

        val state = assertNotNull(
            mapPhonePlaybackSurfaceState(
                playback = playback,
                controlState = controls,
                translationEnabled = false,
                canOpenPlaybackApp = true,
            ),
        )

        assertEquals("Current Track", state.title)
        assertEquals("Artist One, Artist Two", state.artist)
        assertTrue(state.canSeek)
        assertTrue(state.queueAvailable)
        assertEquals(listOf(7L), state.queue.map { it.id })
        assertEquals(
            "content://com.example.player/artwork/7",
            state.queue.single().artworkUri,
        )
        assertTrue(state.canOpenPlaybackApp)
        assertFalse(state.translationEnabled)
    }

    @Test
    fun `published queue remains actionable for skip-to-queue compatibility probe`() {
        val state = assertNotNull(
            mapPhonePlaybackSurfaceState(
                playback = playback(),
                controlState = PlaybackControlState(
                    sourcePackageName = "com.example.player",
                    capabilities = PlaybackControlCapabilities(
                        canPlay = true,
                        canPause = true,
                        canSkipToQueueItem = false,
                    ),
                    queue = listOf(PlaybackQueueItem(8L, "Informational Only")),
                    hasSessionActivity = true,
                ),
                translationEnabled = true,
                canOpenPlaybackApp = true,
            ),
        )

        assertEquals(listOf(8L), state.queue.map { it.id })
        assertFalse(state.canSkipToQueueItem)
        assertTrue(state.queueAvailable)
        assertTrue(state.canOpenPlaybackApp)
    }

    @Test
    fun `stale control state from another package cannot enable actions`() {
        val state = assertNotNull(
            mapPhonePlaybackSurfaceState(
                playback = playback(),
                controlState = PlaybackControlState(
                    sourcePackageName = "com.example.other",
                    capabilities = PlaybackControlCapabilities(
                        canPause = true,
                        canSkipNext = true,
                        canSeek = true,
                    ),
                    queue = listOf(PlaybackQueueItem(9L, "Wrong Session")),
                    hasSessionActivity = true,
                ),
                translationEnabled = true,
                canOpenPlaybackApp = true,
            ),
        )

        assertFalse(state.canPause)
        assertFalse(state.canSkipNext)
        assertFalse(state.canSeek)
        assertTrue(state.queue.isEmpty())
        assertFalse(state.canOpenPlaybackApp)
    }

    private fun playback() = PlaybackSnapshot(
        track = Track(
            title = "Current Track",
            artists = listOf("Artist One", "Artist Two"),
            durationMs = 180_000L,
        ),
        status = PlaybackStatus.PLAYING,
        positionMs = 42_000L,
        playbackRate = 1.0f,
        source = PlaybackSource(id = "com.example.player"),
        positionUpdatedAtMonotonicMs = 1_000L,
    )
}
