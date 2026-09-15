package io.github.whoxamxl.aalyrics.provider.selection

import io.github.whoxamxl.aalyrics.core.model.LyricsAttribution
import io.github.whoxamxl.aalyrics.core.model.LyricsDocument
import io.github.whoxamxl.aalyrics.core.model.PlainLyricLine
import io.github.whoxamxl.aalyrics.core.model.TimedLyricLine
import io.github.whoxamxl.aalyrics.core.model.Track
import io.github.whoxamxl.aalyrics.provider.api.LyricsCandidate
import io.github.whoxamxl.aalyrics.provider.api.LyricsProviderId
import kotlin.test.Test
import kotlin.test.assertEquals

class UnusableLyricsCandidateTest {
    private val selector = CrossProviderCandidateSelector()

    @Test
    fun timedCandidateWithoutUsableTextFallsBackToUsefulPlainLyrics() {
        val track = Track(
            title = "Example Song",
            artists = listOf("Example Artist"),
            durationMs = 180_000L,
        )
        val unusableTimed = LyricsCandidate(
            providerId = LyricsProviderId("lrclib"),
            matchedTrack = track,
            lyrics = LyricsDocument(
                lines = listOf(
                    TimedLyricLine(text = "", startMs = 1_000L),
                    TimedLyricLine(text = "♪", startMs = 2_000L),
                ),
                attribution = LyricsAttribution(providerId = "lrclib"),
            ),
        )
        val usefulPlain = LyricsCandidate(
            providerId = LyricsProviderId("plain-provider"),
            matchedTrack = track,
            lyrics = LyricsDocument(
                lines = listOf(PlainLyricLine("usable lyrics")),
                attribution = LyricsAttribution(providerId = "plain-provider"),
            ),
        )

        val selected = selector.select(track, listOf(unusableTimed, usefulPlain))

        assertEquals("plain-provider", selected?.providerId?.value)
    }
}
