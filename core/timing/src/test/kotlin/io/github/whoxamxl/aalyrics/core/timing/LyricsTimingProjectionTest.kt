package io.github.whoxamxl.aalyrics.core.timing

import io.github.whoxamxl.aalyrics.core.model.LyricsDocument
import io.github.whoxamxl.aalyrics.core.model.PlainLyricLine
import io.github.whoxamxl.aalyrics.core.model.TimedLyricLine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LyricsTimingProjectionTest {
    private val mixedDocument = LyricsDocument(
        lines = listOf(
            PlainLyricLine("Intro"),
            TimedLyricLine("First", startMs = 1_000L, endMs = 1_500L),
            PlainLyricLine("Interlude"),
            TimedLyricLine("Second", startMs = 2_000L, endMs = 2_500L),
        ),
    )

    @Test
    fun lineSelectionPreservesOriginalIndicesAndCurrentBoundaryRules() {
        val expectedByPosition = listOf(
            -300L to null,
            0L to null,
            999L to null,
            1_000L to 1,
            1_500L to 1,
            1_999L to 1,
            2_000L to 3,
            2_500L to 3,
            9_000L to 3,
        )

        expectedByPosition.forEach { (positionMs, expectedIndex) ->
            val projection = projectLyricsTiming(
                document = mixedDocument,
                position = EffectiveLyricsPosition(positionMs),
            )
            assertEquals(expectedIndex, projection.activeLineIndex, "position=$positionMs")
            assertNull(projection.activeWordIndex)
            assertNull(projection.wordProgress)
            assertEquals(WordTimingBoundary.UNAVAILABLE, projection.wordBoundary)
        }
    }

    @Test
    fun backwardSeekRecomputesEarlierLineWithoutHistory() {
        assertEquals(3, projectLyricsTiming(mixedDocument, EffectiveLyricsPosition(2_300L)).activeLineIndex)
        assertEquals(1, projectLyricsTiming(mixedDocument, EffectiveLyricsPosition(1_200L)).activeLineIndex)
    }

    @Test
    fun plainDocumentHasNoActiveLine() {
        val document = LyricsDocument(lines = listOf(PlainLyricLine("Untimed")))

        assertNull(projectLyricsTiming(document, EffectiveLyricsPosition(5_000L)).activeLineIndex)
    }

    @Test
    fun equalAndOutOfOrderStartsFollowLastMatchingDocumentIndex() {
        val document = LyricsDocument(
            lines = listOf(
                TimedLyricLine("Later start", startMs = 3_000L),
                TimedLyricLine("Earlier start", startMs = 1_000L),
                TimedLyricLine("Same start", startMs = 1_000L),
            ),
        )

        assertEquals(2, projectLyricsTiming(document, EffectiveLyricsPosition(1_000L)).activeLineIndex)
        assertEquals(2, projectLyricsTiming(document, EffectiveLyricsPosition(3_000L)).activeLineIndex)
    }
}
