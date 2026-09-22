package io.github.whoxamxl.aalyrics.platform.media

import android.graphics.Bitmap
import io.github.whoxamxl.aalyrics.core.model.PlaybackSnapshot

internal interface RuntimeMediaController<Token> {
    val token: Token
    val packageName: String
    val isPlaying: Boolean

    fun snapshot(): PlaybackSnapshot
    fun artwork(): Bitmap? = null
    fun controlState(): PlaybackControlState
    fun attach(callback: RuntimeMediaControllerCallback)
    fun detach(callback: RuntimeMediaControllerCallback)
    fun play()
    fun pause()
    fun skipToPrevious()
    fun skipToNext()
    fun seekTo(positionMs: Long)
    fun skipToQueueItem(queueItemId: Long)
    fun openSessionActivity(): Boolean
}

internal interface RuntimeMediaControllerCallback {
    fun onMetadataChanged()
    fun onPlaybackStateChanged()
    fun onControlStateChanged()
    fun onSessionDestroyed()
}

internal fun interface ScheduledMetadataTask {
    fun cancel()
}

internal fun interface MetadataTaskScheduler {
    fun schedule(delayMs: Long, task: () -> Unit): ScheduledMetadataTask
}

internal sealed interface MediaSessionSelectionResult {
    data class Connected(
        val packageName: String,
    ) : MediaSessionSelectionResult

    data object Disconnected : MediaSessionSelectionResult

    data class Unavailable(
        val packageName: String? = null,
        val reason: PlaybackSourceUnavailableReason =
            PlaybackSourceUnavailableReason.UNKNOWN,
    ) : MediaSessionSelectionResult
}

internal object MediaSessionSelectionPolicy {
    fun <Token> select(
        currentToken: Token?,
        controllers: List<RuntimeMediaController<Token>>,
        selfPackageName: String,
    ): RuntimeMediaController<Token>? {
        val eligible = controllers.filter { it.packageName != selfPackageName }
        val current = currentToken?.let { token ->
            eligible.firstOrNull { it.token == token }
        }

        return current?.takeIf { it.isPlaying }
            ?: eligible.firstOrNull { it.isPlaying }
            ?: eligible.firstOrNull()
    }
}

/**
 * Owns one selected session callback and forwards normalized snapshots.
 *
 * Track-changing metadata is stabilized for the working fork's 600 ms window.
 * Playback status/timeline updates remain immediate, but retain the last stable
 * track identity while a metadata change is pending.
 */
