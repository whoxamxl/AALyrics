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
    fun `plain estimated max scroll accounts for viewport and boundary padding`() {
        assertEquals(
            725,
            estimatedPlainMaxScrollPx(
                lineCount = 10,
                estimatedRowHeightPx = 84,
                rowSpacingPx = 16,
                topContentPaddingPx = 75,
                bottomContentPaddingPx = 166,
                viewportHeightPx = 500,
            ),
        )
    }

    @Test
    fun `plain lazy target preserves lead in and reaches estimated max at lead out`() {
        assertEquals(
            PlainLazyTarget(index = 0, scrollOffsetPx = 0),
            plainLazyTarget(
                lineCount = 10,
                playbackProgress = 0.03f,
                estimatedRowStridePx = 100,
                estimatedMaxScrollPx = 725,
            ),
        )
        assertEquals(
            PlainLazyTarget(index = 7, scrollOffsetPx = 25),
            plainLazyTarget(
                lineCount = 10,
                playbackProgress = 0.95f,
                estimatedRowStridePx = 100,
                estimatedMaxScrollPx = 725,
            ),
        )
        assertEquals(
            PlainLazyTarget(index = 7, scrollOffsetPx = 25),
            plainLazyTarget(
                lineCount = 10,
                playbackProgress = 0.98f,
                estimatedRowStridePx = 100,
                estimatedMaxScrollPx = 725,
            ),
        )
    }

    @Test
    fun `plain lazy target does not reach end early on medium documents`() {
        assertEquals(
            PlainLazyTarget(index = 5, scrollOffsetPx = 88),
            plainLazyTarget(
                lineCount = 10,
                playbackProgress = 0.78f,
                estimatedRowStridePx = 100,
                estimatedMaxScrollPx = 725,
            ),
        )
    }

    @Test
    fun `plain lazy target advances within estimated scroll extent`() {
        assertEquals(
            PlainLazyTarget(index = 3, scrollOffsetPx = 63),
            plainLazyTarget(
                lineCount = 10,
                playbackProgress = 0.50f,
                estimatedRowStridePx = 100,
                estimatedMaxScrollPx = 725,
            ),
        )
    }


    @Test
    fun `plain playback direction uses continuous visible item geometry`() {
        val target = PlainLazyTarget(index = 4, scrollOffsetPx = 50)

        assertEquals(
            PlaybackRegionDirection.BELOW,
            plainPlaybackRegionDirection(
                target = target,
                visibleItems = listOf(
                    LazyViewportItemGeometry(index = 3, offset = -80, size = 80),
                    LazyViewportItemGeometry(index = 4, offset = 100, size = 80),
                ),
                viewportStartOffset = 0,
                viewportEndOffset = 500,
            ),
        )
        assertEquals(
            PlaybackRegionDirection.ABOVE,
            plainPlaybackRegionDirection(
                target = target,
                visibleItems = listOf(
                    LazyViewportItemGeometry(index = 4, offset = -130, size = 80),
                    LazyViewportItemGeometry(index = 5, offset = -34, size = 80),
                ),
                viewportStartOffset = 0,
                viewportEndOffset = 500,
            ),
        )
        assertEquals(
            null,
            plainPlaybackRegionDirection(
                target = target,
                visibleItems = listOf(
                    LazyViewportItemGeometry(index = 3, offset = -76, size = 80),
                    LazyViewportItemGeometry(index = 4, offset = 20, size = 80),
                ),
                viewportStartOffset = 0,
                viewportEndOffset = 500,
            ),
        )
    }

    @Test
    fun `browse does not rearm follow before lazy geometry exists`() {
        assertEquals(
            false,
            lazyViewportGeometryReady(
                visibleItems = emptyList(),
                viewportStartOffset = 0,
                viewportEndOffset = 0,
            ),
        )
        assertEquals(
            false,
            shouldRearmFollow(
                interactionMode = LyricsViewportInteractionMode.BROWSE,
                hasPlaybackTarget = true,
                geometryReady = false,
                returnDirection = null,
                isScrollInProgress = false,
            ),
        )
    }

    @Test
    fun `browse rearms follow after valid geometry settles inside playback region`() {
        assertEquals(
            true,
            lazyViewportGeometryReady(
                visibleItems = listOf(
                    LazyViewportItemGeometry(index = 4, offset = 20, size = 80),
                ),
                viewportStartOffset = 0,
                viewportEndOffset = 500,
            ),
        )
        assertEquals(
            true,
            shouldRearmFollow(
                interactionMode = LyricsViewportInteractionMode.BROWSE,
                hasPlaybackTarget = true,
                geometryReady = true,
                returnDirection = null,
                isScrollInProgress = false,
            ),
        )
    }


    @Test
    fun `opening padding keeps virtual focus row before the top fade boundary`() {
        assertEquals(
            30,
            lazyTopContentPaddingPx(
                openingContentStartPx = 100,
                openingFocusRowHeightPx = 50,
                rowSpacingPx = 20,
                minimumContentPaddingPx = 16,
                hasOpeningFocusRow = true,
            ),
        )
        assertEquals(
            100,
            lazyTopContentPaddingPx(
                openingContentStartPx = 100,
                openingFocusRowHeightPx = 0,
                rowSpacingPx = 20,
                minimumContentPaddingPx = 16,
                hasOpeningFocusRow = false,
            ),
        )
    }

    @Test
    fun `final row bottom padding preserves the 50 percent boundary with a floor`() {
        assertEquals(
            170,
            lazyBottomContentPaddingPx(
                endingBoundaryStartPx = 250,
                lastLineHeightPx = 80,
                minimumContentPaddingPx = 20,
            ),
        )
        assertEquals(
            20,
            lazyBottomContentPaddingPx(
                endingBoundaryStartPx = 250,
                lastLineHeightPx = 300,
                minimumContentPaddingPx = 20,
            ),
        )
    }

    @Test
    fun `lazy focus uses measured variable row heights instead of uniform estimates`() {
        assertEquals(
            -47f,
            lazyFocusScrollDelta(
                focusIndex = 4.5f,
                visibleItems = listOf(
                    LazyViewportItemGeometry(index = 4, offset = 80, size = 60),
                    LazyViewportItemGeometry(index = 5, offset = 156, size = 180),
                ),
                viewportStartOffset = 0,
                viewportEndOffset = 500,
            ),
        )
    }

    @Test
    fun `fractional lazy focus waits until both adjacent items are materialized`() {
        assertEquals(
            null,
            lazyFocusScrollDelta(
                focusIndex = 20.5f,
                visibleItems = listOf(
                    LazyViewportItemGeometry(index = 20, offset = 120, size = 80),
                ),
                viewportStartOffset = 0,
                viewportEndOffset = 500,
            ),
        )
    }

    @Test
    fun `timed playback direction accepts a visible row inside focus tolerance`() {
        assertEquals(
            null,
            timedPlaybackRegionDirection(
                targetIndex = 6,
                visibleItems = listOf(
                    LazyViewportItemGeometry(index = 6, offset = 185, size = 80),
                ),
                viewportStartOffset = 0,
                viewportEndOffset = 500,
            ),
        )
    }

    @Test
    fun `plain target rejects unusable document inputs`() {
        assertEquals(null, plainLazyTarget(0, 0.5f, 100, 725))
        assertEquals(null, plainLazyTarget(10, null, 100, 725))
        assertEquals(null, plainLazyTarget(10, Float.NaN, 100, 725))
        assertEquals(null, plainLazyTarget(10, 0.5f, 0, 725))
        assertEquals(null, plainLazyTarget(10, 0.5f, 100, 0))
    }


}
