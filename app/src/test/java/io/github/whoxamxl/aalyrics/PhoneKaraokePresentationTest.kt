package io.github.whoxamxl.aalyrics

import io.github.whoxamxl.aalyrics.core.model.LyricsDocument
import io.github.whoxamxl.aalyrics.core.model.TimedLyricLine
import io.github.whoxamxl.aalyrics.core.model.TimedWord
import io.github.whoxamxl.aalyrics.core.timing.EffectiveLyricsPosition
import io.github.whoxamxl.aalyrics.core.timing.projectLyricsTiming
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Adapted from the working fork's LyricWordLayout and PhoneKaraokeSweep cases. */
class PhoneKaraokePresentationTest {
    @Test
    fun `English fragments sweep one lexical word without restarting`() {
        val line = TimedLyricLine("Provider timing works", 0L, words = listOf(
            TimedWord("Pro", 0L, 100L),
            TimedWord("vi", 100L, 200L),
            TimedWord("der", 200L, 400L),
            TimedWord("timing", 500L, 900L),
            TimedWord("works", 1_000L, 1_400L),
        ))
        val first = sweep(line, 50L)
        val middle = sweep(line, 150L)
        val last = sweep(line, 300L)
        assertEquals(0 to 8, first?.let { it.start to it.end })
        assertEquals(0 to 8, middle?.let { it.start to it.end })
        assertEquals(0 to 8, last?.let { it.start to it.end })
        assertEquals(0.125f, first?.progress)
        assertEquals(0.375f, middle?.progress)
        assertEquals(0.75f, last?.progress)
        assertEquals(9 to 15, sweep(line, 700L)?.let { it.start to it.end })
    }

    @Test
    fun `Japanese character tokens share readable ranges`() {
        val text = "君を忘れない"
        val line = TimedLyricLine(text, 1_000L, words = text.mapIndexed { index, char ->
            TimedWord(char.toString(), 1_000L + index * 150L)
        })
        assertEquals(0 to 2, sweep(line, 1_075L)?.let { it.start to it.end })
        assertEquals(2 to text.length, sweep(line, 1_750L)?.let { it.start to it.end })
    }

    @Test
    fun `case and Unicode normalization preserve canonical source ranges`() {
        val caseLine = TimedLyricLine("Hello world", 0L, words = listOf(
            TimedWord("HE", 0L, 150L),
            TimedWord("LLO", 150L, 300L),
            TimedWord("WORLD", 400L, 800L),
        ))
        assertEquals(0 to 5, sweep(caseLine, 200L)?.let { it.start to it.end })
        assertEquals(6 to 11, sweep(caseLine, 600L)?.let { it.start to it.end })

        val normalized = TimedLyricLine("Café", 0L, words = listOf(
            TimedWord("Cafe\u0301", 0L, 500L),
        ))
        assertEquals(0 to 4, sweep(normalized, 250L)?.let { it.start to it.end })
    }

    @Test
    fun `unrelated tokens and incidental anchors fall back to normal style`() {
        val unrelated = TimedLyricLine("different line text", 0L, words = listOf(
            TimedWord("line", 0L, 100L), TimedWord("one", 100L, 200L),
        ))
        val weak = TimedLyricLine("a long day", 0L, words = listOf(
            TimedWord("A", 0L, 100L), TimedWord("X", 100L, 200L),
            TimedWord("G", 200L, 300L), TimedWord("Z", 300L, 400L),
        ))
        assertNull(sweep(unrelated, 150L))
        assertNull(sweep(weak, 250L))
    }

    @Test
    fun `single token uses semantic progress and explicit gap has no sweep`() {
        val line = TimedLyricLine("hello world", 0L, words = listOf(
            TimedWord("hello", 0L, 300L), TimedWord("world", 1_000L, 1_500L),
        ))
        assertEquals(0.5f, sweep(line, 150L)?.progress)
        assertNull(sweep(line, 300L))
        assertNull(sweep(line, 700L))
        assertEquals(6 to 11, sweep(line, 1_250L)?.let { it.start to it.end })
    }

    @Test
    fun `line wide Japanese pseudo token does not enable Karaoke sweep`() {
        val text = "あなたと二人で踊ろうよ"
        val line = TimedLyricLine(
            text = text,
            startMs = 1_000L,
            words = listOf(TimedWord(text, 1_000L)),
        )

        assertEquals(false, LyricWordLayout.hasRenderableWordGranularity(line))
        assertNull(sweep(line, 1_325L))
    }

    @Test
    fun `line wide English pseudo token does not enable Karaoke sweep`() {
        val text = "Hello world again"
        val line = TimedLyricLine(
            text = text,
            startMs = 1_000L,
            words = listOf(TimedWord(text, 1_000L)),
        )

        assertEquals(false, LyricWordLayout.hasRenderableWordGranularity(line))
        assertNull(sweep(line, 1_325L))
    }

    @Test
    fun `genuine single visible word keeps final visual fallback`() {
        val line = TimedLyricLine(
            text = "忘れない",
            startMs = 1_000L,
            words = listOf(TimedWord("忘れない", 1_000L)),
        )

        assertEquals(true, LyricWordLayout.hasRenderableWordGranularity(line))
        assertEquals(0.5f, sweep(line, 1_325L)?.progress)
    }

    @Test
    fun `only final open ended group receives Phone visual fallback`() {
        val line = TimedLyricLine("final", 1_000L, words = listOf(TimedWord("final", 1_000L)))
        assertEquals(0.5f, sweep(line, 1_325L)?.progress)
        assertNull(sweep(line, 1_650L))
        assertNull(sweep(line, 3_000L))
    }

    private fun sweep(line: TimedLyricLine, positionMs: Long) = mapPhoneKaraokeSweep(
        line,
        projectLyricsTiming(LyricsDocument(listOf(line)), EffectiveLyricsPosition(positionMs)),
        positionMs,
    )
}
