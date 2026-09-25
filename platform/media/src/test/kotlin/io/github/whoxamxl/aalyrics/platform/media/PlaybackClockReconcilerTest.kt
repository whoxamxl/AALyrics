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
        val first = snapshot(positionMs = 40_000L, sourceTimestampMs = 10_000L, sampleTimestampMs = 20_000L)
        val second = snapshot(positionMs = 40_000L, sourceTimestampMs = 10_000L, sampleTimestampMs = 21_000L)

        reconciler.reconcile(first)
        val reconciled = reconciler.reconcile(second)

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
    fun `rejected source timestamp stays rejected until source publishes a new timestamp`() {
        reconciler.reconcile(
            snapshot(positionMs = 40_000L, sourceTimestampMs = 10_000L, sampleTimestampMs = 20_000L),
        )
        reconciler.reconcile(
            snapshot(positionMs = 41_000L, sourceTimestampMs = 10_000L, sampleTimestampMs = 21_000L),
        )

        val repeated = reconciler.reconcile(
            snapshot(positionMs = 41_000L, sourceTimestampMs = 10_000L, sampleTimestampMs = 21_500L),
        )
        val recovered = reconciler.reconcile(
            snapshot(positionMs = 42_000L, sourceTimestampMs = 22_000L, sampleTimestampMs = 22_100L),
        )

        assertNull(repeated.positionUpdatedAtMonotonicMs)
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
        positionMs: Long,
        sourceTimestampMs: Long?,
        sampleTimestampMs: Long,
    ) = PlaybackSnapshot(
        track = Track(title = title, artists = listOf("Artist")),
        status = PlaybackStatus.PLAYING,
        positionMs = positionMs,
        source = PlaybackSource(id = "com.example.player", mediaId = title.lowercase()),
        positionUpdatedAtMonotonicMs = sourceTimestampMs,
        positionSampledAtMonotonicMs = sampleTimestampMs,
    )
}
