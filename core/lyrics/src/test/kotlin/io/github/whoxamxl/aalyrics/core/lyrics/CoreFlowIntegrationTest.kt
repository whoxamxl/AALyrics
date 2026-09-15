package io.github.whoxamxl.aalyrics.core.lyrics

import io.github.whoxamxl.aalyrics.core.model.LyricsDocument
import io.github.whoxamxl.aalyrics.core.model.LyricsSyncType
import io.github.whoxamxl.aalyrics.core.model.PlainLyricLine
import io.github.whoxamxl.aalyrics.core.model.Track
import io.github.whoxamxl.aalyrics.provider.api.LyricsCandidate
import io.github.whoxamxl.aalyrics.provider.api.LyricsProvider
import io.github.whoxamxl.aalyrics.provider.api.LyricsProviderDescriptor
import io.github.whoxamxl.aalyrics.provider.api.LyricsProviderId
import io.github.whoxamxl.aalyrics.provider.api.LyricsRequest
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class CoreFlowIntegrationTest {
    @Test
    fun `lookup flows from loading through normalized candidates to ready`() = runTest {
        val track = Track(
            title = "Core Flow",
            artists = listOf("AALyrics"),
            durationMs = 210_000L,
        )
        val first = candidate("provider-a", track, "first")
        val selected = candidate("provider-b", track, "selected")
        val providerA = RecordingProvider("provider-a", listOf(first))
        val providerB = RecordingProvider("provider-b", listOf(selected))
        val selector = RecordingSelector(selected)
        val coordinator = LyricsCoordinator(
            providers = listOf(providerA, providerB),
            selector = selector,
            scope = this,
        )

        val lookup = coordinator.startLookup(track)

        val loading = assertIs<LyricsState.Loading>(coordinator.state.value)
        assertEquals(lookup, loading.lookup)

        advanceUntilIdle()

        val ready = assertIs<LyricsState.Ready>(coordinator.state.value)
        assertEquals(lookup, ready.lookup)
        assertEquals(selected.lyrics, ready.lyrics)
        assertEquals(track, providerA.requests.single().track)
        assertEquals(track, providerB.requests.single().track)
        assertEquals(track, selector.track)
        assertEquals(listOf(first, selected), selector.candidates)
    }

    private fun candidate(
        providerId: String,
        track: Track,
        text: String,
    ) = LyricsCandidate(
        providerId = LyricsProviderId(providerId),
        matchedTrack = track,
        lyrics = LyricsDocument(lines = listOf(PlainLyricLine(text))),
    )

    private class RecordingProvider(
        id: String,
        private val result: List<LyricsCandidate>,
    ) : LyricsProvider {
        override val descriptor = LyricsProviderDescriptor(
            id = LyricsProviderId(id),
            displayName = id,
            supportedSyncTypes = LyricsSyncType.entries.toSet(),
        )

        val requests = mutableListOf<LyricsRequest>()

        override suspend fun search(request: LyricsRequest): List<LyricsCandidate> {
            requests += request
            return result
        }
    }

    private class RecordingSelector(
        private val result: LyricsCandidate,
    ) : CandidateSelector {
        lateinit var track: Track
            private set
        var candidates: List<LyricsCandidate> = emptyList()
            private set

        override fun select(
            track: Track,
            candidates: List<LyricsCandidate>,
            preferences: CandidateSelectionPreferences,
        ): LyricsCandidate {
            this.track = track
            this.candidates = candidates
            return result
        }
    }
}
