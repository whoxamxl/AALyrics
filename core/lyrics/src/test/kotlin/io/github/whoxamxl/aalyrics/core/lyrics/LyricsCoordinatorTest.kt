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
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class LyricsCoordinatorTest {
    private val track = Track(
        title = "Example",
        artists = listOf("Artist"),
        durationMs = 180_000L,
    )

    @Test
    fun `combines provider candidates and delegates winner selection once`() = runTest {
        val first = candidate("provider-a", "First")
        val second = candidate("provider-b", "Second")
        val providerA = FakeProvider("provider-a") { listOf(first) }
        val providerB = FakeProvider("provider-b") { listOf(second) }
        val selector = RecordingSelector { _, candidates, _ -> candidates.last() }
        val coordinator = LyricsCoordinator(
            providers = listOf(providerA, providerB),
            selector = selector,
            scope = this,
        )

        coordinator.startLookup(
            track = track,
            preferences = CandidateSelectionPreferences(LyricsSyncType.WORD),
        )
        advanceUntilIdle()

        val ready = assertIs<LyricsState.Ready>(coordinator.state.value)
        assertEquals(second.lyrics, ready.lyrics)
        assertEquals(1, selector.callCount)
        assertEquals(listOf(first, second), selector.lastCandidates)
        assertEquals(LyricsSyncType.WORD, selector.lastPreferences?.preferredSyncType)
        assertEquals(LyricsSyncType.WORD, providerA.requests.single().preferredSyncType)
        assertEquals(LyricsSyncType.WORD, providerB.requests.single().preferredSyncType)
    }

    @Test
    fun `one provider failure does not discard a healthy winner`() = runTest {
        val winner = candidate("healthy", "Winner")
        val failing = FakeProvider("failing") { error("boom") }
        val healthy = FakeProvider("healthy") { listOf(winner) }
        val selector = RecordingSelector { _, candidates, _ -> candidates.firstOrNull() }
        val coordinator = LyricsCoordinator(
            providers = listOf(failing, healthy),
            selector = selector,
            scope = this,
        )

        coordinator.startLookup(track)
        advanceUntilIdle()

        val degraded = assertIs<LyricsState.Degraded>(coordinator.state.value)
        assertEquals(winner.lyrics, degraded.lyrics)
        assertEquals(1, degraded.failedAttempts)
        assertEquals(1, selector.callCount)
    }

    @Test
    fun `no winner with successful provider attempts is not found`() = runTest {
        val first = FakeProvider("first") { emptyList() }
        val second = FakeProvider("second") { emptyList() }
        val selector = RecordingSelector { _, _, _ -> null }
        val coordinator = LyricsCoordinator(
            providers = listOf(first, second),
            selector = selector,
            scope = this,
        )

        coordinator.startLookup(track)
        advanceUntilIdle()

        assertIs<LyricsState.NotFound>(coordinator.state.value)
    }

    @Test
    fun `no winner with provider failure is terminal failure`() = runTest {
        val failing = FakeProvider("failing") { error("boom") }
        val empty = FakeProvider("empty") { emptyList() }
        val selector = RecordingSelector { _, _, _ -> null }
        val coordinator = LyricsCoordinator(
            providers = listOf(failing, empty),
            selector = selector,
            scope = this,
        )

        coordinator.startLookup(track)
        advanceUntilIdle()

        val failed = assertIs<LyricsState.Failed>(coordinator.state.value)
        assertEquals(1, failed.failedAttempts)
    }

    @Test
    fun `providers are started concurrently`() = runTest {
        val firstStarted = CompletableDeferred<Unit>()
        val secondStarted = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val first = FakeProvider("first") {
            firstStarted.complete(Unit)
            release.await()
            emptyList()
        }
        val second = FakeProvider("second") {
            secondStarted.complete(Unit)
            release.await()
            emptyList()
        }
        val coordinator = LyricsCoordinator(
            providers = listOf(first, second),
            selector = RecordingSelector { _, _, _ -> null },
            scope = this,
        )

        coordinator.startLookup(track)
        runCurrent()

        assertTrue(firstStarted.isCompleted)
        assertTrue(secondStarted.isCompleted)

        release.complete(Unit)
        advanceUntilIdle()
        assertIs<LyricsState.NotFound>(coordinator.state.value)
    }

    @Test
    fun `new lookup cancels previous work and owns final state`() = runTest {
        val firstStarted = CompletableDeferred<Unit>()
        val firstCancelled = CompletableDeferred<Unit>()
        val secondTrack = track.copy(title = "Second")
        val secondCandidate = candidate("provider", "Second lyrics", matchedTrack = secondTrack)
        val provider = FakeProvider("provider") { request ->
            if (request.track.title == "Example") {
                firstStarted.complete(Unit)
                try {
                    awaitCancellation()
                } finally {
                    firstCancelled.complete(Unit)
                }
            } else {
                listOf(secondCandidate)
            }
        }
        val coordinator = LyricsCoordinator(
            providers = listOf(provider),
            selector = RecordingSelector { _, candidates, _ -> candidates.firstOrNull() },
            scope = this,
        )

        val firstLookup = coordinator.startLookup(track)
        runCurrent()
        assertTrue(firstStarted.isCompleted)

        val secondLookup = coordinator.startLookup(secondTrack)
        advanceUntilIdle()

        assertTrue(firstCancelled.isCompleted)
        assertNotEquals(firstLookup.id, secondLookup.id)
        val ready = assertIs<LyricsState.Ready>(coordinator.state.value)
        assertEquals(secondLookup, ready.lookup)
        assertEquals(secondCandidate.lyrics, ready.lyrics)
    }

    @Test
    fun `suspend keeps resolved lyrics reusable in memory`() = runTest {
        val winner = candidate("provider", "Retained")
        val provider = FakeProvider("provider") { listOf(winner) }
        val coordinator = LyricsCoordinator(
            providers = listOf(provider),
            selector = RecordingSelector { _, candidates, _ -> candidates.firstOrNull() },
            scope = this,
        )

        coordinator.startLookup(track)
        advanceUntilIdle()
        val beforeSuspend = assertIs<LyricsState.Ready>(coordinator.state.value)

        val reusable = coordinator.suspendLookup()

        assertTrue(reusable)
        val afterSuspend = assertIs<LyricsState.Ready>(coordinator.state.value)
        assertEquals(beforeSuspend, afterSuspend)
    }

    @Test
    fun `suspend cancels loading work and marks ownership non-reusable`() = runTest {
        val started = CompletableDeferred<Unit>()
        val cancelled = CompletableDeferred<Unit>()
        val provider = FakeProvider("provider") {
            started.complete(Unit)
            try {
                awaitCancellation()
            } finally {
                cancelled.complete(Unit)
            }
        }
        val coordinator = LyricsCoordinator(
            providers = listOf(provider),
            selector = RecordingSelector { _, _, _ -> null },
            scope = this,
        )

        coordinator.startLookup(track)
        runCurrent()
        assertTrue(started.isCompleted)

        val reusable = coordinator.suspendLookup()
        advanceUntilIdle()

        assertTrue(cancelled.isCompleted)
        assertEquals(false, reusable)
        assertEquals(LyricsState.Idle, coordinator.state.value)
    }

    @Test
    fun `clear cancels active work and returns to idle`() = runTest {
        val started = CompletableDeferred<Unit>()
        val cancelled = CompletableDeferred<Unit>()
        val provider = FakeProvider("provider") {
            started.complete(Unit)
            try {
                awaitCancellation()
            } finally {
                cancelled.complete(Unit)
            }
        }
        val coordinator = LyricsCoordinator(
            providers = listOf(provider),
            selector = RecordingSelector { _, _, _ -> null },
            scope = this,
        )

        coordinator.startLookup(track)
        runCurrent()
        assertTrue(started.isCompleted)

        coordinator.clear()
        advanceUntilIdle()

        assertTrue(cancelled.isCompleted)
        assertEquals(LyricsState.Idle, coordinator.state.value)
    }

    @Test
    fun `refreshing the same track creates a new lookup identity`() = runTest {
        val provider = FakeProvider("provider") { emptyList() }
        val coordinator = LyricsCoordinator(
            providers = listOf(provider),
            selector = RecordingSelector { _, _, _ -> null },
            scope = this,
        )

        val first = coordinator.startLookup(track)
        advanceUntilIdle()
        val second = coordinator.startLookup(track)
        advanceUntilIdle()

        assertNotEquals(first.id, second.id)
        assertEquals(second, assertIs<LyricsState.NotFound>(coordinator.state.value).lookup)
    }

    private fun candidate(
        providerId: String,
        text: String,
        matchedTrack: Track = track,
    ): LyricsCandidate = LyricsCandidate(
        providerId = LyricsProviderId(providerId),
        matchedTrack = matchedTrack,
        lyrics = LyricsDocument(lines = listOf(PlainLyricLine(text))),
    )

    private class FakeProvider(
        id: String,
        private val searchBlock: suspend (LyricsRequest) -> List<LyricsCandidate>,
    ) : LyricsProvider {
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
        private val selection: (Track, List<LyricsCandidate>, CandidateSelectionPreferences) -> LyricsCandidate?,
    ) : CandidateSelector {
        var callCount: Int = 0
            private set
        var lastCandidates: List<LyricsCandidate> = emptyList()
            private set
        var lastPreferences: CandidateSelectionPreferences? = null
            private set

        override fun select(
            track: Track,
            candidates: List<LyricsCandidate>,
            preferences: CandidateSelectionPreferences,
        ): LyricsCandidate? {
            callCount += 1
            lastCandidates = candidates
            lastPreferences = preferences
            return selection(track, candidates, preferences)
        }
    }
}
