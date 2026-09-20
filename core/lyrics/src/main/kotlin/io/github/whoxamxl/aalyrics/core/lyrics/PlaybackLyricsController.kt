package io.github.whoxamxl.aalyrics.core.lyrics

import io.github.whoxamxl.aalyrics.core.model.PlaybackSnapshot
import io.github.whoxamxl.aalyrics.core.model.PlaybackTrackIdentity

/**
 * Pure boundary between normalized playback snapshots and lyrics lookup work.
 *
 * Timeline/status updates do not restart a lookup because ownership is keyed by
 * [PlaybackSnapshot.trackIdentity] and [CandidateSelectionPreferences], not by
 * whole-snapshot equality. Android framework types remain in `:platform:media`.
 */
class PlaybackLyricsController(
    private val lookupLifecycle: LyricsLookupLifecycle,
    private val defaultPreferences: CandidateSelectionPreferences = CandidateSelectionPreferences(),
) {
    private var activeLookup: LookupOwnership? = null

    @Synchronized
    fun onPlayback(
        snapshot: PlaybackSnapshot,
        preferences: CandidateSelectionPreferences = defaultPreferences,
    ) {
        val track = snapshot.track
        val nextIdentity = snapshot.trackIdentity

        if (track == null || nextIdentity == null) {
            if (activeLookup != null) {
                activeLookup = null
                lookupLifecycle.clear()
            }
            return
        }

        val nextLookup = LookupOwnership(nextIdentity, preferences)
        if (nextLookup == activeLookup) return

        activeLookup = nextLookup
        lookupLifecycle.startLookup(
            track = track,
            preferences = preferences,
            playbackIdentity = nextIdentity,
        )
    }

    private data class LookupOwnership(
        val trackIdentity: PlaybackTrackIdentity,
        val preferences: CandidateSelectionPreferences,
    )
}
