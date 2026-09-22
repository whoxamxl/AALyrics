package io.github.whoxamxl.aalyrics

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import io.github.whoxamxl.aalyrics.core.model.PlaybackSnapshot
import io.github.whoxamxl.aalyrics.core.model.Track
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PhoneProcessDemandObserverTest {
    @Test
    fun `duplicate process start across activity recreation does not flap demand`() {
        val forwarded = mutableListOf<PlaybackSnapshot>()
        val gate = LyricsDemandGate(forwarded::add) {}
        gate.onPlaybackSnapshot(
            snapshot = PlaybackSnapshot(
                track = Track(title = "Song", artists = listOf("Artist")),
            ),
            sourceEligible = true,
        )
        val observer = PhoneProcessDemandObserver(gate)
        val owner = TestLifecycleOwner()

        observer.onStart(owner)
        observer.onStart(owner)

        assertTrue(gate.isActive())
        assertEquals(1, forwarded.size)
        assertEquals("Song", forwarded.single().track?.title)
    }

    private class TestLifecycleOwner : LifecycleOwner {
        private val registry = LifecycleRegistry(this)
        override val lifecycle: Lifecycle = registry
    }
}
