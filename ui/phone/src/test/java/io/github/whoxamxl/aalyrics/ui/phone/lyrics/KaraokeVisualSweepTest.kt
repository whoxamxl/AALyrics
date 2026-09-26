package io.github.whoxamxl.aalyrics.ui.phone.lyrics

import androidx.compose.ui.geometry.Rect
import kotlin.test.Test
import kotlin.test.assertEquals

class KaraokeVisualSweepTest {
    @Test
    fun `RTL logical box order still sweeps visual left to right`() {
        val clip = karaokeVisualSweepClipRects(
            boxes = listOf(
                KaraokeVisualBox(line = 0, bounds = Rect(20f, 0f, 30f, 10f)),
                KaraokeVisualBox(line = 0, bounds = Rect(10f, 0f, 20f, 10f)),
                KaraokeVisualBox(line = 0, bounds = Rect(0f, 0f, 10f, 10f)),
            ),
            progress = 0.5f,
        )

        assertEquals(
            listOf(Rect(0f, 0f, 15f, 10f)),
            clip,
        )
    }

    @Test
    fun `wrapped sweep consumes earlier visual line before next line`() {
        val clip = karaokeVisualSweepClipRects(
            boxes = listOf(
                KaraokeVisualBox(line = 1, bounds = Rect(10f, 20f, 20f, 30f)),
                KaraokeVisualBox(line = 0, bounds = Rect(10f, 0f, 20f, 10f)),
                KaraokeVisualBox(line = 1, bounds = Rect(0f, 20f, 10f, 30f)),
                KaraokeVisualBox(line = 0, bounds = Rect(0f, 0f, 10f, 10f)),
            ),
            progress = 0.75f,
        )

        assertEquals(
            listOf(
                Rect(0f, 0f, 20f, 10f),
                Rect(0f, 20f, 10f, 30f),
            ),
            clip,
        )
    }

    @Test
    fun `overlapping glyph boxes contribute visual union width only once`() {
        val clip = karaokeVisualSweepClipRects(
            boxes = listOf(
                KaraokeVisualBox(line = 0, bounds = Rect(0f, 0f, 10f, 10f)),
                KaraokeVisualBox(line = 0, bounds = Rect(0f, 0f, 10f, 10f)),
                KaraokeVisualBox(line = 0, bounds = Rect(8f, 0f, 20f, 10f)),
            ),
            progress = 0.5f,
        )

        assertEquals(
            listOf(Rect(0f, 0f, 10f, 10f)),
            clip,
        )
    }

    @Test
    fun `zero progress produces no clip and full progress consumes all visual lines`() {
        val boxes = listOf(
            KaraokeVisualBox(line = 0, bounds = Rect(0f, 0f, 10f, 10f)),
            KaraokeVisualBox(line = 1, bounds = Rect(0f, 20f, 10f, 30f)),
        )

        assertEquals(emptyList(), karaokeVisualSweepClipRects(boxes, progress = 0f))
        assertEquals(
            listOf(
                Rect(0f, 0f, 10f, 10f),
                Rect(0f, 20f, 10f, 30f),
            ),
            karaokeVisualSweepClipRects(boxes, progress = 1f),
        )
    }
}
