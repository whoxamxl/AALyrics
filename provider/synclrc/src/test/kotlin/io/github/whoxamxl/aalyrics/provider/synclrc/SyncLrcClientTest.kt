package io.github.whoxamxl.aalyrics.provider.synclrc

import com.google.gson.annotations.SerializedName
import io.github.whoxamxl.aalyrics.core.model.Track
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SyncLrcClientTest {
    private val client = SyncLrcClient(SyncLrcTransport())
    private val track = Track(
        title = "Provider",
        artists = listOf("Example Artist"),
        album = "Example Album",
        durationMs = 180_000L,
    )

    @Test
    fun apiResponseFieldsHaveStableSerializedNames() {
        val expected = mapOf(
            "karaoke" to "karaoke",
            "synced" to "synced",
            "plain" to "plain",
            "lyrics" to "lyrics",
            "type" to "type",
            "id" to "id",
            "track" to "track",
            "artist" to "artist",
            "album" to "album",
            "duration" to "duration",
            "instrumental" to "instrumental",
        )

        val actual = SyncLrcClient.ApiResponse::class.java.declaredFields
            .filterNot { it.isSynthetic }
            .associate { field -> field.name to field.getAnnotation(SerializedName::class.java)?.value }

        expected.forEach { (fieldName, jsonName) -> assertEquals(jsonName, actual[fieldName]) }
    }

    @Test
    fun acceptsCurrentPublicKaraokeFieldResponse() {
        val result = client.parseApiResponse(
            """
                {
                  "id": "abc",
                  "track": "Provider",
                  "artist": "Example Artist",
                  "album": "Example Album",
                  "duration": 180,
                  "instrumental": false,
                  "karaoke": "[00:01.00]<00:01.00>Pro<00:01.10>vi<00:01.20>der",
                  "synced": "[00:01.00]Provider",
                  "plain": "Provider"
                }
            """.trimIndent(),
            track,
        )

        assertEquals("Provider", result?.matchedTitle)
        assertEquals("Example Artist", result?.matchedArtist)
        assertEquals(180.0, result?.matchedDurationSec)
        assertEquals("abc", result?.sourceId)
        assertEquals("Provider", result?.lines?.single()?.text)
        assertEquals(listOf("Pro", "vi", "der"), result?.lines?.single()?.words?.map { it.text })
        assertTrue(result?.artistQueryCorroborated == true)
    }

    @Test
    fun acceptsLegacyTypeSpecificKaraokeResponseForCompatibility() {
        val result = client.parseApiResponse(
            """{"type":"KaRaOkE","lyrics":"[00:01.00]<00:01.00>Provider"}""",
            track,
        )

        assertEquals("Provider", result?.lines?.single()?.text)
        assertEquals("Provider", result?.matchedTitle)
        assertEquals("Example Artist", result?.matchedArtist)
        assertEquals("Example Album", result?.matchedAlbum)
    }

    @Test
    fun rejectsCurrentResponseWithoutKaraokePayload() {
        val json = """{"synced":"[00:01.00]Provider","plain":"Provider"}"""
        assertNull(client.parseApiResponse(json, track))
    }

    @Test
    fun rejectsLegacySyncedAndPlainFallbacks() {
        val synced = """{"type":"synced","lyrics":"[00:01.00]Provider"}"""
        val plain = """{"type":"plain","lyrics":"Provider"}"""
        assertNull(client.parseApiResponse(synced, track))
        assertNull(client.parseApiResponse(plain, track))
    }

    @Test
    fun rejectsInstrumentalLineOnlyAndPlaceholderKaraoke() {
        val instrumental = """{"instrumental":true,"karaoke":"[00:01.00]<00:01.00>Provider"}"""
        val lineOnly = """{"karaoke":"[00:01.00]Provider"}"""
        val placeholder = """{"karaoke":"[00:01.00]<00:01.00>♪"}"""
        assertNull(client.parseApiResponse(instrumental, track))
        assertNull(client.parseApiResponse(lineOnly, track))
        assertNull(client.parseApiResponse(placeholder, track))
    }

    @Test
    fun malformedAndBlankPayloadsAreNoResult() {
        assertNull(client.parseApiResponse("", track))
        assertNull(client.parseApiResponse("{broken", track))
    }

    @Test
    fun preservesJapaneseFineGrainedTimingForDisplayLayer() {
        val result = client.parseApiResponse(
            """{"track":"君を忘れない","artist":"Example Artist","karaoke":"[00:01.00]<00:01.00>君<00:01.10>を<00:01.20>忘<00:01.30>れ<00:01.40>な<00:01.50>い"}""",
        )
        val line = result?.lines?.single()

        assertEquals("君を忘れない", line?.text)
        assertEquals(6, line?.words?.size)
        assertTrue(line?.words?.all { it.text.length == 1 } == true)
    }
}
