package io.github.whoxamxl.aalyrics

import io.github.whoxamxl.aalyrics.core.lyrics.CandidateSelectionPreferences
import io.github.whoxamxl.aalyrics.core.lyrics.CandidateSelector
import io.github.whoxamxl.aalyrics.core.lyrics.LyricsState
import io.github.whoxamxl.aalyrics.core.model.LyricsDocument
import io.github.whoxamxl.aalyrics.core.model.LyricsSyncType
import io.github.whoxamxl.aalyrics.core.model.PlainLyricLine
import io.github.whoxamxl.aalyrics.core.model.PlaybackSnapshot
import io.github.whoxamxl.aalyrics.core.model.PlaybackStatus
import io.github.whoxamxl.aalyrics.core.model.Track
import io.github.whoxamxl.aalyrics.provider.api.LyricsCandidate
import io.github.whoxamxl.aalyrics.provider.api.LyricsProvider
import io.github.whoxamxl.aalyrics.provider.api.LyricsProviderDescriptor
import io.github.whoxamxl.aalyrics.provider.api.LyricsProviderId
import io.github.whoxamxl.aalyrics.provider.api.LyricsRequest
import io.github.whoxamxl.aalyrics.provider.petitlyrics.PetitLyricsProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ApplicationGraphTest {
    @Test
    fun `production graph contains every provider and preserves karaoke default`() = runTest {
        val graph = createProductionApplicationGraph(this)

        assertEquals(
            listOf("lrclib", "petitlyrics", "musixmatch", "synclrc"),
            graph.providers.map { it.descriptor.id.value },
        )
        assertEquals(LyricsSyncType.WORD, graph.selectionPreferences.preferredSyncType)
        assertSame(graph.coordinator.state, graph.lyricsState)
        val petitLyrics = graph.providers.filterIsInstance<PetitLyricsProvider>().single()
        val hasCompletePetitLyricsConfig = listOf(
            BuildConfig.PETITLYRICS_USER_ID,
            BuildConfig.PETITLYRICS_APP_NAME,
            BuildConfig.PETITLYRICS_PKG_NAME,
            BuildConfig.PETITLYRICS_CLIENT_APP_ID,
        ).all { it.isNotBlank() }
        assertEquals(hasCompletePetitLyricsConfig, petitLyrics.isConfigured)
    }

    @Test
    fun `controller uses application preference for provider request and selection`() = runTest {
        val provider = RecordingProvider()
        val selector = RecordingSelector()
        val preferences = CandidateSelectionPreferences(preferredSyncType = LyricsSyncType.WORD)
        val graph = ApplicationGraph(
            providers = listOf(provider),
            selector = selector,
            applicationScope = this,
            selectionPreferences = preferences,
        )
        val track = Track(title = "Song", artists = listOf("Artist"))

        graph.onEligiblePlayback(PlaybackSnapshot(track = track))
        advanceUntilIdle()
        assertEquals(emptyList(), provider.requests)
        assertEquals(track, graph.playbackState.value.track)

        graph.lyricsDemandGate.setPhoneProcessForeground(true)
        advanceUntilIdle()
        graph.onEligiblePlayback(
            PlaybackSnapshot(
                track = track.copy(durationMs = 200_000L),
                status = PlaybackStatus.PLAYING,
                positionMs = 42_000L,
            ),
        )
        advanceUntilIdle()

        assertEquals(listOf(LyricsSyncType.WORD), provider.requests.map { it.preferredSyncType })
        assertEquals(listOf(preferences), selector.preferences)
    }

    @Test
    fun `resolved lyrics survive demand loss and same-track resume does not refetch`() = runTest {
        val provider = SuccessfulProvider()
        val graph = ApplicationGraph(
            providers = listOf(provider),
            selector = object : CandidateSelector {
                override fun select(
                    track: Track,
                    candidates: List<LyricsCandidate>,
                    preferences: CandidateSelectionPreferences,
                ): LyricsCandidate? = candidates.firstOrNull()
            },
            applicationScope = this,
            selectionPreferences = CandidateSelectionPreferences(),
        )
        val snapshot = PlaybackSnapshot(
            track = Track(title = "Song", artists = listOf("Artist")),
        )

        graph.onEligiblePlayback(snapshot)
        graph.lyricsDemandGate.setPhoneProcessForeground(true)
        advanceUntilIdle()

        assertIs<LyricsState.Ready>(graph.lyricsState.value)
        assertEquals(1, provider.requests.size)

        graph.lyricsDemandGate.setPhoneProcessForeground(false)
        advanceUntilIdle()

        assertIs<LyricsState.Ready>(graph.lyricsState.value)
        assertEquals(1, provider.requests.size)

        graph.lyricsDemandGate.setPhoneProcessForeground(true)
        advanceUntilIdle()

        assertIs<LyricsState.Ready>(graph.lyricsState.value)
        assertEquals(1, provider.requests.size)
    }

    @Test
    fun `track change while demand is off starts a fresh lookup on resume`() = runTest {
        val provider = SuccessfulProvider()
        val graph = ApplicationGraph(
            providers = listOf(provider),
            selector = object : CandidateSelector {
                override fun select(
                    track: Track,
                    candidates: List<LyricsCandidate>,
                    preferences: CandidateSelectionPreferences,
                ): LyricsCandidate? = candidates.firstOrNull()
            },
            applicationScope = this,
            selectionPreferences = CandidateSelectionPreferences(),
        )
        val first = PlaybackSnapshot(
            track = Track(title = "First", artists = listOf("Artist")),
        )
        val second = PlaybackSnapshot(
            track = Track(title = "Second", artists = listOf("Artist")),
        )

        graph.onEligiblePlayback(first)
        graph.lyricsDemandGate.setPhoneProcessForeground(true)
        advanceUntilIdle()
        assertEquals(1, provider.requests.size)

        graph.lyricsDemandGate.setPhoneProcessForeground(false)
        graph.onEligiblePlayback(second)
        advanceUntilIdle()
        assertEquals(1, provider.requests.size)

        graph.lyricsDemandGate.setPhoneProcessForeground(true)
        advanceUntilIdle()

        val ready = assertIs<LyricsState.Ready>(graph.lyricsState.value)
        assertEquals("Second", ready.lookup.track.title)
        assertEquals(2, provider.requests.size)
    }

    @Test
    fun `removing final demand source clears state and cancels provider work`() = runTest {
        val provider = BlockingProvider()
        val graph = ApplicationGraph(
            providers = listOf(provider),
            selector = RecordingSelector(),
            applicationScope = this,
            selectionPreferences = CandidateSelectionPreferences(),
        )
        graph.onEligiblePlayback(
            PlaybackSnapshot(track = Track(title = "Song", artists = listOf("Artist"))),
        )

        graph.lyricsDemandGate.setPhoneProcessForeground(true)
        runCurrent()

        assertIs<LyricsState.Loading>(graph.lyricsState.value)
        assertTrue(provider.started)

        graph.lyricsDemandGate.setAutomotiveProjectionConnected(true)
        graph.lyricsDemandGate.setPhoneProcessForeground(false)
        runCurrent()
        assertIs<LyricsState.Loading>(graph.lyricsState.value)

        graph.lyricsDemandGate.setAutomotiveProjectionConnected(false)
        advanceUntilIdle()

        assertIs<LyricsState.Idle>(graph.lyricsState.value)
        assertTrue(provider.cancelled)
    }

    private fun ApplicationGraph.onEligiblePlayback(snapshot: PlaybackSnapshot) {
        playbackSnapshotSink.onPlaybackSnapshot(snapshot)
        lyricsDemandGate.onPlaybackSnapshot(
            snapshot = snapshot,
            sourceEligible = true,
        )
    }

    private class RecordingProvider : LyricsProvider {
        override val descriptor = LyricsProviderDescriptor(
            id = LyricsProviderId("recording"),
            displayName = "Recording",
            supportedSyncTypes = setOf(LyricsSyncType.LINE),
        )
        val requests = mutableListOf<LyricsRequest>()

        override suspend fun search(request: LyricsRequest): List<LyricsCandidate> {
            requests += request
            return emptyList()
        }
    }

    private class RecordingSelector : CandidateSelector {
        val preferences = mutableListOf<CandidateSelectionPreferences>()

        override fun select(
            track: Track,
            candidates: List<LyricsCandidate>,
            preferences: CandidateSelectionPreferences,
        ): LyricsCandidate? {
            this.preferences += preferences
            return null
        }
    }

    private class SuccessfulProvider : LyricsProvider {
        override val descriptor = LyricsProviderDescriptor(
            id = LyricsProviderId("successful"),
            displayName = "Successful",
            supportedSyncTypes = setOf(LyricsSyncType.PLAIN),
        )
        val requests = mutableListOf<LyricsRequest>()

        override suspend fun search(request: LyricsRequest): List<LyricsCandidate> {
            requests += request
            return listOf(
                LyricsCandidate(
                    providerId = descriptor.id,
                    matchedTrack = request.track,
                    lyrics = LyricsDocument(
                        lines = listOf(PlainLyricLine("Retained lyrics")),
                    ),
                ),
            )
        }
    }

    private class BlockingProvider : LyricsProvider {
        override val descriptor = LyricsProviderDescriptor(
            id = LyricsProviderId("blocking"),
            displayName = "Blocking",
            supportedSyncTypes = setOf(LyricsSyncType.LINE),
        )
        var started = false
        var cancelled = false

        override suspend fun search(request: LyricsRequest): List<LyricsCandidate> {
            started = true
            try {
                awaitCancellation()
            } finally {
                cancelled = true
            }
        }
    }
}
