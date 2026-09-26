package io.github.whoxamxl.aalyrics.provider.musixmatch

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MusixmatchClientTest {
    @Test
    fun parsesAnonymousToken() {
        assertEquals("abc123token", MusixmatchClient.parseTokenResponse(tokenResponse("abc123token")))
    }

    @Test
    fun rejectsUpgradeOnlyToken() {
        assertNull(MusixmatchClient.parseTokenResponse(tokenResponse("UpgradeOnly-token")))
    }

    @Test
    fun parsesMacroTrackAndLineSubtitle() {
        val result = MusixmatchClient.parseMacroResponse(macroResponse(
            subtitle = "[00:01.00]Hey Jude\\n[00:03.00]Don't make it bad",
        ))

        assertNotNull(result)
        assertEquals(123L, result.candidate.trackId)
        assertEquals(456L, result.candidate.commonTrackId)
        assertEquals("Hey Jude", result.candidate.title)
        assertEquals("The Beatles", result.candidate.artist)
        assertEquals("1", result.candidate.album)
        assertEquals(431.0, result.candidate.durationSec)
        assertTrue(result.candidate.hasRichSync)
        assertEquals("[00:01.00]Hey Jude\n[00:03.00]Don't make it bad", result.subtitleBody)
    }

    @Test
    fun capturesRichSyncEmbeddedInMacroResponse() {
        val result = MusixmatchClient.parseMacroResponse(macroResponse(
            richSyncBody = """[{"ts":1.5,"x":"Hey Jude","l":[{"c":"Hey","o":0.0},{"c":"Jude","o":0.5}]}]""",
        ))

        val richSyncResponse = assertNotNull(result?.richSyncResponseJson)
        val lines = MusixmatchClient.parseRichSyncResponse(richSyncResponse)
        assertEquals(1, lines.size)
        assertEquals("Hey Jude", lines.single().text)
        assertEquals(1_500L, lines.single().startMs)
    }

    @Test
    fun rejectsMacroWhenMatcherTrackCallFails() {
        assertNull(MusixmatchClient.parseMacroResponse(macroResponse(matcherStatus = 404)))
    }

    @Test
    fun rejectsMalformedOrMissingMacroSections() {
        assertNull(MusixmatchClient.parseMacroResponse("{broken"))
        assertNull(MusixmatchClient.parseMacroResponse("""{"message":{"header":{"status_code":200},"body":{}}}"""))
        assertNull(MusixmatchClient.parseMacroResponse(
            """{"message":{"header":{"status_code":200},"body":{"macro_calls":{"matcher.track.get":7}}}}""",
        ))
    }

    @Test
    fun parsesLineSubtitleIntoTimedLines() {
        val lines = MusixmatchClient.parseSubtitleBody("[00:01.00]Line one\n[00:03.25]Line two")
        assertEquals(listOf(1_000L, 3_250L), lines.map { it.startMs })
        assertEquals(listOf("Line one", "Line two"), lines.map { it.text })
    }

    @Test
    fun parsesRichSyncIntoLineAndRealWordTiming() {
        val lines = MusixmatchClient.parseRichSyncResponse(richSyncResponse(
            """[{"ts":1.5,"te":4.0,"x":"Hey Jude","l":[{"c":"Hey","o":0.0},{"c":" ","o":0.3},{"c":"Jude","o":0.5}]}]""",
        ))

        assertEquals(1, lines.size)
        assertEquals(1_500L, lines.single().startMs)
        assertEquals(4_000L, lines.single().endMs)
        assertEquals("Hey Jude", lines.single().text)
        assertEquals(listOf("Hey", "Jude"), lines.single().words.map { it.text })
        assertEquals(listOf(1_500L, 2_000L), lines.single().words.map { it.startMs })
    }

    @Test
    fun japaneseRichSyncKeepsExactLineTextAndWordTiming() {
        val lines = MusixmatchClient.parseRichSyncResponse(richSyncResponse(
            """[{"ts":2.0,"te":5.0,"x":"君を忘れない","l":[{"c":"君を","o":0.0},{"c":"忘れない","o":0.8}]}]""",
        ))

        assertEquals("君を忘れない", lines.single().text)
        assertEquals(5_000L, lines.single().endMs)
        assertEquals(listOf("君を", "忘れない"), lines.single().words.map { it.text })
        assertEquals(listOf(2_000L, 2_800L), lines.single().words.map { it.startMs })
    }

    @Test
    fun richSyncWithoutLineEndKeepsLineOpenEnded() {
        val lines = MusixmatchClient.parseRichSyncResponse(richSyncResponse(
            """[{"ts":1.5,"x":"Hey Jude","l":[{"c":"Hey","o":0.0},{"c":"Jude","o":0.5}]}]""",
        ))

        assertEquals(null, lines.single().endMs)
    }

    @Test
    fun invalidRichSyncLineEndIsIgnoredWithoutRewritingWordTiming() {
        val lines = MusixmatchClient.parseRichSyncResponse(richSyncResponse(
            """[{"ts":2.0,"te":1.5,"x":"Hello world","l":[{"c":"Hello","o":0.0},{"c":"world","o":0.4}]}]""",
        ))

        assertEquals(null, lines.single().endMs)
        assertEquals(listOf(2_000L, 2_400L), lines.single().words.map { it.startMs })
    }

    @Test
    fun malformedOrDescendingRichSyncIsRejected() {
        assertTrue(MusixmatchClient.parseRichSyncResponse("{broken").isEmpty())
        assertTrue(MusixmatchClient.parseRichSyncResponse(richSyncResponse(
            """[{"ts":2.0,"x":"bad","l":[{"c":"late","o":1.0},{"c":"early","o":0.0}]}]""",
        )).isEmpty())
    }

    private fun tokenResponse(token: String): String =
        """{"message":{"header":{"status_code":200},"body":{"user_token":"$token"}}}"""

    private fun macroResponse(
        matcherStatus: Int = 200,
        subtitle: String = "[00:01.00]Hey Jude",
        richSyncBody: String? = null,
    ): String {
        val escapedRichSync = richSyncBody?.replace("\\", "\\\\")?.replace("\"", "\\\"")
        val richSync = escapedRichSync?.let {
            ""","track.richsync.get":{"message":{"header":{"status_code":200},"body":{"richsync":{"richsync_body":"$it"}}}}"""
        }.orEmpty()
        return """
            {"message":{"header":{"status_code":200},"body":{"macro_calls":{
              "matcher.track.get":{"message":{"header":{"status_code":$matcherStatus},"body":{"track":{
                "track_id":123,"commontrack_id":456,"track_name":"Hey Jude","artist_name":"The Beatles",
                "album_name":"1","track_length":431,"has_richsync":1,"instrumental":0
              }}}},
              "track.subtitles.get":{"message":{"header":{"status_code":200},"body":{"subtitle_list":[
                {"subtitle":{"subtitle_body":"$subtitle"}}
              ]}}}$richSync
            }}}}
        """.trimIndent()
    }

    private fun richSyncResponse(body: String): String {
        val escaped = body.replace("\\", "\\\\").replace("\"", "\\\"")
        return """{"message":{"header":{"status_code":200},"body":{"richsync":{"richsync_body":"$escaped"}}}}"""
    }
}
