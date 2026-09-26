package io.github.whoxamxl.aalyrics.ui.phone.shell

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlaybackSurfaceTransformTest {
    @Test
    fun `expansion progress maps collapsed and expanded anchors`() {
        assertEquals(
            expected = 0f,
            actual = playbackSurfaceExpansionProgress(
                transformOffsetPx = 0f,
                transformTravelPx = 200f,
            ),
            absoluteTolerance = 0.0001f,
        )
        assertEquals(
            expected = 0.5f,
            actual = playbackSurfaceExpansionProgress(
                transformOffsetPx = -100f,
                transformTravelPx = 200f,
            ),
            absoluteTolerance = 0.0001f,
        )
        assertEquals(
            expected = 1f,
            actual = playbackSurfaceExpansionProgress(
                transformOffsetPx = -200f,
                transformTravelPx = 200f,
            ),
            absoluteTolerance = 0.0001f,
        )
    }

    @Test
    fun `slow release settles to nearest anchor`() {
        assertFalse(
            playbackSurfaceSettlesExpanded(
                expansionProgress = 0.49f,
                velocityPxPerSecond = 0f,
                flingThresholdPxPerSecond = 1_000f,
            ),
        )
        assertTrue(
            playbackSurfaceSettlesExpanded(
                expansionProgress = 0.51f,
                velocityPxPerSecond = 0f,
                flingThresholdPxPerSecond = 1_000f,
            ),
        )
    }

    @Test
    fun `upward fling expands even before midpoint`() {
        assertTrue(
            playbackSurfaceSettlesExpanded(
                expansionProgress = 0.20f,
                velocityPxPerSecond = -1_200f,
                flingThresholdPxPerSecond = 1_000f,
            ),
        )
    }

    @Test
    fun `downward fling collapses even after midpoint`() {
        assertFalse(
            playbackSurfaceSettlesExpanded(
                expansionProgress = 0.80f,
                velocityPxPerSecond = 1_200f,
                flingThresholdPxPerSecond = 1_000f,
            ),
        )
    }
    @Test
    fun `live playback projection uses stable sample fallback when source timestamp is unavailable`() {
        assertEquals(
            6_000L,
            projectedLivePlaybackPositionMs(
                positionMs = 4_000L,
                playbackRate = 1f,
                isPlaying = true,
                durationMs = 20_000L,
                currentMonotonicTimeMs = 12_000L,
                sourceUpdatedAtMonotonicMs = null,
                sampledAtMonotonicMs = 10_000L,
            ),
        )
    }

    @Test
    fun `live playback projection keeps source timestamp authoritative`() {
        assertEquals(
            6_000L,
            projectedLivePlaybackPositionMs(
                positionMs = 4_000L,
                playbackRate = 1f,
                isPlaying = true,
                durationMs = 20_000L,
                currentMonotonicTimeMs = 12_000L,
                sourceUpdatedAtMonotonicMs = 10_000L,
                sampledAtMonotonicMs = 5_000L,
            ),
        )
    }

    @Test
    fun `paused live playback projection does not drift from sample fallback`() {
        assertEquals(
            4_000L,
            projectedLivePlaybackPositionMs(
                positionMs = 4_000L,
                playbackRate = 1f,
                isPlaying = false,
                durationMs = 20_000L,
                currentMonotonicTimeMs = 12_000L,
                sourceUpdatedAtMonotonicMs = null,
                sampledAtMonotonicMs = 10_000L,
            ),
        )
    }

}
