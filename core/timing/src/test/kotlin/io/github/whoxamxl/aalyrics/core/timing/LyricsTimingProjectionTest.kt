package io.github.whoxamxl.aalyrics.core.timing

import io.github.whoxamxl.aalyrics.core.model.LyricsDocument
import io.github.whoxamxl.aalyrics.core.model.PlainLyricLine
import io.github.whoxamxl.aalyrics.core.model.TimedLyricLine
import io.github.whoxamxl.aalyrics.core.model.TimedWord
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

    @Test
    fun wordSelectionPreservesStartsExclusiveEndsAndGaps() {
        val document = LyricsDocument(
            lines = listOf(
                TimedLyricLine(
                    text = "Three words",
                    startMs = 1_000L,
                    words = listOf(
                        TimedWord("First", startMs = 1_200L, endMs = 1_400L),
                        TimedWord("second", startMs = 1_600L),
                        TimedWord("third", startMs = 2_000L, endMs = 2_200L),
                    ),
                ),
            ),
        )
        val expectedByPosition = listOf(
            1_000L to (null to WordTimingBoundary.BEFORE_FIRST),
            1_199L to (null to WordTimingBoundary.BEFORE_FIRST),
            1_200L to (0 to WordTimingBoundary.ACTIVE),
            1_399L to (0 to WordTimingBoundary.ACTIVE),
            1_400L to (null to WordTimingBoundary.GAP),
            1_599L to (null to WordTimingBoundary.GAP),
            1_600L to (1 to WordTimingBoundary.ACTIVE),
            1_999L to (1 to WordTimingBoundary.ACTIVE),
            2_000L to (2 to WordTimingBoundary.ACTIVE),
            2_199L to (2 to WordTimingBoundary.ACTIVE),
            2_200L to (null to WordTimingBoundary.AFTER_LAST),
        )

        expectedByPosition.forEach { (positionMs, expected) ->
            val projection = projectLyricsTiming(document, EffectiveLyricsPosition(positionMs))
            assertEquals(0, projection.activeLineIndex)
            assertEquals(expected.first, projection.activeWordIndex, "position=$positionMs")
            assertEquals(expected.second, projection.wordBoundary, "position=$positionMs")
            assertNull(projection.wordProgress)
        }
    }

    @Test
    fun endedNewerWordDoesNotReactivateOlderOpenEndedWord() {
        val document = LyricsDocument(
            lines = listOf(
                TimedLyricLine(
                    text = "Older newer",
                    startMs = 0L,
                    words = listOf(
                        TimedWord("Older", startMs = 0L),
                        TimedWord("newer", startMs = 500L, endMs = 600L),
                    ),
                ),
            ),
        )

        assertEquals(0, projectLyricsTiming(document, EffectiveLyricsPosition(499L)).activeWordIndex)
        assertEquals(1, projectLyricsTiming(document, EffectiveLyricsPosition(500L)).activeWordIndex)
        val afterNewer = projectLyricsTiming(document, EffectiveLyricsPosition(600L))
        assertNull(afterNewer.activeWordIndex)
        assertEquals(WordTimingBoundary.AFTER_LAST, afterNewer.wordBoundary)
    }

    @Test
    fun backwardSeekRecomputesEarlierWordWithoutHistory() {
        val document = LyricsDocument(
            lines = listOf(
                TimedLyricLine(
                    text = "One two",
                    startMs = 0L,
                    words = listOf(
                        TimedWord("One", startMs = 0L, endMs = 100L),
                        TimedWord("two", startMs = 200L, endMs = 300L),
                    ),
                ),
            ),
        )

        assertEquals(1, projectLyricsTiming(document, EffectiveLyricsPosition(250L)).activeWordIndex)
        assertEquals(0, projectLyricsTiming(document, EffectiveLyricsPosition(50L)).activeWordIndex)
    }

    @Test
    fun wordFactsComeOnlyFromTheActiveTimedLine() {
        val document = LyricsDocument(
            lines = listOf(
                TimedLyricLine("First", startMs = 0L, words = listOf(TimedWord("First", 0L))),
                TimedLyricLine("Second", startMs = 500L),
            ),
        )

        val projection = projectLyricsTiming(document, EffectiveLyricsPosition(500L))
        assertEquals(1, projection.activeLineIndex)
        assertNull(projection.activeWordIndex)
        assertEquals(WordTimingBoundary.UNAVAILABLE, projection.wordBoundary)
    }
}
