package io.github.whoxamxl.aalyrics.provider.musixmatch

import com.google.gson.Gson
import io.github.whoxamxl.aalyrics.core.model.LyricsSyncType
import io.github.whoxamxl.aalyrics.core.model.TimedLyricLine
import io.github.whoxamxl.aalyrics.core.model.Track
import io.github.whoxamxl.aalyrics.core.model.TrackReference
import io.github.whoxamxl.aalyrics.provider.api.LyricsRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MusixmatchProviderTest {
    private val spotifyId = "6rqhFgbbKwnb9MLmUQDhG6"
    private val request = LyricsRequest(Track(
        title = "Song",
        artists = listOf("Artist"),
        album = "Album",
        durationMs = 240_999L,
        references = setOf(TrackReference("spotify", spotifyId)),
    ))

    @Test
    fun anonymousMobileFlowNormalizesEmbeddedRichSyncAndRequestEvidence() = withServer { server, provider ->
        server.enqueueJson(tokenResponse("token-1"))
        server.enqueueJson(macroResponse(richSyncBody = wordBody()))

        val candidate = provider.search(request).single()

        assertEquals("musixmatch", candidate.providerId.value)
        assertEquals(setOf(LyricsSyncType.LINE, LyricsSyncType.WORD), provider.descriptor.supportedSyncTypes)
        assertEquals(Track("Song", listOf("Artist"), "Album", 240_000L), candidate.matchedTrack)
        assertEquals(LyricsSyncType.WORD, candidate.lyrics.syncType)
        assertEquals("123", candidate.lyrics.attribution?.sourceId)
        assertEquals(listOf("Hello", "world"), (candidate.lyrics.lines.single() as TimedLyricLine).words.map { it.text })
        assertTrue(candidate.evidence.artistQueryCorroborated)

        val token = server.nextRequest()
        assertEquals("/ws/1.1/token.get", token.requestUrl!!.encodedPath)
        assertEquals("mac-ios-v2.0", token.requestUrl!!.queryParameter("app_id"))
        assertEquals("en", token.requestUrl!!.queryParameter("user_language"))
        assertEquals(MusixmatchTransport.APP_VERSION, token.getHeader("x-mxm-app-version"))
        assertEquals(MusixmatchTransport.MOBILE_USER_AGENT, token.getHeader("X-User-Agent"))

        val macro = server.nextRequest()
        assertEquals("/ws/1.1/macro.subtitles.get", macro.requestUrl!!.encodedPath)
        assertEquals("token-1", macro.requestUrl!!.queryParameter("usertoken"))
        assertEquals("lyrics_richsynched", macro.requestUrl!!.queryParameter("namespace"))
        assertEquals("track.richsync", macro.requestUrl!!.queryParameter("optional_calls"))
        assertEquals("lrc", macro.requestUrl!!.queryParameter("subtitle_format"))
        assertEquals("Song", macro.requestUrl!!.queryParameter("q_track"))
        assertEquals("Artist", macro.requestUrl!!.queryParameter("q_artist"))
        assertEquals("Album", macro.requestUrl!!.queryParameter("q_album"))
        assertEquals("241", macro.requestUrl!!.queryParameter("q_duration"))
        assertEquals(spotifyId, macro.requestUrl!!.queryParameter("track_spotify_id"))
    }

    @Test
    fun nonSpotifyRequestOmitsSpotifyHintAndBlankArtistEvidence() = withServer { server, provider ->
        server.enqueueJson(tokenResponse("token"))
        server.enqueueJson(macroResponse(artist = "", richSyncBody = wordBody()))

        val candidate = provider.search(LyricsRequest(Track("Song", album = "Album", durationMs = 240_000L))).single()

        assertFalse(candidate.evidence.artistQueryCorroborated)
        server.nextRequest()
        val macro = server.nextRequest()
        assertEquals("", macro.requestUrl!!.queryParameter("q_artist"))
        assertEquals(null, macro.requestUrl!!.queryParameter("track_spotify_id"))
    }

    @Test
    fun explicitSpotifyConflictRejectsOtherwiseMatchingCandidate() = withServer { server, provider ->
        server.enqueueJson(tokenResponse("token"))
        server.enqueueJson(macroResponse(spotifyTrackId = "6RQHFGBBKWNB9MLMUQDHG6", richSyncBody = wordBody()))
        assertTrue(provider.search(request).isEmpty())
    }

    @Test
    fun missingReturnedSpotifyIdKeepsMetadataFallback() = withServer { server, provider ->
        server.enqueueJson(tokenResponse("token"))
        server.enqueueJson(macroResponse(spotifyTrackId = "", richSyncBody = wordBody()))
        assertEquals(1, provider.search(request).size)
    }

    @Test
    fun instrumentalAndWeakMetadataAreRejected() = withServer { server, provider ->
        server.enqueueJson(tokenResponse("token"))
        server.enqueueJson(macroResponse(instrumental = true, richSyncBody = wordBody()))
        server.enqueueJson(macroResponse(title = "Unrelated", richSyncBody = wordBody()))

        assertTrue(provider.search(request).isEmpty())
        assertTrue(provider.search(request).isEmpty())
        assertEquals(3, server.requestCount)
    }

    @Test
    fun missingEmbeddedRichSyncUsesDedicatedFallback() = withServer { server, provider ->
        server.enqueueJson(tokenResponse("token"))
        server.enqueueJson(macroResponse())
        server.enqueueJson(richSyncResponse(wordBody()))

        val candidate = provider.search(request).single()

        assertEquals(LyricsSyncType.WORD, candidate.lyrics.syncType)
        server.nextRequest()
        server.nextRequest()
        val fallback = server.nextRequest()
        assertEquals("/ws/1.1/track.richsync.get", fallback.requestUrl!!.encodedPath)
        assertEquals("456", fallback.requestUrl!!.queryParameter("commontrack_id"))
        assertEquals("token", fallback.requestUrl!!.queryParameter("usertoken"))
    }

    @Test
    fun failedDedicatedRichSyncStillFallsBackToLineSubtitle() = withServer { server, provider ->
        server.enqueueJson(tokenResponse("token"))
        server.enqueueJson(macroResponse(subtitle = "[00:01.00]Line fallback"))
        server.enqueue(MockResponse().setResponseCode(500))

        val candidate = provider.search(request).single()

        assertEquals(LyricsSyncType.LINE, candidate.lyrics.syncType)
        assertEquals("Line fallback", candidate.lyrics.lines.single().text)
    }

    @Test
    fun failedDedicatedRichSyncSurfacesWhenNoSubtitleFallbackExists() = withServer { server, provider ->
        server.enqueueJson(tokenResponse("token"))
        server.enqueueJson(macroResponse(subtitle = ""))
        server.enqueue(MockResponse().setResponseCode(500))

        val error = assertFailsWith<IOException> { provider.search(request) }
        assertEquals("Musixmatch HTTP 500 for track.richsync.get", error.message)
    }

    @Test
    fun failedDedicatedRichSyncIsNotHiddenByTimestampOnlySubtitle() = withServer { server, provider ->
        server.enqueueJson(tokenResponse("token"))
        server.enqueueJson(macroResponse(subtitle = "[00:01.00]"))
        server.enqueue(MockResponse().setResponseCode(500))

        val error = assertFailsWith<IOException> { provider.search(request) }
        assertEquals("Musixmatch HTTP 500 for track.richsync.get", error.message)
    }

    @Test
    fun lineSubtitleIsUsedWhenTrackHasNoRichSync() = withServer { server, provider ->
        server.enqueueJson(tokenResponse("token"))
        server.enqueueJson(macroResponse(hasRichSync = false, subtitle = "[00:02.25]Line"))

        val candidate = provider.search(request).single()
        assertEquals(LyricsSyncType.LINE, candidate.lyrics.syncType)
        assertEquals(2_250L, (candidate.lyrics.lines.single() as TimedLyricLine).startMs)
        assertEquals(2, server.requestCount)
    }

    @Test
    fun validTokenIsReusedWithinTtl() = withServer { server, provider ->
        server.enqueueJson(tokenResponse("token"))
        server.enqueueJson(macroResponse(richSyncBody = wordBody()))
        server.enqueueJson(macroResponse(richSyncBody = wordBody()))

        provider.search(request)
        provider.search(request)

        assertEquals(listOf("token.get", "macro.subtitles.get", "macro.subtitles.get"), server.requestEndpoints())
    }

    @Test
    fun expiredTokenIsAcquiredAgainBeforeTheNextMacro() {
        var timeMs = 1_000L
        withServer(nowMs = { timeMs }) { server, provider ->
            server.enqueueJson(tokenResponse("token-1"))
            server.enqueueJson(macroResponse(richSyncBody = wordBody()))
            server.enqueueJson(tokenResponse("token-2"))
            server.enqueueJson(macroResponse(richSyncBody = wordBody()))

            provider.search(request)
            timeMs += 60_001L
            provider.search(request)

            assertEquals(
                listOf("token.get", "macro.subtitles.get", "token.get", "macro.subtitles.get"),
                server.requestEndpoints(),
            )
        }
    }

    @Test
    fun rejectedTokenRefreshesOnceAndRetriesAuthenticatedRequest() = withServer { server, provider ->
        server.enqueueJson(tokenResponse("old-token"))
        server.enqueue(MockResponse().setResponseCode(401))
        server.enqueueJson(tokenResponse("new-token"))
        server.enqueueJson(macroResponse(richSyncBody = wordBody()))

        assertEquals(1, provider.search(request).size)

        assertEquals("old-token", server.nextRequestAfterToken().requestUrl!!.queryParameter("usertoken"))
        server.nextRequest()
        assertEquals("new-token", server.nextRequest().requestUrl!!.queryParameter("usertoken"))
        assertEquals(4, server.requestCount)
    }

    @Test
    fun apiTokenRejectionAlsoRefreshesOnce() = withServer { server, provider ->
        server.enqueueJson(tokenResponse("old-token"))
        server.enqueueJson(apiResponse(401))
        server.enqueueJson(tokenResponse("new-token"))
        server.enqueueJson(macroResponse(richSyncBody = wordBody()))

        assertEquals(1, provider.search(request).size)
        assertEquals(4, server.requestCount)
    }

    @Test
    fun malformedMacroReturnsEmptyAndOperationalTokenFailureSurfaces() = withServer { server, provider ->
        server.enqueueJson(tokenResponse("token"))
        server.enqueue(MockResponse().setBody("{broken"))
        assertTrue(provider.search(request).isEmpty())
    }

    @Test
    fun tokenServiceFailureSurfacesAsOperationalFailure() = withServer { server, provider ->
        server.enqueue(MockResponse().setResponseCode(503))
        val error = assertFailsWith<IOException> { provider.search(request) }
        assertEquals("Musixmatch HTTP 503 for token.get", error.message)
    }

    @Test
    fun unusableInitialTokenResponsesSurfaceAsOperationalFailures() {
        val responses = listOf(
            Gson().toJson(apiResponse(401)),
            Gson().toJson(apiResponse(403)),
            Gson().toJson(tokenResponse("UpgradeOnly-token")),
            "{broken",
            Gson().toJson(apiResponse(200)),
        )

        responses.forEach { response ->
            withServer { server, provider ->
                server.enqueueJson(response)
                val error = assertFailsWith<IOException> { provider.search(request) }
                assertEquals("Musixmatch token.get returned no usable token", error.message)
                assertEquals(1, server.requestCount)
            }
        }
    }

    @Test
    fun cancellationCancelsInFlightMacroCall() = runBlocking {
        val failed = CountDownLatch(1)
        val listener = object : EventListener() {
            override fun callFailed(call: Call, ioe: IOException) {
                if (call.request().url.encodedPath.endsWith("macro.subtitles.get")) failed.countDown()
            }
        }
        val client = OkHttpClient.Builder().eventListener(listener).build()
        try {
            MockWebServer().use { server ->
                server.start()
                server.enqueueJson(tokenResponse("token"))
                server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
                val provider = MusixmatchProvider(client, server.url("/ws/1.1/").toString())
                val job = launch(Dispatchers.Default) { provider.search(request) }
                assertNotNull(server.takeRequest(3, TimeUnit.SECONDS))
                assertNotNull(server.takeRequest(3, TimeUnit.SECONDS))
                withTimeout(2_000L) { job.cancelAndJoin() }
                assertTrue(job.isCancelled)
                assertTrue(failed.await(2, TimeUnit.SECONDS))
                assertEquals(2, server.requestCount)
            }
        } finally {
            client.connectionPool.evictAll()
            client.dispatcher.executorService.shutdown()
        }
    }

    private fun withServer(
        nowMs: () -> Long = { 1_000L },
        block: suspend (MockWebServer, MusixmatchProvider) -> Unit,
    ) = runBlocking {
        val client = OkHttpClient.Builder().retryOnConnectionFailure(false).build()
        try {
            MockWebServer().use { server ->
                server.start()
                block(server, MusixmatchProvider(client, server.url("/ws/1.1/").toString(), nowMs = nowMs))
            }
        } finally {
            client.connectionPool.evictAll()
            client.dispatcher.executorService.shutdown()
        }
    }

    private fun MockWebServer.enqueueJson(value: Any) {
        enqueue(MockResponse().setHeader("Content-Type", "application/json").setBody(
            if (value is String) value else Gson().toJson(value),
        ))
    }

    private fun MockWebServer.nextRequest(): RecordedRequest =
        assertNotNull(takeRequest(3, TimeUnit.SECONDS), "Expected HTTP request")

    private fun MockWebServer.nextRequestAfterToken(): RecordedRequest {
        nextRequest()
        return nextRequest()
    }

    private fun MockWebServer.requestEndpoints(): List<String> = buildList {
        repeat(requestCount) {
            add(nextRequest().requestUrl!!.pathSegments.last())
        }
    }

    private fun tokenResponse(token: String): Map<String, Any> = apiResponse(
        status = 200,
        body = mapOf("user_token" to token),
    )

    private fun richSyncResponse(body: String): Map<String, Any> = apiResponse(
        status = 200,
        body = mapOf("richsync" to mapOf("richsync_body" to body)),
    )

    private fun macroResponse(
        title: String = "Song",
        artist: String = "Artist",
        album: String = "Album",
        spotifyTrackId: String = spotifyId,
        instrumental: Boolean = false,
        hasRichSync: Boolean = true,
        subtitle: String = "",
        richSyncBody: String? = null,
    ): Map<String, Any> {
        val calls = linkedMapOf<String, Any>(
            "matcher.track.get" to apiResponse(200, mapOf("track" to mapOf(
                "track_id" to 123,
                "commontrack_id" to 456,
                "track_spotify_id" to spotifyTrackId,
                "track_name" to title,
                "artist_name" to artist,
                "album_name" to album,
                "track_length" to 240,
                "has_richsync" to if (hasRichSync) 1 else 0,
                "instrumental" to if (instrumental) 1 else 0,
            ))),
            "track.subtitles.get" to apiResponse(200, mapOf(
                "subtitle_list" to listOf(mapOf("subtitle" to mapOf("subtitle_body" to subtitle))),
            )),
        )
        richSyncBody?.let {
            calls["track.richsync.get"] = richSyncResponse(it)
        }
        return apiResponse(200, mapOf("macro_calls" to calls))
    }

    private fun apiResponse(status: Int, body: Any = emptyMap<String, Any>()): Map<String, Any> = mapOf(
        "message" to mapOf(
            "header" to mapOf("status_code" to status),
            "body" to body,
        ),
    )

    private fun wordBody(): String =
        """[{"ts":1.5,"te":3.0,"x":"Hello world","l":[{"c":"Hello","o":0.0},{"c":"world","o":0.5}]}]"""
}
