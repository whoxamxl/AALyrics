package io.github.whoxamxl.aalyrics.ui.phone.component

import kotlin.test.Test
import kotlin.test.assertEquals

class TrackIdentityMarqueeTest {
    @Test
    fun `neither overflow stays static`() {
        assertEquals(
            TrackIdentityMarqueeMode.STATIC,
            trackIdentityMarqueeMode(
                titleOverflows = false,
                artistOverflows = false,
            ),
        )
        @Test
    fun `manual drag stays within one marquee cycle`() {
        assertEquals(
            180f,
            manualMarqueeOffsetPx(
                currentOffsetPx = 80f,
                dragDeltaPx = -100f,
                cycleDistancePx = 240f,
            ),
        )
        assertEquals(
            240f,
            manualMarqueeOffsetPx(
                currentOffsetPx = 180f,
                dragDeltaPx = -500f,
                cycleDistancePx = 240f,
            ),
        )
        assertEquals(
            0f,
            manualMarqueeOffsetPx(
                currentOffsetPx = 40f,
                dragDeltaPx = 500f,
                cycleDistancePx = 240f,
            ),
        )
    }

    @Test
    fun `marquee duration preserves constant velocity`() {
        assertEquals(
            2_000,
            marqueeTravelDurationMillis(
                distancePx = 120f,
                velocityPxPerSecond = 60f,
            ),
        )
    }
}

    @Test
    fun `title only overflow animates title only`() {
        assertEquals(
            TrackIdentityMarqueeMode.TITLE_ONLY,
            trackIdentityMarqueeMode(
                titleOverflows = true,
                artistOverflows = false,
            ),
        )
    }

    @Test
    fun `artist only overflow animates artist only`() {
        assertEquals(
            TrackIdentityMarqueeMode.ARTIST_ONLY,
            trackIdentityMarqueeMode(
                titleOverflows = false,
                artistOverflows = true,
            ),
        )
    }

    @Test
    fun `both overflow keep synchronized marquee`() {
        assertEquals(
            TrackIdentityMarqueeMode.SYNCHRONIZED,
            trackIdentityMarqueeMode(
                titleOverflows = true,
                artistOverflows = true,
            ),
        )
    }
}
