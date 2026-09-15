package io.github.whoxamxl.aalyrics.core.lyrics

import io.github.whoxamxl.aalyrics.core.model.PlaybackSnapshot
import io.github.whoxamxl.aalyrics.core.model.PlaybackTrackIdentity

/**
 * Pure boundary between normalized playback snapshots and lyrics lookup work.
 *
 * Timeline/status updates do not restart a lookup because ownership is keyed by
 * [PlaybackSnapshot.trackIdentity], not by whole-snapshot equality. Android
 * framework types remain in `:platform:media`.
 */
class PlaybackLyricsController(
    private val lookupLifecycle: LyricsLookupLifecycle,
) {
    private var activeIdentity: PlaybackTrackIdentity? = null

    @Synchronized
    fun onPlayback(snapshot: PlaybackSnapshot) {
        val track = snapshot.track
        val nextIdentity = snapshot.trackIdentity

        if (track == null || nextIdentity == null) {
            if (activeIdentity != null) {
                activeIdentity = null
                lookupLifecycle.clear()
            }
            return
        }

        if (nextIdentity == activeIdentity) return

        activeIdentity = nextIdentity
        lookupLifecycle.startLookup(track)
    }
}
