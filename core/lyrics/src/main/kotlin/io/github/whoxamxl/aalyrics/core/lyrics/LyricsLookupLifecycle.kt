package io.github.whoxamxl.aalyrics.core.lyrics

import io.github.whoxamxl.aalyrics.core.model.PlaybackTrackIdentity
import io.github.whoxamxl.aalyrics.core.model.Track

/**
 * Narrow provider-independent lifecycle used by playback-driven lookup logic.
 *
 * Playback code may start or clear a lookup without knowing how providers are
 * queried, how candidates are ranked, or how observable lyrics state is stored.
 */
interface LyricsLookupLifecycle {
    fun startLookup(
        track: Track,
        preferences: CandidateSelectionPreferences = CandidateSelectionPreferences(),
        playbackIdentity: PlaybackTrackIdentity? = null,
    ): LyricsLookup

    fun clear()
}
