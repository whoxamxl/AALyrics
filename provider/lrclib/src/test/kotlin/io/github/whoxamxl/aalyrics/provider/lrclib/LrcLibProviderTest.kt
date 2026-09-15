package io.github.whoxamxl.aalyrics.provider.lrclib

import com.google.gson.Gson
import io.github.whoxamxl.aalyrics.core.model.*
import io.github.whoxamxl.aalyrics.provider.api.LyricsRequest
import kotlinx.coroutines.*
import okhttp3.Call
import okhttp3.EventListener
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import okhttp3.mockwebserver.SocketPolicy
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.*

class LrcLibProviderTest {
    private val request = LyricsRequest(Track("Song", listOf("Artist"), "Album", 240_999))

    @Test
    fun strongExactMatchSkipsSearchAndNormalizesProviderMetadata() = withServer { server, provider ->
        server.enqueueJson(row(id = 42, duration = 241.5))
        val candidate = provider.search(request).single()
        assertEquals("lrclib", candidate.providerId.value)
        assertEquals(setOf(LyricsSyncType.PLAIN, LyricsSyncType.LINE), provider.descriptor.supportedSyncTypes)
        assertEquals(Track("Song", listOf("Artist"), "Album", 241_500), candidate.matchedTrack)
        assertEquals("42", candidate.lyrics.attribution?.sourceId)
        assertEquals(LyricsSyncType.LINE, candidate.lyrics.syncType)
        assertEquals(1250L, (candidate.lyrics.lines.single() as TimedLyricLine).startMs)
        assertFalse(candidate.evidence.artistQueryCorroborated)
        val sent = server.nextRequest()
        assertEquals("/api/get", sent.requestUrl!!.encodedPath)
        assertEquals("240", sent.requestUrl!!.queryParameter("duration"))
        assertEquals("Artist", sent.requestUrl!!.queryParameter("artist_name"))
        assertEquals("Album", sent.requestUrl!!.queryParameter("album_name"))
        assertTrue(sent.getHeader("User-Agent")!!.startsWith("AALyrics"))
        assertEquals(1, server.requestCount)
    }

    @Test
    fun exact404FallsBackToBothStructuredQueries() = withServer { server, provider ->
        server.enqueue(MockResponse().setResponseCode(404))
        server.enqueueJson(listOf(row(id = 1)))
        server.enqueueJson(emptyList<Any>())
        assertEquals("1", provider.search(request).single().lyrics.attribution?.sourceId)
        assertEquals("/api/get", server.nextRequest().requestUrl!!.encodedPath)
        assertEquals("Album", server.nextRequest().requestUrl!!.queryParameter("album_name"))
        assertNull(server.nextRequest().requestUrl!!.queryParameter("album_name"))
        assertEquals(3, server.requestCount)
    }

    @Test
    fun featuredVariantUsesStrippedTitleAndArtistAfterStructuredSearches() = withServer { server, provider ->
        server.enqueueJson(emptyList<Any>())
        server.enqueueJson(emptyList<Any>())
        server.enqueueJson(listOf(row(title = "Song", artist = "Artist")))
        val query = LyricsRequest(Track("Song feat. Guest", listOf("Artist feat. Guest"), "Album"))
        assertEquals("Song", provider.search(query).single().matchedTrack.title)
        assertEquals("Song feat. Guest", server.nextRequest().requestUrl!!.queryParameter("track_name"))
        assertNull(server.nextRequest().requestUrl!!.queryParameter("album_name"))
        val stripped = server.nextRequest().requestUrl!!
        assertEquals("Song", stripped.queryParameter("track_name"))
        assertEquals("Artist", stripped.queryParameter("artist_name"))
        assertEquals(3, server.requestCount)
    }

    @Test
    fun titleOnlySearchFindsContributorComponentAfterStructuredMisses() = withServer { server, provider ->
        server.enqueueJson(emptyList<Any>())
        server.enqueueJson(listOf(row(artist = "Samuel E. Wright")))
        val candidate = provider.search(LyricsRequest(Track("Song", listOf(
            "Alan Menken", "Howard Ashman", "Samuel E. Wright", "Disney",
        ), durationMs = 240_000))).single()
        assertEquals(listOf("Samuel E. Wright"), candidate.matchedTrack.artists)
        assertEquals("Alan Menken, Howard Ashman, Samuel E. Wright, Disney",
            server.nextRequest().requestUrl!!.queryParameter("artist_name"))
        val titleOnly = server.nextRequest().requestUrl!!
        assertEquals("Song", titleOnly.queryParameter("track_name"))
        assertNull(titleOnly.queryParameter("artist_name"))
        assertEquals(2, server.requestCount)
    }

