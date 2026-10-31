package io.github.whoxamxl.aalyrics.core.lyrics

import io.github.whoxamxl.aalyrics.core.model.LyricsDocument
import io.github.whoxamxl.aalyrics.core.model.LyricsSyncType
import io.github.whoxamxl.aalyrics.core.model.PlainLyricLine
import io.github.whoxamxl.aalyrics.core.model.PlaybackSnapshot
import io.github.whoxamxl.aalyrics.core.model.Track
import io.github.whoxamxl.aalyrics.provider.api.LyricsCandidate
import io.github.whoxamxl.aalyrics.provider.api.LyricsProvider
import io.github.whoxamxl.aalyrics.provider.api.LyricsProviderDescriptor
import io.github.whoxamxl.aalyrics.provider.api.LyricsProviderId
import io.github.whoxamxl.aalyrics.provider.api.LyricsRequest
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class LyricsCacheLookupTest {
    private val track = Track("A", listOf("Artist"))
    private val document = LyricsDocument(listOf(PlainLyricLine("Lyrics A")))
    private val preferences = CandidateSelectionPreferences()
    private val selector = object : CandidateSelector {
        override fun select(
            track: Track,
            candidates: List<LyricsCandidate>,
            preferences: CandidateSelectionPreferences,
        ) = candidates.firstOrNull()
    }

    @Test
    fun `A to B to A offline reuses saved winner under a fresh lookup id`() = runTest {
        val cache = MemoryCache()
        val provider = Provider()
        val coordinator = LyricsCoordinator(listOf(provider), selector, this, cache = cache)
        val controller = PlaybackLyricsController(coordinator)
        controller.onPlayback(PlaybackSnapshot(track = track))
        advanceUntilIdle()
        val first = assertIs<LyricsState.Ready>(coordinator.state.value)
        assertEquals(document, cache.values.values.single())

        provider.offline = true
        controller.onPlayback(PlaybackSnapshot(track = track.copy(title = "B")))
        advanceUntilIdle()
        assertIs<LyricsState.Failed>(coordinator.state.value)
        val callsBeforeReturn = provider.calls
        controller.onPlayback(PlaybackSnapshot(track = track))
        advanceUntilIdle()
        val returned = assertIs<LyricsState.Ready>(coordinator.state.value)
        assertEquals(first.lyrics, returned.lyrics)
        assertNotEquals(first.lookup.id, returned.lookup.id)
        assertEquals(callsBeforeReturn, provider.calls)
        assertTrue(coordinator.diagnostics.value.cacheHit)
        assertEquals(0, coordinator.diagnostics.value.attemptCount)
        assertEquals(emptyList(), coordinator.diagnostics.value.providerFailures)

        controller.suspendForNoDemand()
        controller.onPlayback(PlaybackSnapshot(track = track))
        advanceUntilIdle()
        assertEquals(returned, coordinator.state.value)
        assertEquals(callsBeforeReturn, provider.calls)
    }

    @Test
    fun `different preferences and provider sets cannot reuse a cached winner`() = runTest {
        val cache = MemoryCache()
        val first = Provider()
        val coordinator = LyricsCoordinator(listOf(first), selector, this, cache = cache)
        coordinator.startLookup(track, preferences)
        advanceUntilIdle()
        coordinator.startLookup(track, CandidateSelectionPreferences(LyricsSyncType.WORD))
        advanceUntilIdle()
        assertEquals(2, first.calls)
        val other = Provider("other")
        LyricsCoordinator(listOf(other), selector, this, cache = cache).startLookup(track, preferences)
        advanceUntilIdle()
        assertEquals(1, other.calls)
        assertEquals(3, cache.values.size)
    }

    @Test
    fun `cache read and write errors do not hide successful provider results`() = runTest {
        var writes = 0
        val cache = object : LyricsCache {
            override suspend fun get(key: LyricsCacheKey): LyricsDocument? = error("unreadable")
            override suspend fun put(key: LyricsCacheKey, lyrics: LyricsDocument) {
                writes++
                error("disk full")
            }
        }
        val provider = Provider()
        val coordinator = LyricsCoordinator(listOf(provider), selector, this, cache = cache)
        coordinator.startLookup(track)
        advanceUntilIdle()
        assertEquals(document, assertIs<LyricsState.Ready>(coordinator.state.value).lyrics)
        assertEquals(1, writes)
        assertEquals(1, provider.calls)
        assertFalse(coordinator.diagnostics.value.cacheHit)
    }

    @Test
    fun `failed and not-found lookups never become negative cache entries`() = runTest {
        val cache = MemoryCache()
        val provider = Provider().apply { empty = true }
        val coordinator = LyricsCoordinator(listOf(provider), selector, this, cache = cache)
        coordinator.startLookup(track)
        advanceUntilIdle()
        assertIs<LyricsState.NotFound>(coordinator.state.value)
        provider.offline = true
        coordinator.startLookup(track)
        advanceUntilIdle()
        assertIs<LyricsState.Failed>(coordinator.state.value)
        assertTrue(cache.values.isEmpty())
        provider.offline = false
        provider.empty = false
        coordinator.startLookup(track)
        advanceUntilIdle()
        assertIs<LyricsState.Ready>(coordinator.state.value)
    }

    @Test
    fun `partial provider failure saves usable canonical lyrics without caching failure diagnostics`() = runTest {
        val cache = MemoryCache()
        val successful = Provider()
        val failed = Provider("failed").apply { offline = true }
        val coordinator = LyricsCoordinator(listOf(successful, failed), selector, this, cache = cache)
        coordinator.startLookup(track)
        advanceUntilIdle()
        assertIs<LyricsState.Degraded>(coordinator.state.value)
        coordinator.startLookup(track)
        advanceUntilIdle()
        assertIs<LyricsState.Ready>(coordinator.state.value)
        assertTrue(coordinator.diagnostics.value.cacheHit)
        assertEquals(1, successful.calls)
        assertEquals(1, failed.calls)
    }

    @Test
    fun `result is persisted before ready publication`() = runTest {
        val written = CompletableDeferred<Unit>()
        val cache = object : LyricsCache {
            override suspend fun get(key: LyricsCacheKey): LyricsDocument? = null
            override suspend fun put(key: LyricsCacheKey, lyrics: LyricsDocument) = written.await()
        }
        val coordinator = LyricsCoordinator(listOf(Provider()), selector, this, cache = cache)
        coordinator.startLookup(track)
        runCurrent()
        assertIs<LyricsState.Loading>(coordinator.state.value)
        written.complete(Unit)
        advanceUntilIdle()
        assertIs<LyricsState.Ready>(coordinator.state.value)
    }

    @Test
    fun `late cache hit from superseded track cannot replace current lyrics`() = runTest {
        val release = CompletableDeferred<Unit>()
        val cache = object : LyricsCache {
            override suspend fun get(key: LyricsCacheKey): LyricsDocument? {
                if (key.track == track) withContext(NonCancellable) { release.await() }
                return LyricsDocument(listOf(PlainLyricLine(key.track.title)))
            }
            override suspend fun put(key: LyricsCacheKey, lyrics: LyricsDocument) = Unit
        }
        val provider = Provider()
        val coordinator = LyricsCoordinator(listOf(provider), selector, this, cache = cache)
        coordinator.startLookup(track)
        runCurrent()
        val latest = coordinator.startLookup(track.copy(title = "B"))
        runCurrent()
        release.complete(Unit)
        advanceUntilIdle()
        assertEquals(latest, assertIs<LyricsState.Ready>(coordinator.state.value).lookup)
        assertEquals(latest.id, coordinator.diagnostics.value.lookupId)
        assertEquals(0, provider.calls)
    }

    @Test
    fun `demand loss during a non-cooperative cache miss cannot start network work`() = runTest {
        val release = CompletableDeferred<Unit>()
        val cache = object : LyricsCache {
            override suspend fun get(key: LyricsCacheKey): LyricsDocument? {
                withContext(NonCancellable) { release.await() }
                return null
            }
            override suspend fun put(key: LyricsCacheKey, lyrics: LyricsDocument) = Unit
        }
        val provider = Provider()
        val coordinator = LyricsCoordinator(listOf(provider), selector, this, cache = cache)
        val controller = PlaybackLyricsController(coordinator)
        controller.onPlayback(PlaybackSnapshot(track = track))
        runCurrent()
        controller.suspendForNoDemand()
        release.complete(Unit)
        advanceUntilIdle()
        assertEquals(LyricsState.Idle, coordinator.state.value)
        assertEquals(0, provider.calls)
        controller.onPlayback(PlaybackSnapshot(track = track))
        advanceUntilIdle()
        assertIs<LyricsState.Ready>(coordinator.state.value)
    }

    @Test
    fun `late cache write completion after clear cannot resurrect lyrics`() = runTest {
        val release = CompletableDeferred<Unit>()
        var writeStarted = false
        val cache = object : LyricsCache {
            override suspend fun get(key: LyricsCacheKey): LyricsDocument? = null
            override suspend fun put(key: LyricsCacheKey, lyrics: LyricsDocument) {
                writeStarted = true
                withContext(NonCancellable) { release.await() }
            }
        }
        val coordinator = LyricsCoordinator(listOf(Provider()), selector, this, cache = cache)
        coordinator.startLookup(track)
        runCurrent()
        assertTrue(writeStarted)
        coordinator.clear()
        release.complete(Unit)
        advanceUntilIdle()
        assertEquals(LyricsState.Idle, coordinator.state.value)
        assertEquals(LyricsLookupDiagnostics(), coordinator.diagnostics.value)
    }

    private class MemoryCache : LyricsCache {
        val values = mutableMapOf<LyricsCacheKey, LyricsDocument>()
        override suspend fun get(key: LyricsCacheKey) = values[key]
        override suspend fun put(key: LyricsCacheKey, lyrics: LyricsDocument) { values[key] = lyrics }
    }

    private inner class Provider(id: String = "test") : LyricsProvider {
        override val descriptor = LyricsProviderDescriptor(LyricsProviderId(id), id, LyricsSyncType.entries.toSet())
        var calls = 0
        var offline = false
        var empty = false
        override suspend fun search(request: LyricsRequest): List<LyricsCandidate> {
            calls++
            check(!offline) { "offline" }
            return if (empty) emptyList() else listOf(LyricsCandidate(descriptor.id, request.track, document))
        }
    }
}
