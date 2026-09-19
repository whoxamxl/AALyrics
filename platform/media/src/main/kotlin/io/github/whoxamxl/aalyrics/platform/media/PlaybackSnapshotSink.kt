package io.github.whoxamxl.aalyrics.platform.media

import io.github.whoxamxl.aalyrics.core.model.PlaybackSnapshot

/** Narrow platform/application boundary for normalized playback updates. */
fun interface PlaybackSnapshotSink {
    fun onPlaybackSnapshot(snapshot: PlaybackSnapshot)
}

/** Framework-neutral transport commands for the currently selected media session. */
interface PlaybackTransport {
    fun play()
    fun pause()
    fun skipToPrevious()
    fun skipToNext()
    fun seekTo(positionMs: Long)
}

/** Process-local attachment point used by Android-created media services. */
object MediaSessionRuntimeHost {
    @Volatile
    private var sink: PlaybackSnapshotSink? = null

    @Volatile
    private var transport: PlaybackTransport? = null

    @Synchronized
    fun attach(sink: PlaybackSnapshotSink) {
        this.sink = sink
    }

    @Synchronized
    fun detach(sink: PlaybackSnapshotSink) {
        if (this.sink === sink) this.sink = null
    }

    @Synchronized
    internal fun attachTransport(transport: PlaybackTransport) {
        this.transport = transport
    }

    @Synchronized
    internal fun detachTransport(transport: PlaybackTransport) {
        if (this.transport === transport) this.transport = null
    }

    internal fun forward(snapshot: PlaybackSnapshot) {
        sink?.onPlaybackSnapshot(snapshot)
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
}
