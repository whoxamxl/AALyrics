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
}
