package io.github.whoxamxl.aalyrics.provider.petitlyrics

import io.github.whoxamxl.aalyrics.core.model.LyricsSyncType
import io.github.whoxamxl.aalyrics.core.model.TimedLyricLine
import io.github.whoxamxl.aalyrics.provider.api.LyricsRequest
import io.github.whoxamxl.aalyrics.core.model.Track
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
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PetitLyricsProviderTest {
    private val config = PetitLyricsConfig("fake-user", "fake-app", "fake-package", "fake-client")
    private val request = LyricsRequest(Track("Song", listOf("Artist"), "Album", 240_000))

    @Test
    fun wordSyncSearchPreservesAuthProtocolAndNormalizesCandidate() = withServer { server, provider ->
        server.enqueueXml(searchResponse(song(id = "42", data = wordPayload("Hello ", "world"))))

        val candidate = provider.search(request).single()

        assertEquals(setOf(LyricsSyncType.LINE, LyricsSyncType.WORD), provider.descriptor.supportedSyncTypes)
        assertEquals("petitlyrics", candidate.providerId.value)
        assertEquals("42", candidate.lyrics.attribution?.sourceId)
        assertEquals(LyricsSyncType.WORD, candidate.lyrics.syncType)
        assertEquals(listOf("Hello ", "world"), (candidate.lyrics.lines.single() as TimedLyricLine).words.map { it.text })
        assertEquals(Track("Song", listOf("Artist"), "Album"), candidate.matchedTrack)
        assertTrue(candidate.evidence.artistQueryCorroborated)

        val form = server.takeForm()
        assertEquals("3", form["lyricsType"])
        assertEquals("1.3.4", form["sdkVer"])
        assertEquals("fake-user", form["userId"])
        assertEquals("fake-app", form["appName"])
        assertEquals("fake-package", form["pkgName"])
        assertEquals("fake-client", form["clientAppId"])
        assertEquals("10", form["maxcount"])
        assertEquals("Song", form["key_title"])
        assertEquals("Artist", form["key_artist"])
        assertEquals("Album", form["key_album"])
        assertEquals(1, server.requestCount)
    }

    @Test
    fun progressiveDiscoveryRelaxesAlbumThenArtistAndReportsTitleOnlyEvidence() = withServer { server, provider ->
        server.enqueueXml(searchResponse())
        server.enqueueXml(searchResponse())
        server.enqueueXml(searchResponse(song(data = wordPayload("Found"))))

        val candidate = provider.search(request).single()

        assertFalse(candidate.evidence.artistQueryCorroborated)
        assertEquals(setOf("key_title", "key_artist", "key_album"), server.takeForm().keys.filter { it.startsWith("key_") }.toSet())
        assertEquals(setOf("key_title", "key_artist"), server.takeForm().keys.filter { it.startsWith("key_") }.toSet())
        assertEquals(setOf("key_title"), server.takeForm().keys.filter { it.startsWith("key_") }.toSet())
    }

    @Test
    fun ranksCandidatesLocallyInsteadOfAcceptingFirstResponse() = withServer { server, provider ->
        server.enqueueXml(searchResponse(
            song(id = "wrong", title = "Different", data = wordPayload("Wrong")),
            song(id = "right", data = wordPayload("Right")),
        ))

        val candidate = provider.search(request).single()

        assertEquals("right", candidate.lyrics.attribution?.sourceId)
        assertEquals("Right", candidate.lyrics.lines.single().text)
    }

    @Test
    fun lineSyncUsesPlainCompanionByLyricsId() = withServer { server, provider ->
        server.enqueueXml(searchResponse(song(id = "77", type = 2, data = linePayload(123, 456))))
        server.enqueueXml(searchResponse(song(id = "77", type = 1, data = textPayload("First\nSecond"))))

        val candidate = provider.search(request).single()

        assertEquals(LyricsSyncType.LINE, candidate.lyrics.syncType)
        assertEquals(listOf(1230L, 4560L), candidate.lyrics.lines.map { (it as TimedLyricLine).startMs })
        assertEquals(listOf("First", "Second"), candidate.lyrics.lines.map { it.text })
        server.takeForm()
        val companion = server.takeForm()
        assertEquals("1", companion["lyricsType"])
        assertEquals("77", companion["key_lyricsId"])
        assertNull(companion["key_title"])
    }

    @Test
    fun failedIdCompanionLookupFallsBackToMetadataCompanion() = withServer { server, provider ->
        server.enqueueXml(searchResponse(song(id = "88", type = 2, data = linePayload(100))))
        server.enqueue(MockResponse().setResponseCode(500))
        server.enqueueXml(searchResponse(
            song(id = "wrong", artist = "Other", type = 1, data = textPayload("Wrong")),
            song(id = "88", type = 1, data = textPayload("Recovered")),
        ))

        val candidate = provider.search(request).single()

        assertEquals("Recovered", candidate.lyrics.lines.single().text)
        server.takeForm()
        server.takeForm()
        val metadata = server.takeForm()
        assertEquals("1", metadata["lyricsType"])
        assertEquals("Song", metadata["key_title"])
        assertEquals("Artist", metadata["key_artist"])
        assertEquals("Album", metadata["key_album"])
        assertEquals("3", metadata["maxcount"])
    }

    @Test
    fun malformedCandidateIsAttemptedOnceAcrossQueriesBeforeLaterCandidateWins() = withServer { server, provider ->
        val malformed = song(id = "duplicate", data = "not-base64")
        server.enqueueXml(searchResponse(malformed))
        server.enqueueXml(searchResponse(malformed, song(id = "valid", data = wordPayload("Valid"))))

        assertEquals("valid", provider.search(request).single().lyrics.attribution?.sourceId)
        assertEquals(2, server.requestCount)
    }

    @Test
    fun malformedOuterXmlAndPayloadExhaustToEmptyRatherThanFakeSync() = withServer { server, provider ->
        server.enqueueXml("<not-closed")
        server.enqueueXml(searchResponse(song(data = textPayload("not WSY"))))
        server.enqueueXml(searchResponse())

        assertTrue(provider.search(request).isEmpty())
        assertEquals(3, server.requestCount)
    }

    @Test
    fun allOperationalFailuresSurfaceAfterProviderFallbackIsExhausted() = withServer { server, provider ->
        server.enqueue(MockResponse().setResponseCode(500))
        server.enqueue(MockResponse().setResponseCode(429))
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))

        val error = assertFailsWith<IOException> { provider.search(request) }
        assertEquals("PetitLyrics HTTP 500", error.message)
        assertEquals(3, server.requestCount)
    }

    @Test
    fun incompleteConfigurationSkipsNetworkAndDoesNotExposeSecrets() = runBlocking {
        MockWebServer().use { server ->
            server.start()
            val incomplete = PetitLyricsConfig("secret-user", "", "secret-package", "secret-client")
            val provider = PetitLyricsProvider(incomplete, endpoint = server.url("/api").toString())

            assertFalse(provider.isConfigured)
            assertTrue(provider.search(request).isEmpty())
            assertEquals(0, server.requestCount)
            assertEquals("PetitLyricsConfig(configured=false)", incomplete.toString())
            assertFalse(incomplete.toString().contains("secret"))
        }
    }

    @Test
    fun cancellationCancelsInFlightCallWithoutStartingFallback() = runBlocking {
        val failed = CountDownLatch(1)
        val listener = object : EventListener() {
            override fun callFailed(call: Call, ioe: IOException) { failed.countDown() }
        }
        val client = OkHttpClient.Builder().eventListener(listener).build()
        try {
            MockWebServer().use { server ->
                server.start()
                server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
                val provider = PetitLyricsProvider(config, client, server.url("/api").toString())
                val job = launch(Dispatchers.Default) { provider.search(request) }
                assertNotNull(server.takeRequest(3, TimeUnit.SECONDS))
                withTimeout(2_000) { job.cancelAndJoin() }
                assertTrue(job.isCancelled)
                assertTrue(failed.await(2, TimeUnit.SECONDS))
                assertEquals(1, server.requestCount)
            }
        } finally {
            client.connectionPool.evictAll()
            client.dispatcher.executorService.shutdown()
        }
    }

    private fun withServer(block: suspend (MockWebServer, PetitLyricsProvider) -> Unit) = runBlocking {
        val client = OkHttpClient.Builder().retryOnConnectionFailure(false).build()
        try {
            MockWebServer().use { server ->
                server.start()
                block(server, PetitLyricsProvider(config, client, server.url("/api").toString()))
            }
        } finally {
            client.connectionPool.evictAll()
            client.dispatcher.executorService.shutdown()
        }
    }

    private fun MockWebServer.enqueueXml(xml: String) {
        enqueue(MockResponse().setHeader("Content-Type", "application/xml").setBody(xml))
    }

    private fun MockWebServer.takeForm(): Map<String, String> {
        val request = assertNotNull(takeRequest(3, TimeUnit.SECONDS), "Expected HTTP request")
        return request.body.readUtf8().split('&').filter { it.isNotBlank() }.associate { pair ->
            val (key, value) = pair.split('=', limit = 2)
            URLDecoder.decode(key, StandardCharsets.UTF_8) to URLDecoder.decode(value, StandardCharsets.UTF_8)
        }
    }

    private fun searchResponse(vararg songs: String): String =
        "<result><songs>${songs.joinToString("")}</songs></result>"

    private fun song(
        id: String = "1",
        title: String = "Song",
        artist: String = "Artist",
        album: String = "Album",
        type: Int = 3,
        data: String,
    ): String = """
        <song><lyricsId>$id</lyricsId><title>$title</title><artist>$artist</artist><album>$album</album>
        <lyricsType>$type</lyricsType><lyricsData>$data</lyricsData></song>
    """.trimIndent()

    private fun wordPayload(vararg words: String): String {
        val xml = buildString {
            append("<wsy><line><linestring>")
            append(words.joinToString(""))
            append("</linestring>")
            words.forEachIndexed { index, word ->
                val start = 1_000 + index * 500
                append("<word><starttime>$start</starttime><endtime>${start + 400}</endtime>")
                append("<wordstring>$word</wordstring></word>")
            }
            append("</line></wsy>")
        }
        return Base64.getEncoder().encodeToString(xml.toByteArray())
    }

    private fun textPayload(text: String): String = Base64.getEncoder().encodeToString(text.toByteArray())

    private fun linePayload(vararg timesCentiseconds: Int): String {
        val protectionKey = 0x2345
        val bytes = ByteArray(0xcc + timesCentiseconds.size * 2)
        bytes[0x19] = 0
        writeUInt16Le(bytes, 0x1a, protectionKey)
        writeUInt32Le(bytes, 0x38, timesCentiseconds.size)
        timesCentiseconds.forEachIndexed { index, value ->
            writeUInt16Le(bytes, 0xcc + index * 2, (value and 0xffff) xor protectionKey)
        }
        return Base64.getEncoder().encodeToString(bytes)
    }

    private fun writeUInt16Le(bytes: ByteArray, offset: Int, value: Int) {
        bytes[offset] = value.toByte()
        bytes[offset + 1] = (value ushr 8).toByte()
    }

    private fun writeUInt32Le(bytes: ByteArray, offset: Int, value: Int) {
        bytes[offset] = value.toByte()
        bytes[offset + 1] = (value ushr 8).toByte()
        bytes[offset + 2] = (value ushr 16).toByte()
        bytes[offset + 3] = (value ushr 24).toByte()
    }
}
