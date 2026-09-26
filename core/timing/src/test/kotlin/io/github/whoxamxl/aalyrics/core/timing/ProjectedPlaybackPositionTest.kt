package io.github.whoxamxl.aalyrics.core.timing

import io.github.whoxamxl.aalyrics.core.model.PlaybackSnapshot
import io.github.whoxamxl.aalyrics.core.model.PlaybackStatus
import io.github.whoxamxl.aalyrics.core.model.Track
import kotlin.test.Test
import kotlin.test.assertEquals

class ProjectedPlaybackPositionTest {
    private val track = Track(title = "Song", artists = listOf("Artist"), durationMs = 20_000L)

    @Test
    fun `source timestamp takes precedence over sample timestamp`() {
        val playback = PlaybackSnapshot(
            track = track,
            status = PlaybackStatus.PLAYING,
            positionMs = 5_000L,
            positionUpdatedAtMonotonicMs = 1_000L,
            positionSampledAtMonotonicMs = 1_400L,
        )

        assertEquals(5_750L, projectedPlaybackPosition(playback, 1_750L))
    }

    @Test
    fun `sample timestamp remains fallback anchor when source timestamp is absent`() {
        val playback = PlaybackSnapshot(
            track = track,
            status = PlaybackStatus.PLAYING,
            positionMs = 5_000L,
            positionSampledAtMonotonicMs = 1_000L,
        )

        assertEquals(5_750L, projectedPlaybackPosition(playback, 1_750L))
    }

    @Test
    fun `paused position does not advance and playing position clamps to duration`() {
        val sample = PlaybackSnapshot(
            track = track,
            status = PlaybackStatus.PAUSED,
            positionMs = 19_500L,
            positionSampledAtMonotonicMs = 1_000L,
        )

        assertEquals(19_500L, projectedPlaybackPosition(sample, 2_000L))
        assertEquals(20_000L, projectedPlaybackPosition(sample.copy(status = PlaybackStatus.PLAYING), 2_000L))
    }
}