    @Test
    fun freeTextSyncBeatsPreviouslyDiscoveredPlain() = withServer { server, provider ->
        server.enqueueJson(listOf(row(id = 1, synced = null, plain = "Plain first")))
        server.enqueueJson(emptyList<Any>())
        server.enqueueJson(listOf(row(id = 2, synced = "[00:02.00]Synced later")))
        val candidate = provider.search(request.copy(track = request.track.copy(album = null))).single()
        assertEquals("2", candidate.lyrics.attribution?.sourceId)
        assertEquals(LyricsSyncType.LINE, candidate.lyrics.syncType)
        server.nextRequest()
        server.nextRequest()
        assertEquals("Song Artist", server.nextRequest().requestUrl!!.queryParameter("q"))
    }

    @Test
    fun plainFallbackWaitsUntilAllSyncSearchesFinishAndPreservesRows() = withServer { server, provider ->
        server.enqueueJson(row(id = 3, synced = null, plain = " First row \n\nSecond row"))
        repeat(4) { server.enqueueJson(emptyList<Any>()) }
        val candidate = provider.search(request).single()
        assertEquals(LyricsSyncType.PLAIN, candidate.lyrics.syncType)
        assertEquals(listOf(" First row ", "Second row"), candidate.lyrics.lines.map { it.text })
        assertEquals(5, server.requestCount)
    }

    @Test
    fun duplicateIdsKeepFirstResponseAsInFork() = withServer { server, provider ->
        server.enqueueJson(listOf(row(id = 8, synced = "[00:01.00]First")))
        server.enqueueJson(listOf(row(id = 8, synced = "[00:01.00]Second")))
        val candidate = provider.search(request.copy(track = request.track.copy(durationMs = null))).single()
        assertEquals("First", candidate.lyrics.lines.single().text)
        assertEquals(2, server.requestCount)
    }

    @Test
    fun missingIdsDeduplicateByMetadataAsInFork() = withServer { server, provider ->
        server.enqueueJson(listOf(row(id = null, synced = "[00:01.00]First")))
        server.enqueueJson(listOf(row(id = null, synced = "[00:01.00]Second")))
        val candidate = provider.search(request.copy(track = request.track.copy(durationMs = null))).single()
        assertEquals("First", candidate.lyrics.lines.single().text)
        assertNull(candidate.lyrics.attribution?.sourceId)
    }

    @Test
    fun unusableExactSyncCannotStopDiscovery() = withServer { server, provider ->
        server.enqueueJson(row(id = 1, synced = "[00:01.00]", plain = "Plain"))
        server.enqueueJson(listOf(row(id = 2)))
        server.enqueueJson(emptyList<Any>())
        assertEquals("2", provider.search(request).single().lyrics.attribution?.sourceId)
        assertEquals(3, server.requestCount)
    }

    @Test
    fun malformedSyncCannotHideLowerScoringValidSync() = withServer { server, provider ->
        server.enqueueJson(listOf(row(id = 1, synced = "not LRC"), row(id = 2, album = "Different album", duration = 247.0)))
        server.enqueueJson(emptyList<Any>())
        val candidate = provider.search(request.copy(track = request.track.copy(durationMs = null))).single()
        assertEquals("2", candidate.lyrics.attribution?.sourceId)
    }

    @Test
    fun malformedRowsAreRejectedWithoutDiscardingValidSiblings() = withServer { server, provider ->
        server.enqueueJson(listOf(null, 7, mapOf("id" to "bad"), row(title = null), row(title = ""), row(id = 9)))
        val candidate = provider.search(LyricsRequest(Track("Song", listOf("Artist")))).single()
        assertEquals("9", candidate.lyrics.attribution?.sourceId)
    }

    @Test
    fun malformedPayloadContinuesToTitleOnlyFallback() = withServer { server, provider ->
        server.enqueue(MockResponse().setBody("[{broken"))
        server.enqueueJson(listOf(row()))
        assertEquals(1, provider.search(LyricsRequest(Track("Song", listOf("Artist")))).size)
        assertEquals(2, server.requestCount)
    }

    @Test
    fun weakVersionsArtistsAndDurationAreRejected() = withServer { server, provider ->
        val invalid = listOf(
            row(id = 1, title = "Unrelated"),
            row(id = 2, title = "Song (Live)"),
            row(id = 3, instrumental = true),
            row(id = 4, duration = 256.0),
            row(id = 5, artist = "Completely unrelated", duration = null),
            row(id = 6, synced = "[00:01.00]", plain = " "),
        )
        server.enqueueJson(invalid)
        repeat(2) { server.enqueueJson(emptyList<Any>()) }
        assertTrue(provider.search(request.copy(track = request.track.copy(album = null))).isEmpty())
        assertEquals(3, server.requestCount)
    }

