package io.github.whoxamxl.aalyrics.provider.lrc

import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.Test

class LrcParserTest {

    @Test
    fun enhancedLrcPreservesFragmentSpacingAndAbsoluteTiming() {
        val lrc = "[00:01.00]<00:01.00>Pro<00:01.10>vi<00:01.20>der <00:01.50>timing"

        val line = LrcParser.parseKaraoke(lrc).single()

        assertEquals(1_000L, line.startMs)
        assertEquals("Provider timing", line.text)
        assertEquals(listOf("Pro", "vi", "der ", "timing"), line.words.map { it.text })
        assertEquals(listOf(1_000L, 1_100L, 1_200L, 1_500L), line.words.map { it.startMs })
    }

    @Test
    fun enhancedLrcUsesFollowingTimestampAsWordEnd() {
        val lrc = "[00:01.00]<00:01.00>Hello <00:01.50>world<00:02.10>"

        val line = LrcParser.parseKaraoke(lrc).single()

        assertEquals(listOf("Hello ", "world"), line.words.map { it.text })
        assertEquals(listOf(1_000L, 1_500L), line.words.map { it.startMs })
        assertEquals(listOf(1_500L, 2_100L), line.words.map { it.endMs })
    }

    @Test
    fun enhancedLrcWithoutTrailingTimestampLeavesFinalWordOpenEnded() {
        val lrc = "[00:01.00]<00:01.00>Hello <00:01.50>world"

        val line = LrcParser.parseKaraoke(lrc).single()

        assertEquals(1_500L, line.words.first().endMs)
        assertEquals(null, line.words.last().endMs)
    }

    @Test
    fun enhancedLrcIgnoresFormattingSpaceBeforeFirstToken() {
        val lrc = "[00:01.00] <00:01.05>Hello <00:01.50>world"

        val line = LrcParser.parseKaraoke(lrc).single()

        assertEquals("Hello world", line.text)
        assertEquals(listOf("Hello ", "world"), line.words.map { it.text })
        assertEquals(1_050L, line.words.first().startMs)
    }

    @Test
    fun enhancedLrcKeepsJapaneseCharacterTokensWithoutInventingSpaces() {
        val lrc = "[00:03.00]<00:03.00>君<00:03.15>を<00:03.30>忘<00:03.45>れ<00:03.60>な<00:03.75>い"

        val line = LrcParser.parseKaraoke(lrc).single()

        assertEquals("君を忘れない", line.text)
        assertEquals(listOf("君", "を", "忘", "れ", "な", "い"), line.words.map { it.text })
    }

    @Test
    fun enhancedLrcAcceptsColonFractionSeparator() {
        val lrc = "[00:01:25]<00:01:25>Hello <00:01:75>world"

        val line = LrcParser.parseKaraoke(lrc).single()

        assertEquals(1_250L, line.startMs)
        assertEquals("Hello world", line.text)
        assertEquals(1_750L, line.words[1].startMs)
    }

    @Test
    fun karaokeParserLeavesLineOnlyLrcWithoutFakeWordTiming() {
        val lines = LrcParser.parseKaraoke("[00:01.00]Line only")

        assertEquals(1, lines.size)
        assertEquals("Line only", lines.single().text)
        assertTrue(lines.single().words.isEmpty())
    }
    @Test
    fun ordinaryLrcPreservesMultipleTimestampsFractionsAndBlankRows() {
        val lines = LrcParser.parse("[ar:Artist]\n[01:02.345][00:01:25] Lyric \n[00:03.00]\ninvalid")
        assertEquals(listOf(1250L, 3000L, 62345L), lines.map { it.startMs })
        assertEquals(listOf("Lyric", "♪", "Lyric"), lines.map { it.text })
        assertTrue(lines.all { it.words.isEmpty() })
    }

    @Test
    fun lineParserPreservesEnhancedTagsAsInWorkingFork() {
        assertEquals("<00:01.00>Hello", LrcParser.parse("[00:01.00]<00:01.00>Hello").single().text)
    }

    @Test
    fun malformedWordOrderIsRejectedWithoutBreakingOtherRows() {
        val lines = LrcParser.parseKaraoke("[00:01.00]<00:02.00>Later<00:01.00>Earlier\n[00:03.00]Valid")
        assertEquals(listOf("Valid"), lines.map { it.text })
    }
}
