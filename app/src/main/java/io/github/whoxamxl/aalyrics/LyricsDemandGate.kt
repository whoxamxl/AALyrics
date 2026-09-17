package io.github.whoxamxl.aalyrics

import io.github.whoxamxl.aalyrics.core.model.PlaybackSnapshot

/**
 * Process-level gate between continuously observed playback and lyrics work.
 *
 * The latest normalized snapshot is always retained. It reaches the playback
 * controller only while phone-process or automotive-projection demand is active.
 */
internal class LyricsDemandGate(
    private val downstream: (PlaybackSnapshot) -> Unit,
) {
    private var latestSnapshot = PlaybackSnapshot()
    private var phoneProcessForeground = false
    private var automotiveProjectionConnected = false
    private var active = false

    @Synchronized
    fun onPlaybackSnapshot(snapshot: PlaybackSnapshot) {
        latestSnapshot = snapshot
        if (active) downstream(snapshot)
    }

    @Synchronized
    fun setPhoneProcessForeground(foreground: Boolean) {
        if (phoneProcessForeground == foreground) return
        phoneProcessForeground = foreground
        publishDemandTransition()
    }

    @Synchronized
    fun setAutomotiveProjectionConnected(connected: Boolean) {
        if (automotiveProjectionConnected == connected) return
        automotiveProjectionConnected = connected
        publishDemandTransition()
    }

    @Synchronized
    fun isActive(): Boolean = active

    private fun publishDemandTransition() {
        val next = phoneProcessForeground || automotiveProjectionConnected
        if (next == active) return

        active = next
        downstream(if (next) latestSnapshot else PlaybackSnapshot())
    }
}
