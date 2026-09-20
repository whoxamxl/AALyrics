package io.github.whoxamxl.aalyrics.ui.phone.shell

import kotlin.test.Test
import kotlin.test.assertEquals

class PlaybackSeekTest {
    @Test
    fun `backward relative seek moves five media seconds per hold second`() {
        assertEquals(
            25_000L,
            relativeSeekPreviewPositionMs(
                startPositionMs = 30_000L,
                durationMs = 180_000L,
                heldAfterLongPressMs = 1_000L,
                direction = RelativeSeekDirection.BACKWARD,
            ),
        )
    }

    @Test
    fun `forward relative seek moves five media seconds per hold second`() {
        assertEquals(
            35_000L,
            relativeSeekPreviewPositionMs(
                startPositionMs = 30_000L,
                durationMs = 180_000L,
                heldAfterLongPressMs = 1_000L,
                direction = RelativeSeekDirection.FORWARD,
            ),
        )
    }

    @Test
    fun `relative seek clamps at start and duration`() {
        assertEquals(
            0L,
            relativeSeekPreviewPositionMs(
                startPositionMs = 2_000L,
                durationMs = 180_000L,
                heldAfterLongPressMs = 1_000L,
                direction = RelativeSeekDirection.BACKWARD,
            ),
        )
        assertEquals(
            180_000L,
            relativeSeekPreviewPositionMs(
                startPositionMs = 178_000L,
                durationMs = 180_000L,
                heldAfterLongPressMs = 1_000L,
                direction = RelativeSeekDirection.FORWARD,
            ),
        )
    }

    @Test
    fun `playback time formatting handles minutes and hours`() {
        assertEquals("0:00", formatPlaybackTime(0L))
        assertEquals("2:05", formatPlaybackTime(125_000L))
        assertEquals("1:02:03", formatPlaybackTime(3_723_000L))
    }

    @Test
    fun `zero additional hold preserves starting position`() {
        assertEquals(
            30_000L,
            relativeSeekPreviewPositionMs(
                startPositionMs = 30_000L,
                durationMs = 180_000L,
                heldAfterLongPressMs = 0L,
                direction = RelativeSeekDirection.FORWARD,
            ),
        )
    }
}
