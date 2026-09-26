package io.github.whoxamxl.aalyrics.ui.automotive.screen

import android.support.v4.media.session.PlaybackStateCompat
import io.github.whoxamxl.aalyrics.ui.automotive.AutomotiveTransportCapabilities
import io.github.whoxamxl.aalyrics.core.model.PlaybackStatus
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

    @Test
    fun `playback status maps to its real media session state`() {
        assertEquals(PlaybackStateCompat.STATE_PLAYING, NowPlayingScreen.frameworkState(PlaybackStatus.PLAYING))
        assertEquals(PlaybackStateCompat.STATE_PAUSED, NowPlayingScreen.frameworkState(PlaybackStatus.PAUSED))
        assertEquals(PlaybackStateCompat.STATE_BUFFERING, NowPlayingScreen.frameworkState(PlaybackStatus.BUFFERING))
        assertEquals(PlaybackStateCompat.STATE_STOPPED, NowPlayingScreen.frameworkState(PlaybackStatus.STOPPED))
        assertEquals(PlaybackStateCompat.STATE_NONE, NowPlayingScreen.frameworkState(PlaybackStatus.IDLE))
    }
}