internal class SelectedMediaSessionRuntime<Token>(
    private val selfPackageName: String,
    private val sink: PlaybackSnapshotSink,
    private val controlStateSink: PlaybackControlStateSink,
    private val artworkSink: PlaybackArtworkSink = PlaybackArtworkSink {},
    private val scheduler: MetadataTaskScheduler,
    private val refreshSessions: () -> Unit,
    private val metadataStabilizationMs: Long = DEFAULT_METADATA_STABILIZATION_MS,
) : PlaybackTransport, PlaybackSessionLauncher {
    private var selectedController: RuntimeMediaController<Token>? = null
    private var selectedCallback: RuntimeMediaControllerCallback? = null
    private var stableSnapshot: PlaybackSnapshot? = null
    private var pendingMetadataTask: ScheduledMetadataTask? = null

    fun updateSessions(
        controllers: List<RuntimeMediaController<Token>>,
    ): MediaSessionSelectionResult {
        val next = MediaSessionSelectionPolicy.select(
            currentToken = selectedController?.token,
            controllers = controllers,
            selfPackageName = selfPackageName,
        )

        if (next != null && next.token == selectedController?.token) {
            return MediaSessionSelectionResult.Connected(next.packageName)
        }

        switchTo(next)

        return when {
            next != null -> MediaSessionSelectionResult.Connected(next.packageName)
            controllers.isEmpty() -> MediaSessionSelectionResult.Disconnected
            else -> MediaSessionSelectionResult.Unavailable(
                packageName = controllers
                    .firstOrNull { it.packageName != selfPackageName }
                    ?.packageName,
                reason = PlaybackSourceUnavailableReason.UNKNOWN,
            )
        }
    }

    fun disconnect() {
        switchTo(null)
    }

    override fun play() = routeTransport { it.play() }

    override fun pause() = routeTransport { it.pause() }

    override fun skipToPrevious() = routeTransport { it.skipToPrevious() }

    override fun skipToNext() = routeTransport { it.skipToNext() }

    override fun seekTo(positionMs: Long) {
        if (positionMs >= 0L) routeTransport { it.seekTo(positionMs) }
    }

    override fun skipToQueueItem(queueItemId: Long) {
        routeTransport { it.skipToQueueItem(queueItemId) }
    }

    override fun openSessionActivity(): Boolean {
        val controller = selectedController ?: return false
        return try {
            controller.openSessionActivity()
        } catch (_: RuntimeException) {
            false
        }
    }

    private fun routeTransport(command: (RuntimeMediaController<Token>) -> Unit) {
        val controller = selectedController ?: return
        try {
            command(controller)
        } catch (_: RuntimeException) {
            // A selected framework session can disappear between observation and command dispatch.
        }
    }

    private fun switchTo(next: RuntimeMediaController<Token>?) {
        pendingMetadataTask?.cancel()
        pendingMetadataTask = null

        val previous = selectedController
        val previousCallback = selectedCallback
        selectedController = null
        selectedCallback = null
        if (previous != null && previousCallback != null) {
            try {
                previous.detach(previousCallback)
            } catch (_: RuntimeException) {
                // Ownership still moves on; token guards make late callbacks stale.
            }
        }

        stableSnapshot = null
        if (next == null) {
            sink.onPlaybackSnapshot(PlaybackSnapshot())
            controlStateSink.onPlaybackControlState(PlaybackControlState())
            artworkSink.onPlaybackArtwork(null)
            return
        }

        val callback = callbackFor(next.token)
        selectedController = next
        selectedCallback = callback
        try {
            next.attach(callback)
            val snapshot = next.snapshot()
            stableSnapshot = snapshot
            sink.onPlaybackSnapshot(snapshot)
            controlStateSink.onPlaybackControlState(next.controlState())
            artworkSink.onPlaybackArtwork(next.artwork())
        } catch (failure: RuntimeException) {
            try {
                next.detach(callback)
            } catch (_: RuntimeException) {
                // Preserve the original attach/snapshot failure.
            }
            selectedController = null
            selectedCallback = null
            stableSnapshot = null
            sink.onPlaybackSnapshot(PlaybackSnapshot())
            controlStateSink.onPlaybackControlState(PlaybackControlState())
            artworkSink.onPlaybackArtwork(null)
            throw failure
        }
    }

    private fun callbackFor(token: Token): RuntimeMediaControllerCallback =
        object : RuntimeMediaControllerCallback {
            override fun onMetadataChanged() {
                if (!owns(token)) return
                val latest = selectedController?.snapshot() ?: return
                if (latest.trackIdentity == stableSnapshot?.trackIdentity) {
                    stableSnapshot = latest
                    sink.onPlaybackSnapshot(latest)
                    artworkSink.onPlaybackArtwork(selectedController?.artwork())
                } else {
                    scheduleMetadata(token)
                }
            }

            override fun onPlaybackStateChanged() {
                val current = selectedController?.takeIf { it.token == token } ?: return
                val latest = current.snapshot()
                controlStateSink.onPlaybackControlState(current.controlState())
                if (
                    pendingMetadataTask == null &&
                    latest.trackIdentity != stableSnapshot?.trackIdentity
                ) {
                    scheduleMetadata(token)
                }
                val forwarded = if (pendingMetadataTask != null) {
                    stableSnapshot?.let { stable ->
                        latest.copy(track = stable.track, source = stable.source)
                    } ?: latest
                } else {
                    stableSnapshot = latest
                    latest
                }
                sink.onPlaybackSnapshot(forwarded)
                if (!current.isPlaying) {
                    refreshSessions()
                }
            }

            override fun onControlStateChanged() {
                val current = selectedController?.takeIf { it.token == token } ?: return
                controlStateSink.onPlaybackControlState(current.controlState())
            }

            override fun onSessionDestroyed() {
                if (!owns(token)) return
                switchTo(null)
                refreshSessions()
            }
        }

    private fun scheduleMetadata(token: Token) {
        pendingMetadataTask?.cancel()
        pendingMetadataTask = scheduler.schedule(metadataStabilizationMs) {
            pendingMetadataTask = null
            val current = selectedController?.takeIf { it.token == token } ?: return@schedule
            val snapshot = current.snapshot()
            stableSnapshot = snapshot
            sink.onPlaybackSnapshot(snapshot)
            artworkSink.onPlaybackArtwork(current.artwork())
        }
    }

    private fun owns(token: Token): Boolean = selectedController?.token == token

    private companion object {
        const val DEFAULT_METADATA_STABILIZATION_MS = 600L
    }
}
