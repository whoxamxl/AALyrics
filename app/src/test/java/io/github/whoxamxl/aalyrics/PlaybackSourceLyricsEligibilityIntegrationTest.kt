package io.github.whoxamxl.aalyrics

import io.github.whoxamxl.aalyrics.core.lyrics.CandidateSelectionPreferences
import io.github.whoxamxl.aalyrics.core.lyrics.LyricsLookup
import io.github.whoxamxl.aalyrics.core.lyrics.LyricsLookupId
import io.github.whoxamxl.aalyrics.core.lyrics.LyricsLookupLifecycle
import io.github.whoxamxl.aalyrics.core.lyrics.PlaybackLyricsController
import io.github.whoxamxl.aalyrics.core.model.PlaybackSnapshot
import io.github.whoxamxl.aalyrics.core.model.PlaybackSource
import io.github.whoxamxl.aalyrics.core.model.PlaybackTrackIdentity
import io.github.whoxamxl.aalyrics.core.model.Track
import kotlin.test.Test
import kotlin.test.assertEquals

class PlaybackSourceLyricsEligibilityIntegrationTest {
    @Test
    fun `blocked source clears existing lookup and never starts provider work`() {
        val lifecycle = RecordingLifecycle()
        val controller = PlaybackLyricsController(lifecycle)
        val gate = LyricsDemandGate(
            downstream = controller::onPlayback,
            onDemandInactive = controller::suspendForNoDemand,
        )
        gate.setPhoneProcessForeground(true)

        gate.onPlaybackSnapshot(
            snapshot(packageName = "com.audio.player", title = "Allowed"),
            sourceEligible = true,
        )
        gate.onPlaybackSnapshot(
            snapshot(packageName = "com.video.player", title = "Blocked"),
            sourceEligible = false,
        )

        assertEquals(listOf("Allowed"), lifecycle.startedTracks.map { it.title })
        assertEquals(1, lifecycle.clearCount)
    }

    @Test
    fun `reenabling current blocked source starts exactly one lookup for retained snapshot`() {
        val lifecycle = RecordingLifecycle()
        val controller = PlaybackLyricsController(lifecycle)
        val gate = LyricsDemandGate(
            downstream = controller::onPlayback,
            onDemandInactive = controller::suspendForNoDemand,
        )
        gate.setPhoneProcessForeground(true)
        gate.onPlaybackSnapshot(
            snapshot(packageName = "com.unknown.player", title = "Current"),
            sourceEligible = false,
        )

        gate.setSourceEligible(true)
        gate.setSourceEligible(true)

        assertEquals(listOf("Current"), lifecycle.startedTracks.map { it.title })
        assertEquals(0, lifecycle.clearCount)
    }

    private fun snapshot(
        packageName: String,
        title: String,
    ) = PlaybackSnapshot(
        track = Track(title = title, artists = listOf("Artist")),
        source = PlaybackSource(id = packageName, mediaId = title.lowercase()),
    )

    private class RecordingLifecycle : LyricsLookupLifecycle {
        val startedTracks = mutableListOf<Track>()
        var clearCount = 0
        private var nextId = 0L

        override fun startLookup(
            track: Track,
            preferences: CandidateSelectionPreferences,
            playbackIdentity: PlaybackTrackIdentity?,
        ): LyricsLookup {
            startedTracks += track
            return LyricsLookup(
                id = LyricsLookupId(nextId++),
                track = track,
                playbackIdentity = requireNotNull(playbackIdentity),
            )
        }

        override fun suspendLookup(): Boolean = false

        override fun clear() {
            clearCount += 1
        }
    }
}
