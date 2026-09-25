package io.github.whoxamxl.aalyrics.core.timing

import kotlin.test.Test
import kotlin.test.assertEquals

class EffectiveLyricsPositionTest {
    @Test
    fun zeroOffsetPreservesProjectedPlaybackPosition() {
        assertEquals(
            EffectiveLyricsPosition(31_200L),
            effectiveLyricsPosition(31_200L, LyricsTimingOffset.ZERO),
        )
    }

    @Test
    fun positiveOffsetAdvancesLyrics() {
        assertEquals(
            EffectiveLyricsPosition(32_000L),
            effectiveLyricsPosition(31_200L, LyricsTimingOffset(800L)),
        )
    }

    @Test
    fun negativeOffsetDelaysLyrics() {
        assertEquals(
            EffectiveLyricsPosition(30_400L),
            effectiveLyricsPosition(31_200L, LyricsTimingOffset(-800L)),
        )
    }

    @Test
    fun lyricsBehindMusicUsePositiveAdjustment() {
        val canonicalNextLineStartMs = 10_500L

        assertEquals(
            canonicalNextLineStartMs,
            effectiveLyricsPosition(10_000L, LyricsTimingOffset(500L)).milliseconds,
        )
    }

    @Test
    fun lyricsAheadOfMusicUseNegativeAdjustment() {
        val canonicalPreviousLineStartMs = 10_000L

        assertEquals(
            canonicalPreviousLineStartMs,
            effectiveLyricsPosition(10_500L, LyricsTimingOffset(-500L)).milliseconds,
        )
    }

    @Test
    fun effectivePositionCanBeNegative() {
        assertEquals(
            EffectiveLyricsPosition(-300L),
            effectiveLyricsPosition(200L, LyricsTimingOffset(-500L)),
        )
    }

    @Test
    fun sourceTimestampIsNotRewrittenByTimingAdjustment() {
        val canonicalLineStartMs = 10_500L

        val effectivePosition = effectiveLyricsPosition(10_000L, LyricsTimingOffset(500L))

        assertEquals(10_500L, canonicalLineStartMs)
        assertEquals(canonicalLineStartMs, effectivePosition.milliseconds)
    }
}