    @Test
    fun albumEvidenceAllowsLiveTitle() = withServer { server, provider ->
        server.enqueueJson(listOf(row(title = "Song (Live)", album = "Live at Wembley")))
        server.enqueueJson(emptyList<Any>())
        val candidate = provider.search(LyricsRequest(Track("Song", listOf("Artist"), "Live at Wembley"))).single()
        assertEquals("Song (Live)", candidate.matchedTrack.title)
    }

    @Test
    fun fullWidthMetadataMatchesAndUnknownProviderMetadataStaysUnknown() = withServer { server, provider ->
        server.enqueueJson(listOf(row(title = "Ｓｏｎｇ", artist = null, album = "-", duration = null)))
        val candidate = provider.search(LyricsRequest(Track("Song", listOf("Artist")))).single()
        assertEquals("Ｓｏｎｇ", candidate.matchedTrack.title)
        assertTrue(candidate.matchedTrack.artists.isEmpty())
        assertNull(candidate.matchedTrack.album)
        assertNull(candidate.matchedTrack.durationMs)
    }

    @Test
    fun requestQueryEncodingRoundTripsSpecialCharacters() = withServer { server, provider ->
        val title = "曲名 & A+B?"
        server.enqueueJson(listOf(row(title = title)))
        provider.search(LyricsRequest(Track(title, listOf("Artist"))))
        assertEquals(title, server.nextRequest().requestUrl!!.queryParameter("track_name"))
    }

    @Test
    fun unavailableAndMalformedOnlyResultsReturnEmpty() = withServer { server, provider ->
        server.enqueueJson(null)
        server.enqueueJson(listOf(row(synced = "broken", plain = " ")))
        server.enqueueJson(emptyList<Any>())
        assertTrue(provider.search(LyricsRequest(Track("Song", listOf("Artist")))).isEmpty())
    }

    @Test
    fun serviceAndRateLimitFailuresSurfaceAsExceptions() {
        for (status in listOf(401, 429, 500)) withServer { server, provider ->
            server.enqueue(MockResponse().setResponseCode(status))
            val error = assertFailsWith<IOException> { provider.search(request) }
            assertTrue(error.message!!.contains(status.toString()))
            assertEquals(1, server.requestCount)
        }
    }

    @Test
    fun search404IsOperationalFailureRatherThanExactMiss() = withServer { server, provider ->
        server.enqueue(MockResponse().setResponseCode(404))
        assertFailsWith<IOException> { provider.search(LyricsRequest(Track("Song"))) }
    }

    @Test
    fun disconnectedTransportSurfacesAsFailure() = withServer { server, provider ->
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))
        assertFailsWith<IOException> { provider.search(request) }
    }

    @Test
    fun cancellationCancelsInFlightHttpCallWithoutStartingFallback() = runBlocking {
        val failed = CountDownLatch(1)
        val listener = object : EventListener() {
            override fun callFailed(call: Call, ioe: IOException) { failed.countDown() }
        }
        val client = OkHttpClient.Builder().eventListener(listener).build()
        MockWebServer().use { server ->
            server.start()
            server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
            val provider = LrcLibProvider(client, server.url("/api/").toString())
            val job = launch(Dispatchers.Default) { provider.search(request) }
            server.nextRequest()
            withTimeout(2_000) { job.cancelAndJoin() }
            assertTrue(job.isCancelled)
            assertTrue(failed.await(2, TimeUnit.SECONDS))
            assertEquals(1, server.requestCount)
        }
        client.connectionPool.evictAll()
        client.dispatcher.executorService.shutdown()
    }

    private fun withServer(block: suspend (MockWebServer, LrcLibProvider) -> Unit) = runBlocking {
        val client = OkHttpClient.Builder().retryOnConnectionFailure(false).build()
        try {
            MockWebServer().use { server ->
                server.start()
                block(server, LrcLibProvider(client, server.url("/api/").toString()))
            }
        } finally {
            client.connectionPool.evictAll()
            client.dispatcher.executorService.shutdown()
        }
    }

    private fun MockWebServer.enqueueJson(value: Any?) {
        enqueue(MockResponse().setHeader("Content-Type", "application/json").setBody(Gson().toJson(value)))
    }

    private fun MockWebServer.nextRequest(): RecordedRequest =
        assertNotNull(takeRequest(3, TimeUnit.SECONDS), "Expected HTTP request")

    private fun row(
        id: Int? = 1,
        title: String? = "Song",
        artist: String? = "Artist",
        album: String? = "Album",
        duration: Double? = 240.0,
        synced: String? = "[00:01.25]Line",
        plain: String? = null,
        instrumental: Boolean = false,
    ): Map<String, Any?> = mapOf(
        "id" to id, "trackName" to title, "artistName" to artist, "albumName" to album,
        "duration" to duration, "syncedLyrics" to synced, "plainLyrics" to plain, "instrumental" to instrumental,
    )
}
