package io.github.whoxamxl.aalyrics.provider.synclrc

import io.github.whoxamxl.aalyrics.core.model.LyricsSyncType
import io.github.whoxamxl.aalyrics.core.model.TimedLyricLine
import io.github.whoxamxl.aalyrics.core.model.Track
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
import okhttp3.mockwebserver.SocketPolicy
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SyncLrcProviderTest {
    private val request = LyricsRequest(
        track = Track("Provider", listOf("Example Artist"), "Example Album", 180_499L),
        preferredSyncType = LyricsSyncType.WORD,
    )

    @Test
    fun wordSearchPreservesRequestAndNormalizesCandidate() = withServer { server, provider ->
        server.enqueueJson(
            """
                {
                  "id":"source-7",
                  "track":"Provider",
                  "artist":"Example Artist",
                  "album":"Example Album",
                  "duration":180.25,
                  "karaoke":"[00:01.00]<00:01.00>Pro<00:01.10>vi<00:01.20>der"
                }
            """.trimIndent(),
        )

        val candidate = provider.search(request).single()

        assertEquals(setOf(LyricsSyncType.WORD), provider.descriptor.supportedSyncTypes)
        assertEquals("synclrc", candidate.providerId.value)
        assertEquals(Track("Provider", listOf("Example Artist"), "Example Album", 180_250L), candidate.matchedTrack)
        assertEquals(LyricsSyncType.WORD, candidate.lyrics.syncType)
        assertEquals("source-7", candidate.lyrics.attribution?.sourceId)
        assertEquals(listOf("Pro", "vi", "der"), (candidate.lyrics.lines.single() as TimedLyricLine).words.map { it.text })
        assertTrue(candidate.evidence.artistQueryCorroborated)

        val sent = assertNotNull(server.takeRequest(3, TimeUnit.SECONDS))
        assertEquals("/lyrics", sent.requestUrl!!.encodedPath)
        assertEquals("Provider", sent.requestUrl!!.queryParameter("track"))
        assertEquals("Example Artist", sent.requestUrl!!.queryParameter("artist"))
        assertEquals("karaoke", sent.requestUrl!!.queryParameter("type"))
        assertEquals("Example Album", sent.requestUrl!!.queryParameter("album"))
        assertEquals("180", sent.requestUrl!!.queryParameter("duration"))
        assertEquals("application/json", sent.getHeader("Accept"))
        assertEquals("AALyrics/test-version (Android; SyncLRC)", sent.getHeader("User-Agent"))
    }

    @Test
    fun nonWordPreferencesAndMissingArtistSkipTransport() = withServer { server, provider ->
        listOf(null, LyricsSyncType.PLAIN, LyricsSyncType.LINE).forEach { preference ->
            assertTrue(provider.search(request.copy(preferredSyncType = preference)).isEmpty())
        }
        assertTrue(
            provider.search(LyricsRequest(Track("Provider"), LyricsSyncType.WORD)).isEmpty(),
        )
        assertEquals(0, server.requestCount)
    }

    @Test
    fun unknownOptionalMetadataIsOmittedAndResponseMetadataFallsBackToRequest() = withServer { server, provider ->
        server.enqueueJson("""{"karaoke":"[00:01.00]<00:01.00>Provider"}""")
        val sparseRequest = LyricsRequest(
            Track("Provider", listOf("Example Artist")),
            LyricsSyncType.WORD,
        )

        val candidate = provider.search(sparseRequest).single()

        assertEquals(Track("Provider", listOf("Example Artist")), candidate.matchedTrack)
        val sent = assertNotNull(server.takeRequest(3, TimeUnit.SECONDS))
        assertNull(sent.requestUrl!!.queryParameter("album"))
        assertNull(sent.requestUrl!!.queryParameter("duration"))
    }

    @Test
    fun unusablePayloadsRemainNoResult() = withServer { server, provider ->
        server.enqueueJson("{broken")
        server.enqueueJson("""{"synced":"[00:01.00]Provider","plain":"Provider"}""")

        assertTrue(provider.search(request).isEmpty())
        assertTrue(provider.search(request).isEmpty())
        assertEquals(2, server.requestCount)
    }

    @Test
    fun serviceFailureSurfacesAsOperationalFailure() = withServer { server, provider ->
        server.enqueue(MockResponse().setResponseCode(503))

        val error = assertFailsWith<IOException> { provider.search(request) }

        assertEquals("SyncLRC HTTP 503", error.message)
        assertEquals(1, server.requestCount)
    }

    @Test
    fun networkFailureSurfacesAsOperationalFailure() = withServer { server, provider ->
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))
        assertFailsWith<IOException> { provider.search(request) }
        assertEquals(1, server.requestCount)
    }

    @Test
    fun cancellationCancelsInFlightCall() = runBlocking {
        val failed = CountDownLatch(1)
        val listener = object : EventListener() {
            override fun callFailed(call: Call, ioe: IOException) {
                failed.countDown()
            }
        }
        val client = OkHttpClient.Builder()
            .retryOnConnectionFailure(false)
            .eventListener(listener)
            .build()
        try {
            MockWebServer().use { server ->
                server.start()
                server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
                val provider = SyncLrcProvider(client, server.url("/lyrics").toString(), "test-version")
                val job = launch(Dispatchers.Default) { provider.search(request) }
                assertNotNull(server.takeRequest(3, TimeUnit.SECONDS))
                withTimeout(2_000L) { job.cancelAndJoin() }
                assertTrue(job.isCancelled)
                assertTrue(failed.await(2, TimeUnit.SECONDS))
                assertEquals(1, server.requestCount)
            }
        } finally {
            client.connectionPool.evictAll()
            client.dispatcher.executorService.shutdown()
        }
    }

    private fun withServer(block: suspend (MockWebServer, SyncLrcProvider) -> Unit) = runBlocking {
        val client = OkHttpClient.Builder().retryOnConnectionFailure(false).build()
        try {
            MockWebServer().use { server ->
                server.start()
                block(server, SyncLrcProvider(client, server.url("/lyrics").toString(), "test-version"))
            }
        } finally {
            client.connectionPool.evictAll()
            client.dispatcher.executorService.shutdown()
        }
    }

    private fun MockWebServer.enqueueJson(json: String) {
        enqueue(MockResponse().setHeader("Content-Type", "application/json").setBody(json))
    }
}
