package io.github.whoxamxl.aalyrics.provider.api

import io.github.whoxamxl.aalyrics.core.model.LyricsAttribution
import io.github.whoxamxl.aalyrics.core.model.LyricsDocument
import io.github.whoxamxl.aalyrics.core.model.LyricsSyncType
import io.github.whoxamxl.aalyrics.core.model.PlainLyricLine
import io.github.whoxamxl.aalyrics.core.model.Track
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class LyricsProviderContractTest {

    @Test
    fun `provider id must be non-blank`() {
        assertFailsWith<IllegalArgumentException> {
            LyricsProviderId(" ")
        }
    }

    @Test
    fun `descriptor must declare capabilities`() {
        assertFailsWith<IllegalArgumentException> {
            LyricsProviderDescriptor(
                id = LyricsProviderId("example"),
                displayName = "Example",
                supportedSyncTypes = emptySet(),
            )
        }
    }

    @Test
    fun `candidate requires matching attribution provider`() {
        val providerId = LyricsProviderId("example")
        val track = Track(title = "Track")

        assertFailsWith<IllegalArgumentException> {
            LyricsCandidate(
                providerId = providerId,
                matchedTrack = track,
                lyrics = LyricsDocument(
                    lines = listOf(PlainLyricLine("line")),
                    attribution = LyricsAttribution(providerId = "different"),
                ),
            )
        }
    }

    @Test
    fun `sync preference remains a preference on the request`() {
        val request = LyricsRequest(
            track = Track(title = "Track"),
            preferredSyncType = LyricsSyncType.WORD,
        )

        assertEquals(LyricsSyncType.WORD, request.preferredSyncType)
    }
}
