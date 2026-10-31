package io.github.whoxamxl.aalyrics

import io.github.whoxamxl.aalyrics.cache.files.FileLyricsCache
import io.github.whoxamxl.aalyrics.core.lyrics.CandidateSelectionPreferences
import io.github.whoxamxl.aalyrics.core.lyrics.LyricsState
import io.github.whoxamxl.aalyrics.core.model.LyricsAttribution
import io.github.whoxamxl.aalyrics.core.model.LyricsDocument
import io.github.whoxamxl.aalyrics.core.model.LyricsSyncType
import io.github.whoxamxl.aalyrics.core.model.PlaybackSnapshot
import io.github.whoxamxl.aalyrics.core.model.TimedLyricLine
import io.github.whoxamxl.aalyrics.core.model.TimedWord
import io.github.whoxamxl.aalyrics.core.model.Track
import io.github.whoxamxl.aalyrics.core.model.TrackReference
import io.github.whoxamxl.aalyrics.provider.api.LyricsCandidate
import io.github.whoxamxl.aalyrics.provider.api.LyricsProvider
import io.github.whoxamxl.aalyrics.provider.api.LyricsProviderDescriptor
import io.github.whoxamxl.aalyrics.provider.api.LyricsProviderId
import io.github.whoxamxl.aalyrics.provider.api.LyricsRequest
import io.github.whoxamxl.aalyrics.provider.selection.CrossProviderCandidateSelector
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class OfflineLyricsCacheIntegrationTest {
    private val directory = Files.createTempDirectory("aalyrics-offline-test").toFile()
    private val track = Track("Song A", listOf("Artist"), "Album", 180_000, setOf(TrackReference("spotify", "song-a")))
    private val lyrics = LyricsDocument(
        lines = listOf(
            TimedLyricLine("One two", 1000, 5000, listOf(TimedWord("One", 1000, 3000), TimedWord("two", 3000, 5000))),
            TimedLyricLine("Three four", 6000, words = listOf(TimedWord("Three", 6000, 8000), TimedWord("four", 8000))),
        ),
        languageTag = "en",
        attribution = LyricsAttribution("musixmatch", "Musixmatch", "test-source"),
    )

    @AfterTest
    fun cleanUp() { directory.deleteRecursively() }

    @Test
    fun `disk cache restores A after offline B and after application graph recreation`() = runTest {
        val provider = Provider()
        val graph = graph(provider)
        graph.play(track.copy(durationMs = null))
        advanceUntilIdle()
        assertEquals(lyrics, assertIs<LyricsState.Ready>(graph.lyricsState.value).lyrics)
        assertEquals(1, provider.calls)

        provider.offline = true
        graph.play(track.copy(title = "Song B", references = setOf(TrackReference("spotify", "song-b"))))
        advanceUntilIdle()
        assertIs<LyricsState.Failed>(graph.lyricsState.value)
        val beforeReturn = provider.calls
        graph.play(track)
        advanceUntilIdle()
        assertEquals(lyrics, assertIs<LyricsState.Ready>(graph.lyricsState.value).lyrics)
        assertEquals(beforeReturn, provider.calls)

        // No shared in-memory cache or coordinator: the new graph can only recover from disk.
        val restartedProvider = Provider().apply { offline = true }
        val restarted = graph(restartedProvider)
        restarted.play(track)
        advanceUntilIdle()
        assertEquals(lyrics, assertIs<LyricsState.Ready>(restarted.lyricsState.value).lyrics)
        assertEquals(0, restartedProvider.calls)
        assertTrue(restarted.lyricsLookupDiagnostics.value.cacheHit)
        restarted.lyricsDemandGate.setPhoneProcessForeground(false)
        restarted.lyricsDemandGate.setPhoneProcessForeground(true)
        advanceUntilIdle()
        assertEquals(0, restartedProvider.calls)
    }

    @Test
    fun `unavailable storage does not break normal provider selection`() = runTest {
        val unavailable = File(directory, "regular-file").apply { writeText("not a directory") }
        val provider = Provider()
        val graph = graph(provider, unavailable)
        graph.play(track)
        advanceUntilIdle()
        assertEquals(lyrics, assertIs<LyricsState.Ready>(graph.lyricsState.value).lyrics)
        assertEquals(1, provider.calls)
    }

    private fun TestScope.graph(provider: Provider, cacheDirectory: File = directory) = ApplicationGraph(
        providers = listOf(provider),
        selector = CrossProviderCandidateSelector(),
        applicationScope = this,
        selectionPreferences = CandidateSelectionPreferences(LyricsSyncType.WORD),
        lyricsCache = FileLyricsCache(cacheDirectory, dispatcher = StandardTestDispatcher(testScheduler)),
    )

    private fun ApplicationGraph.play(track: Track) {
        lyricsDemandGate.setPhoneProcessForeground(true)
        val snapshot = PlaybackSnapshot(track = track)
        playbackSnapshotSink.onPlaybackSnapshot(snapshot)
        lyricsDemandGate.onPlaybackSnapshot(snapshot, sourceEligible = true)
    }

    private inner class Provider : LyricsProvider {
        override val descriptor = LyricsProviderDescriptor(LyricsProviderId("musixmatch"), "Musixmatch", setOf(LyricsSyncType.WORD))
        var offline = false
        var calls = 0
        override suspend fun search(request: LyricsRequest): List<LyricsCandidate> {
            calls++
            check(!offline) { "Offline" }
            return listOf(LyricsCandidate(descriptor.id, request.track, lyrics))
        }
    }
}
