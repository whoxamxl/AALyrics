package io.github.whoxamxl.aalyrics.core.lyrics

import io.github.whoxamxl.aalyrics.core.model.LyricsDocument
import io.github.whoxamxl.aalyrics.core.model.LyricsSyncType
import io.github.whoxamxl.aalyrics.core.model.PlainLyricLine
import io.github.whoxamxl.aalyrics.core.model.Track
import io.github.whoxamxl.aalyrics.provider.api.LyricsCandidate
import io.github.whoxamxl.aalyrics.provider.api.LyricsProviderId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

class CandidateSelectorContractTest {
    private val requestedTrack = Track(
        title = "Example",
        artists = listOf("Artist"),
        album = "Album",
        durationMs = 180_000L,
    )

    private val candidate = LyricsCandidate(
        providerId = LyricsProviderId("fake"),
        matchedTrack = requestedTrack,
        lyrics = LyricsDocument(
            lines = listOf(PlainLyricLine("Hello")),
        ),
    )

    @Test
    fun `selector receives normalized input and may return one candidate`() {
        val preferences = CandidateSelectionPreferences(
            preferredSyncType = LyricsSyncType.WORD,
        )
        var observedTrack: Track? = null
        var observedCandidates: List<LyricsCandidate>? = null
        var observedPreferences: CandidateSelectionPreferences? = null

        val selector = object : CandidateSelector {
            override fun select(
                track: Track,
                candidates: List<LyricsCandidate>,
                preferences: CandidateSelectionPreferences,
            ): LyricsCandidate? {
                observedTrack = track
                observedCandidates = candidates
                observedPreferences = preferences
                return candidates.single()
            }
        }

        val selected = selector.select(
            track = requestedTrack,
            candidates = listOf(candidate),
            preferences = preferences,
        )

        assertSame(candidate, selected)
        assertEquals(requestedTrack, observedTrack)
        assertEquals(listOf(candidate), observedCandidates)
        assertEquals(preferences, observedPreferences)
    }

    @Test
    fun `selector may reject all candidates without encoding scoring policy in the port`() {
        val selector = object : CandidateSelector {
            override fun select(
                track: Track,
                candidates: List<LyricsCandidate>,
                preferences: CandidateSelectionPreferences,
            ): LyricsCandidate? = null
        }

        assertNull(
            selector.select(
                track = requestedTrack,
                candidates = listOf(candidate),
            ),
        )
    }

    @Test
    fun `selection preferences default to no timing preference`() {
        assertNull(CandidateSelectionPreferences().preferredSyncType)
    }
}
