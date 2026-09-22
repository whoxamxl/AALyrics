package io.github.whoxamxl.aalyrics.platform.media

import io.github.whoxamxl.aalyrics.core.model.PlaybackSnapshot
import io.github.whoxamxl.aalyrics.core.model.PlaybackSource
import io.github.whoxamxl.aalyrics.core.model.Track
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class MediaSessionObservationTest {
    @Test
    fun `active sessions are not read before listener connection`() {
        val source = FakeSource()
        val states = mutableListOf<PlaybackSourceRuntimeState>()
        val observation = observation(source, mutableListOf(), states)

        observation.refresh()

        assertEquals(0, source.currentSessionsCount)
        assertEquals(0, source.registerCount)
        assertEquals(emptyList(), states)
    }

    @Test
    fun `empty successful session query moves connecting to disconnected`() {
        val states = mutableListOf<PlaybackSourceRuntimeState>()
        val observation = observation(FakeSource(), mutableListOf(), states)

        observation.connect()

        assertEquals(
            listOf(
                PlaybackSourceRuntimeState.Connecting,
                PlaybackSourceRuntimeState.Disconnected,
            ),
            states,
        )
    }

    @Test
    fun `selected session reports connected package`() {
        val states = mutableListOf<PlaybackSourceRuntimeState>()
        val source = FakeSource().apply {
            sessions = listOf(FakeController("selected"))
        }
        val observation = observation(source, mutableListOf(), states)

        observation.connect()

        assertEquals(PlaybackSourceRuntimeState.Connecting, states.first())
        assertEquals(
            PlaybackSourceRuntimeState.Connected("com.example.player"),
            states.last(),
        )
    }

    @Test
    fun `sessions present but ineligible report unavailable`() {
        val states = mutableListOf<PlaybackSourceRuntimeState>()
        val source = FakeSource().apply {
            sessions = listOf(
                FakeController(
                    token = "self",
                    packageName = SELF_PACKAGE,
                ),
            )
        }
        val observation = observation(source, mutableListOf(), states)

        observation.connect()

        val unavailable = assertIs<PlaybackSourceRuntimeState.Unavailable>(states.last())
        assertEquals(
            PlaybackSourceUnavailableReason.UNKNOWN,
            unavailable.reason,
        )
    }

    @Test
    fun `security failure reports notification access lost and clears playback`() {
        val snapshots = mutableListOf<PlaybackSnapshot>()
        val states = mutableListOf<PlaybackSourceRuntimeState>()
        val source = FakeSource()
        val observation = observation(source, snapshots, states)
        source.sessions = listOf(FakeController("selected"))

        observation.connect()
        source.failCurrentSessionsWithSecurityException = true
        observation.refresh()

        assertEquals(1, source.unregisterCount)
        assertNull(snapshots.last().track)
        assertEquals(
            PlaybackSourceRuntimeState.Error(
                PlaybackSourceErrorReason.NOTIFICATION_ACCESS_LOST,
            ),
            states.last(),
        )
    }

    @Test
    fun `session query runtime failure reports query error`() {
        val states = mutableListOf<PlaybackSourceRuntimeState>()
        val source = FakeSource().apply {
            failCurrentSessionsWithRuntimeException = true
        }
        val observation = observation(source, mutableListOf(), states)

        observation.connect()

        assertEquals(
            PlaybackSourceRuntimeState.Error(
                PlaybackSourceErrorReason.SESSION_QUERY_FAILED,
            ),
            states.last(),
        )
    }

    @Test
    fun `controller attach failure reports session attach error`() {
        val states = mutableListOf<PlaybackSourceRuntimeState>()
        val source = FakeSource().apply {
            sessions = listOf(
                FakeController("selected").apply {
                    failAttach = true
                },
            )
        }
        val observation = observation(source, mutableListOf(), states)

        observation.connect()

        assertEquals(
            PlaybackSourceRuntimeState.Error(
                PlaybackSourceErrorReason.SESSION_ATTACH_FAILED,
            ),
            states.last(),
        )
    }

    @Test
    fun `unexpected registration failure reports unknown error`() {
        val states = mutableListOf<PlaybackSourceRuntimeState>()
        val source = FakeSource().apply {
            failRegister = true
        }
        val observation = observation(source, mutableListOf(), states)

        observation.connect()

        assertEquals(
            PlaybackSourceRuntimeState.Error(PlaybackSourceErrorReason.UNKNOWN),
            states.last(),
        )
    }

    @Test
    fun `listener disconnect reports unknown error clears playback and can reconnect`() {
        val snapshots = mutableListOf<PlaybackSnapshot>()
        val states = mutableListOf<PlaybackSourceRuntimeState>()
        val controller = FakeController("selected")
        val source = FakeSource().apply { sessions = listOf(controller) }
        val observation = observation(source, snapshots, states)

        observation.connect()
        observation.listenerDisconnected()

        assertEquals(1, source.unregisterCount)
        assertEquals(1, controller.detachCount)
        assertNull(snapshots.last().track)
        assertEquals(
            PlaybackSourceRuntimeState.Error(PlaybackSourceErrorReason.UNKNOWN),
            states.last(),
        )

        observation.connect()

        assertEquals(2, source.registerCount)
        assertEquals(
            PlaybackSourceRuntimeState.Connected("com.example.player"),
            states.last(),
        )
    }

    @Test
    fun `disconnect unregisters listener detaches callback and clears playback`() {
        val snapshots = mutableListOf<PlaybackSnapshot>()
        val states = mutableListOf<PlaybackSourceRuntimeState>()
        val controller = FakeController("selected")
        val source = FakeSource().apply { sessions = listOf(controller) }
        val observation = observation(source, snapshots, states)

        observation.connect()
        observation.disconnect()

        assertEquals(1, source.unregisterCount)
        assertEquals(1, controller.detachCount)
        assertNull(snapshots.last().track)
        assertEquals(PlaybackSourceRuntimeState.Disconnected, states.last())
    }

    private fun observation(
        source: FakeSource,
        snapshots: MutableList<PlaybackSnapshot>,
        states: MutableList<PlaybackSourceRuntimeState>,
    ): MediaSessionObservation<String> {
        lateinit var observation: MediaSessionObservation<String>
        val runtime = SelectedMediaSessionRuntime<String>(
            selfPackageName = SELF_PACKAGE,
            sink = PlaybackSnapshotSink { snapshots += it },
            controlStateSink = PlaybackControlStateSink {},
            scheduler = MetadataTaskScheduler { _, _ -> ScheduledMetadataTask {} },
            refreshSessions = { observation.refresh() },
        )
        observation = MediaSessionObservation(
            source = source,
            runtime = runtime,
            stateSink = PlaybackSourceRuntimeStateSink { states += it },
        )
        return observation
    }

    private class FakeSource : ActiveSessionSource<String> {
        var sessions: List<RuntimeMediaController<String>> = emptyList()
        var failRegister = false
        var failCurrentSessionsWithSecurityException = false
        var failCurrentSessionsWithRuntimeException = false
        var registerCount = 0
        var unregisterCount = 0
        var currentSessionsCount = 0
        private var listener: ((List<RuntimeMediaController<String>>) -> Unit)? = null

        override fun register(listener: (List<RuntimeMediaController<String>>) -> Unit) {
            registerCount += 1
            if (failRegister) throw IllegalStateException("registration failed")
            this.listener = listener
        }

        override fun unregister() {
            unregisterCount += 1
            listener = null
        }

        override fun currentSessions(): List<RuntimeMediaController<String>> {
            currentSessionsCount += 1
            if (failCurrentSessionsWithSecurityException) {
                throw SecurityException("notification access unavailable")
            }
            if (failCurrentSessionsWithRuntimeException) {
                throw IllegalStateException("query failed")
            }
            return sessions
        }
    }

    private class FakeController(
        override val token: String,
        override val packageName: String = "com.example.player",
    ) : RuntimeMediaController<String> {
        override val isPlaying = true
        private var callback: RuntimeMediaControllerCallback? = null
        var detachCount = 0
        var failAttach = false

        override fun snapshot() = PlaybackSnapshot(
            track = Track(title = token, artists = listOf("Artist")),
            source = PlaybackSource(id = packageName),
        )

        override fun controlState() = PlaybackControlState(
            sourcePackageName = packageName,
        )

        override fun attach(callback: RuntimeMediaControllerCallback) {
            if (failAttach) throw IllegalStateException("attach failed")
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
        override fun skipToQueueItem(queueItemId: Long) = Unit
        override fun openSessionActivity(): Boolean = false
    }

    private companion object {
        const val SELF_PACKAGE = "io.github.whoxamxl.aalyrics"
    }
}
