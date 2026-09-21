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
