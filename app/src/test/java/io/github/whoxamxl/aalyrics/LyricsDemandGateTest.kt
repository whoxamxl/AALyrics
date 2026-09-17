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
    fun `initial no-demand state retains playback without forwarding it`() {
        val forwarded = mutableListOf<PlaybackSnapshot>()
        val gate = LyricsDemandGate(forwarded::add)

        gate.onPlaybackSnapshot(snapshot("First"))

        assertFalse(gate.isActive())
        assertTrue(forwarded.isEmpty())
    }

    @Test
    fun `phone demand replays latest snapshot exactly once and duplicate updates are no-ops`() {
        val forwarded = mutableListOf<PlaybackSnapshot>()
        val gate = LyricsDemandGate(forwarded::add)
        val latest = snapshot("Latest")
        gate.onPlaybackSnapshot(latest)

        gate.setPhoneProcessForeground(true)
        gate.setPhoneProcessForeground(true)

        assertTrue(gate.isActive())
        assertEquals(listOf(latest), forwarded)
    }

    @Test
    fun `automotive demand alone activates and final removal clears exactly once`() {
        val forwarded = mutableListOf<PlaybackSnapshot>()
        val gate = LyricsDemandGate(forwarded::add)
        val latest = snapshot("Projected")
        gate.onPlaybackSnapshot(latest)

        gate.setAutomotiveProjectionConnected(true)
        gate.setAutomotiveProjectionConnected(false)
        gate.setAutomotiveProjectionConnected(false)

        assertFalse(gate.isActive())
        assertEquals(2, forwarded.size)
        assertEquals(latest, forwarded.first())
        assertNull(forwarded.last().track)
        assertEquals(PlaybackStatus.IDLE, forwarded.last().status)
    }

    @Test
    fun `removing one source keeps demand active while the other remains`() {
        val forwarded = mutableListOf<PlaybackSnapshot>()
        val gate = LyricsDemandGate(forwarded::add)
        val latest = snapshot("Shared")
        gate.onPlaybackSnapshot(latest)

        gate.setPhoneProcessForeground(true)
        gate.setAutomotiveProjectionConnected(true)
        gate.setPhoneProcessForeground(false)

        assertTrue(gate.isActive())
        assertEquals(listOf(latest), forwarded)

        gate.setAutomotiveProjectionConnected(false)

        assertFalse(gate.isActive())
        assertEquals(2, forwarded.size)
        assertNull(forwarded.last().track)
    }

    @Test
    fun `newest track observed while demand is off is replayed on activation`() {
        val forwarded = mutableListOf<PlaybackSnapshot>()
        val gate = LyricsDemandGate(forwarded::add)
        gate.onPlaybackSnapshot(snapshot("First"))
        gate.onPlaybackSnapshot(snapshot("Second"))

        gate.setPhoneProcessForeground(true)

        assertEquals(listOf("Second"), forwarded.mapNotNull { it.track?.title })
    }

    @Test
    fun `empty playback observed while demand is off prevents stale replay`() {
        val forwarded = mutableListOf<PlaybackSnapshot>()
        val gate = LyricsDemandGate(forwarded::add)
        gate.onPlaybackSnapshot(snapshot("Gone"))
        gate.onPlaybackSnapshot(PlaybackSnapshot())

        gate.setAutomotiveProjectionConnected(true)

        assertEquals(1, forwarded.size)
        assertNull(forwarded.single().track)
    }

    @Test
    fun `active demand forwards playback churn without replaying demand`() {
        val forwarded = mutableListOf<PlaybackSnapshot>()
        val gate = LyricsDemandGate(forwarded::add)
        gate.setPhoneProcessForeground(true)
        forwarded.clear()
        val first = snapshot("Song")
        val churn = first.copy(status = PlaybackStatus.PAUSED, positionMs = 42_000L)

        gate.onPlaybackSnapshot(first)
        gate.onPlaybackSnapshot(churn)

        assertEquals(listOf(first, churn), forwarded)
    }

    private fun snapshot(title: String) = PlaybackSnapshot(
        track = Track(title = title, artists = listOf("Artist")),
        status = PlaybackStatus.PLAYING,
    )
}
