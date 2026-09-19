package io.github.whoxamxl.aalyrics.platform.media

import io.github.whoxamxl.aalyrics.core.model.PlaybackSnapshot
import io.github.whoxamxl.aalyrics.core.model.Track
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MediaSessionObservationTest {
    @Test
    fun `active sessions are not read before listener connection`() {
        val source = FakeSource()
        val observation = observation(source, mutableListOf())

        observation.refresh()

        assertEquals(0, source.currentSessionsCount)
        assertEquals(0, source.registerCount)
    }

    @Test
    fun `security failure detaches observation and clears selected playback`() {
        val snapshots = mutableListOf<PlaybackSnapshot>()
        val source = FakeSource()
        val observation = observation(source, snapshots)
        source.sessions = listOf(FakeController("selected"))

        observation.connect()
        source.failCurrentSessions = true
        observation.refresh()

        assertEquals(1, source.unregisterCount)
        assertNull(snapshots.last().track)

        val readsAfterFailure = source.currentSessionsCount
        observation.refresh()
        assertEquals(readsAfterFailure, source.currentSessionsCount)
    }

    @Test
    fun `disconnect unregisters listener detaches callback and clears playback`() {
        val snapshots = mutableListOf<PlaybackSnapshot>()
        val controller = FakeController("selected")
        val source = FakeSource().apply { sessions = listOf(controller) }
        val observation = observation(source, snapshots)

        observation.connect()
        observation.disconnect()

        assertEquals(1, source.unregisterCount)
        assertEquals(1, controller.detachCount)
        assertNull(snapshots.last().track)
    }

    private fun observation(
        source: FakeSource,
        snapshots: MutableList<PlaybackSnapshot>,
    ): MediaSessionObservation<String> {
        lateinit var observation: MediaSessionObservation<String>
        val runtime = SelectedMediaSessionRuntime<String>(
            selfPackageName = SELF_PACKAGE,
            sink = PlaybackSnapshotSink { snapshots += it },
            scheduler = MetadataTaskScheduler { _, _ -> ScheduledMetadataTask {} },
            refreshSessions = { observation.refresh() },
        )
        observation = MediaSessionObservation(source, runtime)
        return observation
    }

    private class FakeSource : ActiveSessionSource<String> {
        var sessions: List<RuntimeMediaController<String>> = emptyList()
        var failCurrentSessions = false
        var registerCount = 0
        var unregisterCount = 0
        var currentSessionsCount = 0
        private var listener: ((List<RuntimeMediaController<String>>) -> Unit)? = null

        override fun register(listener: (List<RuntimeMediaController<String>>) -> Unit) {
            registerCount += 1
            this.listener = listener
        }

        override fun unregister() {
            unregisterCount += 1
            listener = null
        }

        override fun currentSessions(): List<RuntimeMediaController<String>> {
            currentSessionsCount += 1
            if (failCurrentSessions) throw SecurityException("notification access unavailable")
            return sessions
        }
    }

    private class FakeController(
        override val token: String,
    ) : RuntimeMediaController<String> {
        override val packageName = "com.example.player"
        override val isPlaying = true
        private var callback: RuntimeMediaControllerCallback? = null
        var detachCount = 0

        override fun snapshot() = PlaybackSnapshot(
            track = Track(title = token, artists = listOf("Artist")),
        )

        override fun attach(callback: RuntimeMediaControllerCallback) {
            this.callback = callback
        }

        override fun detach(callback: RuntimeMediaControllerCallback) {
            detachCount += 1
            if (this.callback === callback) this.callback = null
        }

        override fun play() = Unit
        override fun pause() = Unit
        override fun skipToPrevious() = Unit
        override fun skipToNext() = Unit
        override fun seekTo(positionMs: Long) = Unit
    }

    private companion object {
        const val SELF_PACKAGE = "io.github.whoxamxl.aalyrics"
    }
}
