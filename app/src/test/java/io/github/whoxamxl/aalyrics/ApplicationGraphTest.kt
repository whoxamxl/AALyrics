package io.github.whoxamxl.aalyrics

import io.github.whoxamxl.aalyrics.core.lyrics.CandidateSelectionPreferences
import io.github.whoxamxl.aalyrics.core.lyrics.CandidateSelector
import io.github.whoxamxl.aalyrics.core.model.LyricsSyncType
import io.github.whoxamxl.aalyrics.core.model.PlaybackSnapshot
import io.github.whoxamxl.aalyrics.core.model.Track
import io.github.whoxamxl.aalyrics.provider.api.LyricsCandidate
import io.github.whoxamxl.aalyrics.provider.api.LyricsProvider
import io.github.whoxamxl.aalyrics.provider.api.LyricsProviderDescriptor
import io.github.whoxamxl.aalyrics.provider.api.LyricsProviderId
import io.github.whoxamxl.aalyrics.provider.api.LyricsRequest
import io.github.whoxamxl.aalyrics.provider.petitlyrics.PetitLyricsProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

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

        graph.playbackLyricsController.onPlayback(PlaybackSnapshot(track = track))
        advanceUntilIdle()

        assertEquals(listOf(LyricsSyncType.WORD), provider.requests.map { it.preferredSyncType })
        assertEquals(listOf(preferences), selector.preferences)
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
}
