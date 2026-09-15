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
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

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

    @Test
    fun `provider completion order does not become winner selection order`() = runTest {
        val track = Track(
            title = "Completion Order",
            artists = listOf("AALyrics"),
            durationMs = 180_000L,
        )
        val slowCandidate = candidate("slow", track, "slow winner")
        val fastCandidate = candidate("fast", track, "fast result")
        val releaseSlow = CompletableDeferred<Unit>()
        val slowStarted = CompletableDeferred<Unit>()
        val fastCompleted = CompletableDeferred<Unit>()

        val slowProvider = RecordingProvider("slow") {
            slowStarted.complete(Unit)
            releaseSlow.await()
            listOf(slowCandidate)
        }
        val fastProvider = RecordingProvider("fast") {
            fastCompleted.complete(Unit)
            listOf(fastCandidate)
        }
        val selector = RecordingSelector(slowCandidate)
        val coordinator = LyricsCoordinator(
            providers = listOf(slowProvider, fastProvider),
            selector = selector,
            scope = this,
        )

        coordinator.startLookup(track)
        runCurrent()

        assertTrue(slowStarted.isCompleted)
        assertTrue(fastCompleted.isCompleted)
        assertEquals(0, selector.callCount)
        assertIs<LyricsState.Loading>(coordinator.state.value)

        releaseSlow.complete(Unit)
        advanceUntilIdle()

        val ready = assertIs<LyricsState.Ready>(coordinator.state.value)
        assertEquals(slowCandidate.lyrics, ready.lyrics)
        assertEquals(1, selector.callCount)
        assertEquals(setOf(slowCandidate, fastCandidate), selector.candidates.toSet())
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
        private val searchBlock: suspend (LyricsRequest) -> List<LyricsCandidate>,
    ) : LyricsProvider {
        constructor(
            id: String,
            result: List<LyricsCandidate>,
        ) : this(id, { result })

        override val descriptor = LyricsProviderDescriptor(
            id = LyricsProviderId(id),
            displayName = id,
            supportedSyncTypes = LyricsSyncType.entries.toSet(),
        )

        val requests = mutableListOf<LyricsRequest>()

        override suspend fun search(request: LyricsRequest): List<LyricsCandidate> {
            requests += request
            return searchBlock(request)
        }
    }

    private class RecordingSelector(
        private val result: LyricsCandidate,
    ) : CandidateSelector {
        lateinit var track: Track
            private set
        var candidates: List<LyricsCandidate> = emptyList()
            private set
        var callCount: Int = 0
            private set

        override fun select(
            track: Track,
            candidates: List<LyricsCandidate>,
            preferences: CandidateSelectionPreferences,
        ): LyricsCandidate {
            callCount += 1
            this.track = track
            this.candidates = candidates
            return result
        }
    }
}
