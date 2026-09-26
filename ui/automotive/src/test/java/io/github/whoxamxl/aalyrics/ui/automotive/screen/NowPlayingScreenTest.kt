package io.github.whoxamxl.aalyrics.ui.automotive.screen

import android.support.v4.media.session.PlaybackStateCompat
import io.github.whoxamxl.aalyrics.ui.automotive.AutomotiveTransportCapabilities
import kotlin.test.Test
import kotlin.test.assertEquals

class NowPlayingScreenTest {
    @Test
    fun `each transport action follows its source capability`() {
        val cases = listOf(
            AutomotiveTransportCapabilities(canPlay = true) to PlaybackStateCompat.ACTION_PLAY,
            AutomotiveTransportCapabilities(canPause = true) to PlaybackStateCompat.ACTION_PAUSE,
            AutomotiveTransportCapabilities(canSkipPrevious = true) to PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS,
            AutomotiveTransportCapabilities(canSkipNext = true) to PlaybackStateCompat.ACTION_SKIP_TO_NEXT,
            AutomotiveTransportCapabilities(canSeek = true) to PlaybackStateCompat.ACTION_SEEK_TO,
        )
        assertEquals(0L, NowPlayingScreen.actionMask(AutomotiveTransportCapabilities()))
        cases.forEach { (capabilities, expected) ->
            assertEquals(expected, NowPlayingScreen.actionMask(capabilities))
        }
    }

    @Test
    fun `toggle is advertised only when both play and pause are available`() {
        val actions = NowPlayingScreen.actionMask(
            AutomotiveTransportCapabilities(canPlay = true, canPause = true),
        )
        assertEquals(
            PlaybackStateCompat.ACTION_PLAY or
                PlaybackStateCompat.ACTION_PAUSE or
                PlaybackStateCompat.ACTION_PLAY_PAUSE,
            actions,
        )
    }
}
