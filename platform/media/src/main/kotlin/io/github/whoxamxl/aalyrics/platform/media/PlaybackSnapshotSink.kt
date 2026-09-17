package io.github.whoxamxl.aalyrics.platform.media

import io.github.whoxamxl.aalyrics.core.model.PlaybackSnapshot

/** Narrow platform/application boundary for normalized playback updates. */
fun interface PlaybackSnapshotSink {
    fun onPlaybackSnapshot(snapshot: PlaybackSnapshot)
}

/** Process-local attachment point used by the Android-created listener service. */
object MediaSessionRuntimeHost {
    @Volatile
    private var sink: PlaybackSnapshotSink? = null

    @Synchronized
    fun attach(sink: PlaybackSnapshotSink) {
        this.sink = sink
    }

    @Synchronized
    fun detach(sink: PlaybackSnapshotSink) {
        if (this.sink === sink) this.sink = null
    }

    internal fun forward(snapshot: PlaybackSnapshot) {
        sink?.onPlaybackSnapshot(snapshot)
    }
}
