package io.github.whoxamxl.aalyrics.ui.phone.shell

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlaybackSurfaceTransformTest {
    @Test
    fun `expansion progress maps collapsed and expanded anchors`() {
        assertEquals(
            0f,
            playbackSurfaceExpansionProgress(
                transformOffsetPx = 0f,
                transformTravelPx = 200f,
            ),
        )
        assertEquals(
            0.5f,
            playbackSurfaceExpansionProgress(
                transformOffsetPx = -100f,
                transformTravelPx = 200f,
            ),
        )
        assertEquals(
            1f,
            playbackSurfaceExpansionProgress(
                transformOffsetPx = -200f,
                transformTravelPx = 200f,
            ),
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
}
