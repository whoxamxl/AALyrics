package io.github.whoxamxl.aalyrics

import io.github.whoxamxl.aalyrics.core.model.PlaybackSnapshot
import io.github.whoxamxl.aalyrics.core.model.PlaybackStatus
import io.github.whoxamxl.aalyrics.core.model.Track
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LyricsDemandGateTest {
    @Test
    fun `initial no-demand state retains eligible playback without forwarding it`() {
        val forwarded = mutableListOf<PlaybackSnapshot>()
        val gate = LyricsDemandGate(forwarded::add) {}

        gate.onPlaybackSnapshot(snapshot("First"), sourceEligible = true)

        assertFalse(gate.isActive())
        assertTrue(forwarded.isEmpty())
    }

    @Test
    fun `phone demand replays latest eligible snapshot exactly once`() {
        val forwarded = mutableListOf<PlaybackSnapshot>()
        val gate = LyricsDemandGate(forwarded::add) {}
        val latest = snapshot("Latest")
        gate.onPlaybackSnapshot(latest, sourceEligible = true)

        gate.setPhoneProcessForeground(true)
        gate.setPhoneProcessForeground(true)

        assertTrue(gate.isActive())
        assertEquals(listOf(latest), forwarded)
    }

    @Test
    fun `automotive demand alone activates and final removal suspends exactly once`() {
        val forwarded = mutableListOf<PlaybackSnapshot>()
        var suspendCount = 0
        val gate = LyricsDemandGate(forwarded::add) { suspendCount += 1 }
        val latest = snapshot("Projected")
        gate.onPlaybackSnapshot(latest, sourceEligible = true)

        gate.setAutomotiveProjectionConnected(true)
        gate.setAutomotiveProjectionConnected(false)
        gate.setAutomotiveProjectionConnected(false)

        assertFalse(gate.isActive())
        assertEquals(listOf(latest), forwarded)
        assertEquals(1, suspendCount)
    }

    @Test
    fun `automotive host service demand alone activates and suspends on final removal`() {
        val forwarded = mutableListOf<PlaybackSnapshot>()
        var suspendCount = 0
        val gate = LyricsDemandGate(forwarded::add) { suspendCount += 1 }
        val latest = snapshot("Host")
        gate.onPlaybackSnapshot(latest, sourceEligible = true)

        gate.setAutomotiveHostActive(true)
        gate.setAutomotiveHostActive(false)

        assertFalse(gate.isActive())
        assertEquals(listOf(latest), forwarded)
        assertEquals(1, suspendCount)
    }

    @Test
    fun `projection removal keeps lookup active while automotive host service remains`() {
        val forwarded = mutableListOf<PlaybackSnapshot>()
        var suspendCount = 0
        val gate = LyricsDemandGate(forwarded::add) { suspendCount += 1 }
        val latest = snapshot("Shared automotive")
        gate.onPlaybackSnapshot(latest, sourceEligible = true)

        gate.setAutomotiveProjectionConnected(true)
        gate.setAutomotiveHostActive(true)
        gate.setAutomotiveProjectionConnected(false)

        assertTrue(gate.isActive())
        assertEquals(listOf(latest), forwarded)
        assertEquals(0, suspendCount)

        gate.setAutomotiveHostActive(false)

        assertFalse(gate.isActive())
        assertEquals(1, suspendCount)
    }

    @Test
    fun `removing one demand source keeps lookup active while the other remains`() {
        val forwarded = mutableListOf<PlaybackSnapshot>()
        var suspendCount = 0
        val gate = LyricsDemandGate(forwarded::add) { suspendCount += 1 }
        val latest = snapshot("Shared")
        gate.onPlaybackSnapshot(latest, sourceEligible = true)

        gate.setPhoneProcessForeground(true)
        gate.setAutomotiveProjectionConnected(true)
        gate.setPhoneProcessForeground(false)

        assertTrue(gate.isActive())
        assertEquals(listOf(latest), forwarded)
        assertEquals(0, suspendCount)

        gate.setAutomotiveProjectionConnected(false)

        assertFalse(gate.isActive())
        assertEquals(listOf(latest), forwarded)
        assertEquals(1, suspendCount)
    }

    @Test
    fun `newest eligible track observed while demand is off is replayed on activation`() {
        val forwarded = mutableListOf<PlaybackSnapshot>()
        val gate = LyricsDemandGate(forwarded::add) {}
        gate.onPlaybackSnapshot(snapshot("First"), sourceEligible = true)
        gate.onPlaybackSnapshot(snapshot("Second"), sourceEligible = true)

        gate.setPhoneProcessForeground(true)

        assertEquals(listOf("Second"), forwarded.mapNotNull { it.track?.title })
    }

    @Test
    fun `empty playback observed while demand is off prevents stale replay`() {
        val forwarded = mutableListOf<PlaybackSnapshot>()
        val gate = LyricsDemandGate(forwarded::add) {}
        gate.onPlaybackSnapshot(snapshot("Gone"), sourceEligible = true)
        gate.onPlaybackSnapshot(PlaybackSnapshot(), sourceEligible = true)

        gate.setAutomotiveProjectionConnected(true)

        assertEquals(1, forwarded.size)
        assertNull(forwarded.single().track)
    }

    @Test
    fun `active demand forwards eligible playback churn`() {
        val forwarded = mutableListOf<PlaybackSnapshot>()
        val gate = LyricsDemandGate(forwarded::add) {}
        gate.onPlaybackSnapshot(PlaybackSnapshot(), sourceEligible = true)
        gate.setPhoneProcessForeground(true)
        forwarded.clear()
        val first = snapshot("Song")
        val churn = first.copy(status = PlaybackStatus.PAUSED, positionMs = 42_000L)

        gate.onPlaybackSnapshot(first, sourceEligible = true)
        gate.onPlaybackSnapshot(churn, sourceEligible = true)

        assertEquals(listOf(first, churn), forwarded)
    }

    @Test
    fun `blocked snapshot never reaches downstream and clears prior eligible lookup`() {
        val forwarded = mutableListOf<PlaybackSnapshot>()
        val gate = LyricsDemandGate(forwarded::add) {}
        gate.onPlaybackSnapshot(snapshot("Allowed"), sourceEligible = true)
        gate.setPhoneProcessForeground(true)

        gate.onPlaybackSnapshot(snapshot("Blocked"), sourceEligible = false)
        gate.onPlaybackSnapshot(
            snapshot("Blocked").copy(positionMs = 12_000L),
            sourceEligible = false,
        )

        assertEquals("Allowed", forwarded.first().track?.title)
        assertEquals(2, forwarded.size)
        assertNull(forwarded.last().track)
    }

    @Test
    fun `source becoming blocked hard-clears even after demand suspension`() {
        val forwarded = mutableListOf<PlaybackSnapshot>()
        var suspendCount = 0
        val gate = LyricsDemandGate(forwarded::add) { suspendCount += 1 }
        val allowed = snapshot("Allowed")
        gate.onPlaybackSnapshot(allowed, sourceEligible = true)
        gate.setPhoneProcessForeground(true)

        gate.setPhoneProcessForeground(false)
        gate.onPlaybackSnapshot(snapshot("Blocked"), sourceEligible = false)

        assertEquals(1, suspendCount)
        assertEquals(2, forwarded.size)
        assertEquals(allowed, forwarded.first())
        assertNull(forwarded.last().track)
    }

    @Test
    fun `blocked to allowed transition forwards only the new allowed snapshot`() {
        val forwarded = mutableListOf<PlaybackSnapshot>()
        val gate = LyricsDemandGate(forwarded::add) {}
        gate.onPlaybackSnapshot(snapshot("Blocked"), sourceEligible = false)
        gate.setPhoneProcessForeground(true)

        gate.onPlaybackSnapshot(snapshot("Allowed"), sourceEligible = true)

        assertEquals(listOf("Allowed"), forwarded.mapNotNull { it.track?.title })
    }

    @Test
    fun `setting change can block and reallow the retained current snapshot`() {
        val forwarded = mutableListOf<PlaybackSnapshot>()
        val gate = LyricsDemandGate(forwarded::add) {}
        val current = snapshot("Current")
        gate.onPlaybackSnapshot(current, sourceEligible = true)
        gate.setPhoneProcessForeground(true)

        gate.setSourceEligible(false)
        gate.setSourceEligible(true)

        assertEquals(3, forwarded.size)
        assertEquals(current, forwarded.first())
        assertNull(forwarded[1].track)
        assertEquals(current, forwarded.last())
    }

    private fun snapshot(title: String) = PlaybackSnapshot(
        track = Track(title = title, artists = listOf("Artist")),
        status = PlaybackStatus.PLAYING,
    )
}
