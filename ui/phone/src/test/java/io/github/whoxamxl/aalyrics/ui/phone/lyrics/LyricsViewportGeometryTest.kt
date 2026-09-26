package io.github.whoxamxl.aalyrics.ui.phone.lyrics

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class LyricsViewportGeometryTest {
    @Test
    fun `existing row spacing absorbs small vertical focus growth`() {
        assertEquals(
            100,
            reservedTimedRowHeightPx(
                unscaledHeightPx = 100,
                rowSpacingPx = 16,
                maxScale = 1.15f,
            ),
        )
    }

    @Test
    fun `tall wrapped rows reserve only scale growth beyond normal spacing`() {
        assertEquals(
            214,
            reservedTimedRowHeightPx(
                unscaledHeightPx = 200,
                rowSpacingPx = 16,
                maxScale = 1.15f,
            ),
        )
    }

    @Test
    fun `zero row spacing reserves the full maximum scaled height`() {
        assertEquals(
            230,
            reservedTimedRowHeightPx(
                unscaledHeightPx = 200,
                rowSpacingPx = 0,
                maxScale = 1.15f,
            ),
        )
    }

    @Test
    fun `invalid geometry inputs are rejected`() {
        assertFailsWith<IllegalArgumentException> {
            reservedTimedRowHeightPx(
                unscaledHeightPx = -1,
                rowSpacingPx = 16,
                maxScale = 1.15f,
            )
        }
        assertFailsWith<IllegalArgumentException> {
            reservedTimedRowHeightPx(
                unscaledHeightPx = 100,
                rowSpacingPx = -1,
                maxScale = 1.15f,
            )
        }
        assertFailsWith<IllegalArgumentException> {
            reservedTimedRowHeightPx(
                unscaledHeightPx = 100,
                rowSpacingPx = 16,
                maxScale = 0.9f,
            )
        }
    }
    @Test
    fun `lazy row key ignores translation-only changes`() {
        val canonical = LyricsViewportLineUiState(
            text = "Hello world",
            words = listOf("Hello", "world"),
        )
        val translated = canonical.copy(translatedText = "こんにちは世界")

        assertEquals(
            lyricsLazyItemKey(4, canonical),
            lyricsLazyItemKey(4, translated),
        )
    }

    @Test
    fun `lazy row key distinguishes canonical row positions`() {
        val line = LyricsViewportLineUiState(text = "Repeated line")

        kotlin.test.assertNotEquals(
            lyricsLazyItemKey(2, line),
            lyricsLazyItemKey(3, line),
        )
    }

    @Test
    fun `lazy row key rejects negative indices`() {
        assertFailsWith<IllegalArgumentException> {
            lyricsLazyItemKey(
                -1,
                LyricsViewportLineUiState(text = "Invalid"),
            )
        }
    }

    @Test
    fun `lazy focus delta centers a visible timed row at 45 percent`() {
        assertEquals(
            35f,
            lazyFocusScrollDelta(
                focusIndex = 2f,
                visibleItems = listOf(
                    LazyViewportItemGeometry(index = 2, offset = 200, size = 120),
                ),
                viewportStartOffset = 0,
                viewportEndOffset = 500,
            ),
        )
    }

    @Test
    fun `lazy focus delta interpolates between adjacent row centers`() {
        assertEquals(
            25f,
            lazyFocusScrollDelta(
                focusIndex = 2.5f,
                visibleItems = listOf(
                    LazyViewportItemGeometry(index = 2, offset = 160, size = 80),
                    LazyViewportItemGeometry(index = 3, offset = 260, size = 80),
                ),
                viewportStartOffset = 0,
                viewportEndOffset = 500,
            ),
        )
    }

    @Test
    fun `timed playback direction identifies offscreen rows by item index`() {
        val visible = listOf(
            LazyViewportItemGeometry(index = 4, offset = 0, size = 80),
            LazyViewportItemGeometry(index = 5, offset = 96, size = 80),
        )

        assertEquals(
            PlaybackRegionDirection.ABOVE,
            timedPlaybackRegionDirection(
                targetIndex = 2,
                visibleItems = visible,
                viewportStartOffset = 0,
                viewportEndOffset = 500,
            ),
        )
        assertEquals(
            PlaybackRegionDirection.BELOW,
            timedPlaybackRegionDirection(
                targetIndex = 8,
                visibleItems = visible,
                viewportStartOffset = 0,
                viewportEndOffset = 500,
            ),
        )
    }

    @Test
    fun `plain lazy target preserves lead in and reaches final row`() {
        assertEquals(
            PlainLazyTarget(index = 0, scrollOffsetPx = 0),
            plainLazyTarget(
                lineCount = 10,
                playbackProgress = 0.03f,
                estimatedRowStridePx = 100,
            ),
        )
        assertEquals(
            PlainLazyTarget(index = 9, scrollOffsetPx = 0),
            plainLazyTarget(
                lineCount = 10,
                playbackProgress = 0.98f,
                estimatedRowStridePx = 100,
            ),
        )
    }

    @Test
    fun `plain lazy target advances within an estimated row stride`() {
        assertEquals(
            PlainLazyTarget(index = 4, scrollOffsetPx = 50),
            plainLazyTarget(
                lineCount = 10,
                playbackProgress = 0.50f,
                estimatedRowStridePx = 100,
            ),
        )
    }


    @Test
    fun `plain playback direction follows lazy scroll target`() {
        val target = PlainLazyTarget(index = 4, scrollOffsetPx = 50)

        assertEquals(
            PlaybackRegionDirection.BELOW,
            plainPlaybackRegionDirection(
                target = target,
                firstVisibleItemIndex = 3,
                firstVisibleItemScrollOffset = 0,
                viewportSizePx = 500,
            ),
        )
        assertEquals(
            PlaybackRegionDirection.ABOVE,
            plainPlaybackRegionDirection(
                target = target,
                firstVisibleItemIndex = 5,
                firstVisibleItemScrollOffset = 0,
                viewportSizePx = 500,
            ),
        )
        assertEquals(
            null,
            plainPlaybackRegionDirection(
                target = target,
                firstVisibleItemIndex = 4,
                firstVisibleItemScrollOffset = 40,
                viewportSizePx = 500,
            ),
        )
    }


}
