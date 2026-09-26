package io.github.whoxamxl.aalyrics.platform.media

import io.github.whoxamxl.aalyrics.core.model.PlaybackSnapshot
import io.github.whoxamxl.aalyrics.core.model.PlaybackSource
import io.github.whoxamxl.aalyrics.core.model.PlaybackStatus
import io.github.whoxamxl.aalyrics.core.model.Track
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PlaybackClockReconcilerTest {
    private val reconciler = PlaybackClockReconciler()

    @Test
    fun `old but internally stable source timestamp remains authoritative`() {
        reconciler.reconcile(
            snapshot(positionMs = 40_000L, sourceTimestampMs = 10_000L, sampleTimestampMs = 20_000L),
        )

        val reconciled = reconciler.reconcile(
            snapshot(positionMs = 40_000L, sourceTimestampMs = 10_000L, sampleTimestampMs = 21_000L),
        )

        assertEquals(10_000L, reconciled.positionUpdatedAtMonotonicMs)
    }

    @Test
    fun `same source timestamp with changed raw position falls back to local sample clock`() {
        reconciler.reconcile(
            snapshot(positionMs = 40_000L, sourceTimestampMs = 10_000L, sampleTimestampMs = 20_000L),
        )

        val reconciled = reconciler.reconcile(
            snapshot(positionMs = 41_000L, sourceTimestampMs = 10_000L, sampleTimestampMs = 21_000L),
        )

        assertNull(reconciled.positionUpdatedAtMonotonicMs)
        assertEquals(21_000L, reconciled.positionSampledAtMonotonicMs)
    }

    @Test
    fun `same source timestamp with pause to play transition is rejected`() {
        reconciler.reconcile(
            snapshot(
                status = PlaybackStatus.PAUSED,
                positionMs = 40_000L,
                sourceTimestampMs = 10_000L,
                sampleTimestampMs = 20_000L,
            ),
        )

        val reconciled = reconciler.reconcile(
            snapshot(
                status = PlaybackStatus.PLAYING,
                positionMs = 40_000L,
                sourceTimestampMs = 10_000L,
                sampleTimestampMs = 21_000L,
            ),
        )

        assertNull(reconciled.positionUpdatedAtMonotonicMs)
        assertEquals(21_000L, reconciled.positionSampledAtMonotonicMs)
    }

    @Test
    fun `same source timestamp with playback rate change is rejected`() {
        reconciler.reconcile(
            snapshot(
                playbackRate = 1.0f,
                positionMs = 40_000L,
                sourceTimestampMs = 10_000L,
                sampleTimestampMs = 20_000L,
            ),
        )

        val reconciled = reconciler.reconcile(
            snapshot(
                playbackRate = 1.25f,
                positionMs = 40_000L,
                sourceTimestampMs = 10_000L,
                sampleTimestampMs = 21_000L,
            ),
        )

        assertNull(reconciled.positionUpdatedAtMonotonicMs)
    }

    @Test
    fun `null source timestamp does not clear quarantine for rejected timestamp`() {
        reconciler.reconcile(
            snapshot(positionMs = 40_000L, sourceTimestampMs = 10_000L, sampleTimestampMs = 20_000L),
        )
        reconciler.reconcile(
            snapshot(positionMs = 41_000L, sourceTimestampMs = 10_000L, sampleTimestampMs = 21_000L),
        )

        val missingTimestamp = reconciler.reconcile(
            snapshot(positionMs = 41_500L, sourceTimestampMs = null, sampleTimestampMs = 21_500L),
        )
        val repeatedRejectedTimestamp = reconciler.reconcile(
            snapshot(positionMs = 42_000L, sourceTimestampMs = 10_000L, sampleTimestampMs = 22_000L),
        )

        assertNull(missingTimestamp.positionUpdatedAtMonotonicMs)
        assertNull(repeatedRejectedTimestamp.positionUpdatedAtMonotonicMs)
    }

    @Test
    fun `new valid source timestamp clears quarantine`() {
        reconciler.reconcile(
            snapshot(positionMs = 40_000L, sourceTimestampMs = 10_000L, sampleTimestampMs = 20_000L),
        )
        reconciler.reconcile(
            snapshot(positionMs = 41_000L, sourceTimestampMs = 10_000L, sampleTimestampMs = 21_000L),
        )

        val recovered = reconciler.reconcile(
            snapshot(positionMs = 42_000L, sourceTimestampMs = 22_000L, sampleTimestampMs = 22_100L),
        )

        assertEquals(22_000L, recovered.positionUpdatedAtMonotonicMs)
    }

    @Test
    fun `source timestamp after local sample time is rejected`() {
        val reconciled = reconciler.reconcile(
            snapshot(positionMs = 40_000L, sourceTimestampMs = 20_500L, sampleTimestampMs = 20_000L),
        )

        assertNull(reconciled.positionUpdatedAtMonotonicMs)
    }

    @Test
    fun `source timestamp moving backwards on same track is rejected`() {
        reconciler.reconcile(
            snapshot(positionMs = 40_000L, sourceTimestampMs = 20_000L, sampleTimestampMs = 20_100L),
        )

        val reconciled = reconciler.reconcile(
            snapshot(positionMs = 40_500L, sourceTimestampMs = 19_000L, sampleTimestampMs = 21_000L),
        )

        assertNull(reconciled.positionUpdatedAtMonotonicMs)
    }

    @Test
    fun `rejected backward timestamp does not become next comparison baseline`() {
        val accepted = reconciler.reconcile(
            snapshot(positionMs = 40_000L, sourceTimestampMs = 20_000L, sampleTimestampMs = 20_100L),
        )
        val backward = reconciler.reconcile(
            snapshot(positionMs = 40_500L, sourceTimestampMs = 19_000L, sampleTimestampMs = 21_000L),
        )
        val stillBehindLastAccepted = reconciler.reconcile(
            snapshot(positionMs = 41_000L, sourceTimestampMs = 19_500L, sampleTimestampMs = 21_500L),
        )
        val recovered = reconciler.reconcile(
            snapshot(positionMs = 41_500L, sourceTimestampMs = 20_500L, sampleTimestampMs = 21_600L),
        )

        assertEquals(20_000L, accepted.positionUpdatedAtMonotonicMs)
        assertNull(backward.positionUpdatedAtMonotonicMs)
        assertNull(stillBehindLastAccepted.positionUpdatedAtMonotonicMs)
        assertEquals(20_500L, recovered.positionUpdatedAtMonotonicMs)
    }

    @Test
    fun `future outlier does not poison recovery from last accepted timestamp`() {
        val accepted = reconciler.reconcile(
            snapshot(positionMs = 40_000L, sourceTimestampMs = 20_000L, sampleTimestampMs = 20_100L),
        )
        val futureOutlier = reconciler.reconcile(
            snapshot(positionMs = 40_500L, sourceTimestampMs = 30_000L, sampleTimestampMs = 21_000L),
        )
        val recovered = reconciler.reconcile(
            snapshot(positionMs = 41_000L, sourceTimestampMs = 21_000L, sampleTimestampMs = 21_100L),
        )

        assertEquals(20_000L, accepted.positionUpdatedAtMonotonicMs)
        assertNull(futureOutlier.positionUpdatedAtMonotonicMs)
        assertEquals(21_000L, recovered.positionUpdatedAtMonotonicMs)
    }

    @Test
    fun `different track starts a fresh clock comparison`() {
        reconciler.reconcile(
            snapshot(positionMs = 40_000L, sourceTimestampMs = 10_000L, sampleTimestampMs = 20_000L),
        )

        val changedTrack = reconciler.reconcile(
            snapshot(
                title = "Next",
                positionMs = 1_000L,
                sourceTimestampMs = 10_000L,
                sampleTimestampMs = 20_500L,
            ),
        )

        assertEquals(10_000L, changedTrack.positionUpdatedAtMonotonicMs)
    }

    private fun snapshot(
        title: String = "Track",
        status: PlaybackStatus = PlaybackStatus.PLAYING,
        playbackRate: Float = 1.0f,
        positionMs: Long,
        sourceTimestampMs: Long?,
        sampleTimestampMs: Long,
    ) = PlaybackSnapshot(
        track = Track(title = title, artists = listOf("Artist")),
        status = status,
        positionMs = positionMs,
        playbackRate = playbackRate,
        source = PlaybackSource(id = "com.example.player", mediaId = title.lowercase()),
        positionUpdatedAtMonotonicMs = sourceTimestampMs,
        positionSampledAtMonotonicMs = sampleTimestampMs,
    )
}
