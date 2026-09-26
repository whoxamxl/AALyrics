package io.github.whoxamxl.aalyrics

import io.github.whoxamxl.aalyrics.core.timing.LyricsTimingOffset
import kotlin.test.Test
import kotlin.test.assertEquals

class AndroidAutoLyricsTimingPolicyTest {
    @Test
    fun `projection connection applies fixed audio latency compensation`() {
        assertEquals(
            LyricsTimingOffset(-75L),
            lyricsTimingOffsetForProjection(isProjectionConnected = true),
        )
    }

    @Test
    fun `disconnected phone presentation keeps zero timing offset`() {
        assertEquals(
            LyricsTimingOffset.ZERO,
            lyricsTimingOffsetForProjection(isProjectionConnected = false),
        )
    }
}
