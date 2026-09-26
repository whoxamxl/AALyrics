package io.github.whoxamxl.aalyrics.platform.media

import io.github.whoxamxl.aalyrics.core.model.PlaybackSnapshot
import io.github.whoxamxl.aalyrics.core.model.PlaybackSource
import io.github.whoxamxl.aalyrics.core.model.PlaybackStatus
import io.github.whoxamxl.aalyrics.core.model.Track
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SelectedMediaSessionRuntimeTest {
    @Test
    fun `selection change detaches old callback attaches new callback and forwards snapshot`() {
        val scheduler = FakeScheduler()
        val snapshots = mutableListOf<PlaybackSnapshot>()
        val runtime = runtime(scheduler, snapshots)
        val first = controller("first", "First", playing = true)
        val second = controller("second", "Second", playing = true)

        runtime.updateSessions(listOf(first))
        first.playing = false
        runtime.updateSessions(listOf(first, second))

        assertEquals(1, first.attachCount)
        assertEquals(1, first.detachCount)
        assertEquals(1, second.attachCount)
        assertEquals(listOf("First", "Second"), snapshots.mapNotNull { it.track?.title })
    }

    @Test
    fun `empty session list detaches ownership and forwards empty playback`() {
        val snapshots = mutableListOf<PlaybackSnapshot>()
        val first = controller("first", "First", playing = true)
        val runtime = runtime(FakeScheduler(), snapshots)

        runtime.updateSessions(listOf(first))
        runtime.updateSessions(emptyList())

        assertEquals(1, first.detachCount)
        assertNull(snapshots.last().track)
        assertEquals(PlaybackStatus.IDLE, snapshots.last().status)
    }

    @Test
    fun `destroyed selected session clears then refreshes to replacement`() {
        val snapshots = mutableListOf<PlaybackSnapshot>()
        val first = controller("first", "First", playing = true)
        val second = controller("second", "Second", playing = true)
        lateinit var runtime: SelectedMediaSessionRuntime<String>
        var refreshCount = 0
        runtime = runtime(FakeScheduler(), snapshots) {
            refreshCount += 1
            runtime.updateSessions(listOf(second))
        }

        runtime.updateSessions(listOf(first))
        first.destroy()

        assertEquals(1, refreshCount)
        assertEquals(1, first.detachCount)
        assertEquals(1, second.attachCount)
        assertNull(snapshots[snapshots.lastIndex - 1].track)
        assertEquals("Second", snapshots.last().track?.title)
    }

    @Test
    fun `stopped selected session refreshes to an already playing replacement`() {
        val snapshots = mutableListOf<PlaybackSnapshot>()
        val first = controller("first", "First", playing = true)
        val second = controller("second", "Second", playing = true)
        lateinit var runtime: SelectedMediaSessionRuntime<String>
        var refreshCount = 0
        runtime = runtime(FakeScheduler(), snapshots) {
            refreshCount += 1
            runtime.updateSessions(listOf(first, second))
        }

        runtime.updateSessions(listOf(first, second))
        first.playing = false
        first.snapshot = snapshot("First", status = PlaybackStatus.PAUSED)
        first.playbackChanged()

        assertEquals(1, refreshCount)
        assertEquals(1, first.detachCount)
        assertEquals(1, second.attachCount)
        assertEquals("Second", snapshots.last().track?.title)
        assertEquals(PlaybackStatus.PLAYING, snapshots.last().status)
    }

    @Test
    fun `transport commands route only to the selected controller`() {
        val snapshots = mutableListOf<PlaybackSnapshot>()
        val first = controller("first", "First", playing = true)
        val second = controller("second", "Second", playing = true)
        val runtime = runtime(FakeScheduler(), snapshots)

        runtime.updateSessions(listOf(first))
        runtime.play()
        runtime.pause()
        runtime.skipToPrevious()
        runtime.skipToNext()
        runtime.seekTo(12_345L)
        runtime.skipToQueueItem(42L)

        first.playing = false
        runtime.updateSessions(listOf(first, second))
        runtime.play()

        assertEquals(1, first.playCount)
        assertEquals(1, first.pauseCount)
        assertEquals(1, first.previousCount)
        assertEquals(1, first.nextCount)
        assertEquals(listOf(12_345L), first.seekPositions)
        assertEquals(listOf(42L), first.queueItemIds)
        assertEquals(1, second.playCount)
    }


    @Test
    fun `selected session launch routes only to the selected controller`() {
        val snapshots = mutableListOf<PlaybackSnapshot>()
        val first = controller("first", "First", playing = true)
        val second = controller("second", "Second", playing = true)
        val runtime = runtime(FakeScheduler(), snapshots)

        runtime.updateSessions(listOf(first))
        assertEquals(true, runtime.openSessionActivity())

        first.playing = false
        runtime.updateSessions(listOf(first, second))
        second.sessionLaunchResult = false
        assertEquals(false, runtime.openSessionActivity())

        assertEquals(1, first.sessionLaunchCount)
        assertEquals(1, second.sessionLaunchCount)
    }

    @Test
    fun `control state follows selected session playback and queue changes`() {
        val snapshots = mutableListOf<PlaybackSnapshot>()
        val controlStates = mutableListOf<PlaybackControlState>()
        val session = controller("session", "Track", playing = true).apply {
            controls = PlaybackControlState(
                sourcePackageName = packageName,
                capabilities = PlaybackControlCapabilities(
                    canPlay = true,
                    canPause = true,
                    canSkipNext = true,
                    canSkipToQueueItem = true,
                    canSeek = true,
                ),
                queue = listOf(PlaybackQueueItem(1L, "First")),
                hasSessionActivity = true,
            )
        }
        val runtime = runtime(
            scheduler = FakeScheduler(),
            snapshots = snapshots,
            controlStates = controlStates,
        )

        runtime.updateSessions(listOf(session))
        session.controls = session.controls.copy(
            queue = listOf(
                PlaybackQueueItem(1L, "First"),
                PlaybackQueueItem(2L, "Second", "Artist"),
            ),
        )
        session.controlStateChanged()

        assertEquals(2, controlStates.size)
        assertEquals("com.example.session", controlStates.first().sourcePackageName)
        assertEquals(true, controlStates.first().capabilities.canSeek)
        assertEquals(true, controlStates.first().capabilities.canSkipToQueueItem)
        assertEquals(listOf(1L, 2L), controlStates.last().queue.map { it.id })

        runtime.disconnect()
        assertEquals(PlaybackControlState(), controlStates.last())
    }

    @Test
    fun `pause to play with unchanged source timestamp falls back before Phone projection`() {
        val scheduler = FakeScheduler()
        val snapshots = mutableListOf<PlaybackSnapshot>()
        val session = controller("session", "Track", playing = false)
        session.snapshot = snapshot(
            title = "Track",
            status = PlaybackStatus.PAUSED,
            positionMs = 40_000L,
            sourceTimestampMs = 10_000L,
            sampleTimestampMs = 20_000L,
        )
        val runtime = runtime(scheduler, snapshots)

        runtime.updateSessions(listOf(session))
        session.playing = true
        session.snapshot = snapshot(
            title = "Track",
            status = PlaybackStatus.PLAYING,
            positionMs = 40_000L,
            sourceTimestampMs = 10_000L,
            sampleTimestampMs = 21_000L,
        )
        session.playbackChanged()

        assertEquals(2, snapshots.size)
        assertNull(snapshots.last().positionUpdatedAtMonotonicMs)
        assertEquals(21_000L, snapshots.last().positionSampledAtMonotonicMs)
    }

    @Test
    fun `clock validation does not mix pending track timeline into stable identity`() {
        val scheduler = FakeScheduler()
        val snapshots = mutableListOf<PlaybackSnapshot>()
        val session = controller("session", "Stable", playing = true)
        session.snapshot = snapshot(
            title = "Stable",
            status = PlaybackStatus.PLAYING,
            positionMs = 40_000L,
            sourceTimestampMs = 10_000L,
            sampleTimestampMs = 20_000L,
        )
        val runtime = runtime(scheduler, snapshots)

        runtime.updateSessions(listOf(session))

        session.snapshot = snapshot(
            title = "Next",
            status = PlaybackStatus.PLAYING,
            positionMs = 1_000L,
            sourceTimestampMs = 10_000L,
            sampleTimestampMs = 20_100L,
        )
        session.metadataChanged()
        assertEquals(listOf(250L, 600L), scheduler.scheduledDelays)

        session.snapshot = snapshot(
            title = "Next",
            status = PlaybackStatus.PLAYING,
            positionMs = 1_250L,
            sourceTimestampMs = 10_000L,
            sampleTimestampMs = 20_250L,
        )
        scheduler.runNext()

        assertEquals(1, snapshots.size)
        assertEquals("Stable", snapshots.single().track?.title)
        assertEquals(40_000L, snapshots.single().positionMs)

        session.snapshot = snapshot(
            title = "Next",
            status = PlaybackStatus.PLAYING,
            positionMs = 1_500L,
            sourceTimestampMs = 10_000L,
            sampleTimestampMs = 20_600L,
        )
        scheduler.runNext()

        assertEquals(2, snapshots.size)
        assertEquals("Next", snapshots.last().track?.title)
        assertEquals(1_500L, snapshots.last().positionMs)
        assertNull(snapshots.last().positionUpdatedAtMonotonicMs)
        assertEquals(20_600L, snapshots.last().positionSampledAtMonotonicMs)
    }

    @Test
    fun `initial playing snapshot is resampled to reject stale source timestamp paired with moving position`() {
        val scheduler = FakeScheduler()
        val snapshots = mutableListOf<PlaybackSnapshot>()
        val session = controller("session", "Track", playing = true)
        session.snapshot = snapshot(
            title = "Track",
            status = PlaybackStatus.PLAYING,
            positionMs = 40_000L,
            sourceTimestampMs = 10_000L,
            sampleTimestampMs = 20_000L,
        )
        val runtime = runtime(scheduler, snapshots)

        runtime.updateSessions(listOf(session))
        assertEquals(listOf(250L), scheduler.scheduledDelays)

        session.snapshot = snapshot(
            title = "Track",
            status = PlaybackStatus.PLAYING,
            positionMs = 40_250L,
            sourceTimestampMs = 10_000L,
            sampleTimestampMs = 20_250L,
        )
        scheduler.runPending()

        assertEquals(2, snapshots.size)
        assertNull(snapshots.last().positionUpdatedAtMonotonicMs)
        assertEquals(20_250L, snapshots.last().positionSampledAtMonotonicMs)
    }

    @Test
    fun `initial resample does not reject a valid old stationary MediaSession anchor`() {
        val scheduler = FakeScheduler()
        val snapshots = mutableListOf<PlaybackSnapshot>()
        val session = controller("session", "Track", playing = true)
        session.snapshot = snapshot(
            title = "Track",
            status = PlaybackStatus.PLAYING,
            positionMs = 40_000L,
            sourceTimestampMs = 10_000L,
            sampleTimestampMs = 20_000L,
        )
        val runtime = runtime(scheduler, snapshots)

        runtime.updateSessions(listOf(session))
        session.snapshot = snapshot(
            title = "Track",
            status = PlaybackStatus.PLAYING,
            positionMs = 40_000L,
            sourceTimestampMs = 10_000L,
            sampleTimestampMs = 20_250L,
        )
        scheduler.runPending()

        assertEquals(1, snapshots.size)
        assertEquals(10_000L, snapshots.single().positionUpdatedAtMonotonicMs)
    }

    @Test
    fun `pending metadata holds cross identity playback timeline until atomic commit`() {
        val scheduler = FakeScheduler()
        val snapshots = mutableListOf<PlaybackSnapshot>()
        val session = controller("session", "Stable", playing = false)
        session.snapshot = snapshot(
            title = "Stable",
            status = PlaybackStatus.PAUSED,
            positionMs = 180_000L,
        )
        val runtime = runtime(scheduler, snapshots)

        runtime.updateSessions(listOf(session))
        session.snapshot = snapshot(
            title = "Transient",
            status = PlaybackStatus.PLAYING,
            positionMs = 2_000L,
        )
        session.metadataChanged()
        session.playbackChanged()

        assertEquals(1, scheduler.pendingCount)
        assertEquals(listOf(600L), scheduler.scheduledDelays)
        assertEquals(1, snapshots.size)
        assertEquals("Stable", snapshots.single().track?.title)
        assertEquals(PlaybackStatus.PAUSED, snapshots.single().status)
        assertEquals(180_000L, snapshots.single().positionMs)

        scheduler.runPending()

        assertEquals(2, snapshots.size)
        assertEquals("Transient", snapshots.last().track?.title)
        assertEquals(PlaybackStatus.PLAYING, snapshots.last().status)
        assertEquals(2_000L, snapshots.last().positionMs)
    }

    @Test
    fun `same identity playback update remains live while metadata candidate is pending`() {
        val scheduler = FakeScheduler()
        val snapshots = mutableListOf<PlaybackSnapshot>()
        val session = controller("session", "Stable", playing = true)
        session.snapshot = snapshot(
            title = "Stable",
            status = PlaybackStatus.PLAYING,
            positionMs = 10_000L,
        )
        val runtime = runtime(scheduler, snapshots)

        runtime.updateSessions(listOf(session))

        session.snapshot = snapshot(
            title = "Transient",
            status = PlaybackStatus.PLAYING,
            positionMs = 1_000L,
        )
        session.metadataChanged()
        assertEquals(1, scheduler.pendingCount)

        session.snapshot = snapshot(
            title = "Stable",
            status = PlaybackStatus.PLAYING,
            positionMs = 11_000L,
        )
        session.playbackChanged()

        assertEquals(2, snapshots.size)
        assertEquals("Stable", snapshots.last().track?.title)
        assertEquals(11_000L, snapshots.last().positionMs)
    }

    @Test
    fun `new metadata replaces an older pending stabilization task`() {
        val scheduler = FakeScheduler()
        val snapshots = mutableListOf<PlaybackSnapshot>()
        val session = controller("session", "Stable", playing = true)
        val runtime = runtime(scheduler, snapshots)

        runtime.updateSessions(listOf(session))
        session.snapshot = snapshot("Intermediate")
        session.metadataChanged()
        session.snapshot = snapshot("Final")
        session.metadataChanged()
        scheduler.runPending()

        assertEquals(listOf("Stable", "Final"), snapshots.mapNotNull { it.track?.title })
    }

    @Test
    fun `playback callback cannot bypass stabilization or synthesize cross identity snapshot`() {
        val scheduler = FakeScheduler()
        val snapshots = mutableListOf<PlaybackSnapshot>()
        val session = controller("session", "Stable", playing = true)
        session.snapshot = snapshot(
            title = "Stable",
            status = PlaybackStatus.PLAYING,
            positionMs = 120_000L,
        )
        val runtime = runtime(scheduler, snapshots)

        runtime.updateSessions(listOf(session))
        session.snapshot = snapshot(
            title = "Next",
            status = PlaybackStatus.PAUSED,
            positionMs = 5_000L,
        )
        session.playbackChanged()

        assertEquals(1, snapshots.size)
        assertEquals("Stable", snapshots.single().track?.title)
        assertEquals(PlaybackStatus.PLAYING, snapshots.single().status)
        assertEquals(120_000L, snapshots.single().positionMs)
        assertEquals(1, scheduler.pendingCount)

        scheduler.runPending()

        assertEquals(2, snapshots.size)
        assertEquals("Next", snapshots.last().track?.title)
        assertEquals(PlaybackStatus.PAUSED, snapshots.last().status)
        assertEquals(5_000L, snapshots.last().positionMs)
    }

    private fun runtime(
        scheduler: FakeScheduler,
        snapshots: MutableList<PlaybackSnapshot>,
        controlStates: MutableList<PlaybackControlState> = mutableListOf(),
        refresh: () -> Unit = {},
    ): SelectedMediaSessionRuntime<String> = SelectedMediaSessionRuntime(
        selfPackageName = SELF_PACKAGE,
        sink = PlaybackSnapshotSink { snapshots += it },
        controlStateSink = PlaybackControlStateSink { controlStates += it },
        scheduler = scheduler,
        refreshSessions = refresh,
    )

    private fun controller(
        token: String,
        title: String,
        playing: Boolean,
    ) = FakeController(
        token = token,
        packageName = "com.example.$token",
        playing = playing,
        snapshot = snapshot(
            title,
            status = if (playing) PlaybackStatus.PLAYING else PlaybackStatus.PAUSED,
        ),
    )

    private class FakeController(
        override val token: String,
        override val packageName: String,
        var playing: Boolean,
        var snapshot: PlaybackSnapshot,
    ) : RuntimeMediaController<String> {
        private val callbacks = linkedSetOf<RuntimeMediaControllerCallback>()
        var attachCount = 0
        var detachCount = 0
        var playCount = 0
        var pauseCount = 0
        var previousCount = 0
        var nextCount = 0
        val seekPositions = mutableListOf<Long>()
        val queueItemIds = mutableListOf<Long>()
        var sessionLaunchCount = 0
        var sessionLaunchResult = true
        var controls = PlaybackControlState(
            sourcePackageName = packageName,
            capabilities = PlaybackControlCapabilities(
                canPlay = true,
                canPause = true,
                canSkipPrevious = true,
                canSkipNext = true,
                canSeek = true,
            ),
        )

        override val isPlaying: Boolean get() = playing
        override fun snapshot(): PlaybackSnapshot = snapshot
        override fun controlState(): PlaybackControlState = controls

        override fun attach(callback: RuntimeMediaControllerCallback) {
            attachCount += 1
            callbacks += callback
        }

        override fun detach(callback: RuntimeMediaControllerCallback) {
            detachCount += 1
            callbacks -= callback
        }

        override fun play() {
            playCount += 1
        }

        override fun pause() {
            pauseCount += 1
        }

        override fun skipToPrevious() {
            previousCount += 1
        }

        override fun skipToNext() {
            nextCount += 1
        }

        override fun seekTo(positionMs: Long) {
            seekPositions += positionMs
        }

        override fun skipToQueueItem(queueItemId: Long) {
            queueItemIds += queueItemId
        }

        override fun openSessionActivity(): Boolean {
            sessionLaunchCount += 1
            return sessionLaunchResult
        }

        fun metadataChanged() = callbacks.toList().forEach { it.onMetadataChanged() }
        fun playbackChanged() = callbacks.toList().forEach { it.onPlaybackStateChanged() }
        fun controlStateChanged() = callbacks.toList().forEach { it.onControlStateChanged() }
        fun destroy() = callbacks.toList().forEach { it.onSessionDestroyed() }
    }

    private class FakeScheduler : MetadataTaskScheduler {
        private val tasks = mutableListOf<Task>()
        val scheduledDelays = mutableListOf<Long>()
        val pendingCount: Int get() = tasks.count { !it.cancelled }

        override fun schedule(delayMs: Long, task: () -> Unit): ScheduledMetadataTask {
            scheduledDelays += delayMs
            return Task(task).also(tasks::add)
        }

        fun runNext() {
            while (tasks.isNotEmpty()) {
                val next = tasks.removeAt(0)
                if (!next.cancelled) {
                    next.block()
                    return
                }
            }
        }

        fun runPending() {
            val pending = tasks.toList()
            tasks.clear()
            pending.filterNot { it.cancelled }.forEach { it.block() }
        }

        private class Task(val block: () -> Unit) : ScheduledMetadataTask {
            var cancelled = false
            override fun cancel() {
                cancelled = true
            }
        }
    }

    private companion object {
        const val SELF_PACKAGE = "io.github.whoxamxl.aalyrics"

        fun snapshot(
            title: String,
            status: PlaybackStatus = PlaybackStatus.PLAYING,
            positionMs: Long = 0L,
            sourceTimestampMs: Long? = null,
            sampleTimestampMs: Long? = null,
        ) = PlaybackSnapshot(
            track = Track(title = title, artists = listOf("Artist")),
            status = status,
            positionMs = positionMs,
            source = PlaybackSource(id = "com.example.player", mediaId = title.lowercase()),
            positionUpdatedAtMonotonicMs = sourceTimestampMs,
            positionSampledAtMonotonicMs = sampleTimestampMs,
        )
    }
}
