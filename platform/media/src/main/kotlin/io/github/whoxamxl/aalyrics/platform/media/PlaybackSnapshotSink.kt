package io.github.whoxamxl.aalyrics.platform.media

import android.graphics.Bitmap
import io.github.whoxamxl.aalyrics.core.model.PlaybackSnapshot

/** Narrow platform/application boundary for normalized playback updates. */
fun interface PlaybackSnapshotSink {
    fun onPlaybackSnapshot(snapshot: PlaybackSnapshot)
}

/** Framework-neutral capabilities advertised by the currently selected media session. */
data class PlaybackControlCapabilities(
    val canPlay: Boolean = false,
    val canPause: Boolean = false,
    val canSkipPrevious: Boolean = false,
    val canSkipNext: Boolean = false,
    val canSkipToQueueItem: Boolean = false,
    val canSeek: Boolean = false,
)

/** Framework-neutral queue entry exposed by the currently selected media session. */
data class PlaybackQueueItem(
    val id: Long,
    val title: String,
    val subtitle: String? = null,
)

/**
 * Non-lyrics control state for the currently selected media session.
 *
 * Android framework objects intentionally do not cross this boundary.
 */
data class PlaybackControlState(
    val sourcePackageName: String? = null,
    val capabilities: PlaybackControlCapabilities = PlaybackControlCapabilities(),
    val queue: List<PlaybackQueueItem> = emptyList(),
    val hasSessionActivity: Boolean = false,
) {
    val hasQueue: Boolean
        get() = queue.isNotEmpty()
}

/** Narrow platform/application boundary for selected-session control capabilities. */
fun interface PlaybackControlStateSink {
    fun onPlaybackControlState(state: PlaybackControlState)
}

enum class PlaybackSourceErrorReason {
    NOTIFICATION_ACCESS_LOST,
    SESSION_QUERY_FAILED,
    SESSION_ATTACH_FAILED,
    UNKNOWN,
}

enum class PlaybackSourceUnavailableReason {
    UNSUPPORTED_PLAYER,
    UNKNOWN,
}

sealed interface PlaybackSourceRuntimeState {
    data object Connecting : PlaybackSourceRuntimeState

    data class Connected(
        val packageName: String,
    ) : PlaybackSourceRuntimeState

    data object Disconnected : PlaybackSourceRuntimeState

    data class Unavailable(
        val packageName: String? = null,
        val reason: PlaybackSourceUnavailableReason =
            PlaybackSourceUnavailableReason.UNSUPPORTED_PLAYER,
    ) : PlaybackSourceRuntimeState

    data class Error(
        val reason: PlaybackSourceErrorReason,
    ) : PlaybackSourceRuntimeState
}

fun interface PlaybackSourceRuntimeStateSink {
    fun onPlaybackSourceRuntimeState(state: PlaybackSourceRuntimeState)
}

/**
 * Android-owned artwork boundary for the selected session.
 *
 * Bitmap ownership deliberately stops at :app; presentation modules continue to receive
 * renderable artwork content rather than Android media/framework objects.
 */
fun interface PlaybackArtworkSink {
    fun onPlaybackArtwork(bitmap: Bitmap?)
}

/** Framework-neutral transport commands for the currently selected media session. */
interface PlaybackTransport {
    fun play()
    fun pause()
    fun skipToPrevious()
    fun skipToNext()
    fun seekTo(positionMs: Long)
    fun skipToQueueItem(queueItemId: Long)
}

/** Framework-neutral request to launch the currently selected session's explicit activity. */
fun interface PlaybackSessionLauncher {
    fun openSessionActivity(): Boolean
}

/** Process-local attachment point used by Android-created media services. */
object MediaSessionRuntimeHost {
    @Volatile
    private var sink: PlaybackSnapshotSink? = null

    @Volatile
    private var controlStateSink: PlaybackControlStateSink? = null

    @Volatile
    private var artworkSink: PlaybackArtworkSink? = null

    @Volatile
    private var sourceRuntimeStateSink: PlaybackSourceRuntimeStateSink? = null

    @Volatile
    private var transport: PlaybackTransport? = null

    @Volatile
    private var sessionLauncher: PlaybackSessionLauncher? = null

    @Synchronized
    fun attach(sink: PlaybackSnapshotSink) {
        this.sink = sink
    }

    @Synchronized
    fun detach(sink: PlaybackSnapshotSink) {
        if (this.sink === sink) this.sink = null
    }

    @Synchronized
    fun attachControlState(sink: PlaybackControlStateSink) {
        controlStateSink = sink
    }

    @Synchronized
    fun detachControlState(sink: PlaybackControlStateSink) {
        if (controlStateSink === sink) controlStateSink = null
    }

    @Synchronized
    fun attachArtwork(sink: PlaybackArtworkSink) {
        artworkSink = sink
    }

    @Synchronized
    fun detachArtwork(sink: PlaybackArtworkSink) {
        if (artworkSink === sink) artworkSink = null
    }

    @Synchronized
    fun attachSourceRuntimeState(sink: PlaybackSourceRuntimeStateSink) {
        sourceRuntimeStateSink = sink
    }

    @Synchronized
    fun detachSourceRuntimeState(sink: PlaybackSourceRuntimeStateSink) {
        if (sourceRuntimeStateSink === sink) sourceRuntimeStateSink = null
    }

    @Synchronized
    internal fun attachTransport(transport: PlaybackTransport) {
        this.transport = transport
    }

    @Synchronized
    internal fun detachTransport(transport: PlaybackTransport) {
        if (this.transport === transport) this.transport = null
    }

    @Synchronized
    internal fun attachSessionLauncher(launcher: PlaybackSessionLauncher) {
        sessionLauncher = launcher
    }

    @Synchronized
    internal fun detachSessionLauncher(launcher: PlaybackSessionLauncher) {
        if (sessionLauncher === launcher) sessionLauncher = null
    }

    internal fun forward(snapshot: PlaybackSnapshot) {
        sink?.onPlaybackSnapshot(snapshot)
    }

    internal fun forwardControlState(state: PlaybackControlState) {
        controlStateSink?.onPlaybackControlState(state)
    }

    internal fun forwardArtwork(bitmap: Bitmap?) {
        artworkSink?.onPlaybackArtwork(bitmap)
    }

    internal fun forwardSourceRuntimeState(state: PlaybackSourceRuntimeState) {
        sourceRuntimeStateSink?.onPlaybackSourceRuntimeState(state)
    }

    fun play() {
        transport?.play()
    }

    fun pause() {
        transport?.pause()
    }

    fun skipToPrevious() {
        transport?.skipToPrevious()
    }

    fun skipToNext() {
        transport?.skipToNext()
    }

    fun seekTo(positionMs: Long) {
        if (positionMs >= 0L) transport?.seekTo(positionMs)
    }

    fun skipToQueueItem(queueItemId: Long) {
        transport?.skipToQueueItem(queueItemId)
    }

    fun openSessionActivity(): Boolean =
        sessionLauncher?.openSessionActivity() == true
}
