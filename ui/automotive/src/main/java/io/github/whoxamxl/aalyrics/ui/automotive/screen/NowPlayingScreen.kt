package io.github.whoxamxl.aalyrics.ui.automotive.screen

import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat
import io.github.whoxamxl.aalyrics.core.model.PlaybackStatus
import io.github.whoxamxl.aalyrics.ui.automotive.state.AutomotiveLyricsUiState
import io.github.whoxamxl.aalyrics.ui.automotive.AutomotiveTransportCapabilities

internal object NowPlayingScreen {
    fun metadata(state: AutomotiveLyricsUiState): MediaMetadataCompat =
        MediaMetadataCompat.Builder().apply {
            state.trackTitle?.let { putString(MediaMetadataCompat.METADATA_KEY_TITLE, it) }
            state.artist?.let { putString(MediaMetadataCompat.METADATA_KEY_ARTIST, it) }
            state.album?.let { putString(MediaMetadataCompat.METADATA_KEY_ALBUM, it) }
            state.durationMs?.let { putLong(MediaMetadataCompat.METADATA_KEY_DURATION, it) }
            state.artwork?.let {
                putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, it)
                putBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, it)
            }
            putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, state.displayTitle)
            putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, state.subtitle)
            state.lyrics.secondaryText?.let {
                putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_DESCRIPTION, it)
            }
        }.build()

    fun playbackState(state: AutomotiveLyricsUiState): PlaybackStateCompat {
        val frameworkState = frameworkState(state.playbackStatus)
        val speed = if (state.playbackStatus == PlaybackStatus.PLAYING) state.playbackRate else 0f

        return PlaybackStateCompat.Builder()
            .setState(frameworkState, state.positionMs, speed)
            .setActions(actionMask(state.capabilities))
            .build()
    }

    fun frameworkState(status: PlaybackStatus): Int = when (status) {
            PlaybackStatus.PLAYING -> PlaybackStateCompat.STATE_PLAYING
            PlaybackStatus.PAUSED -> PlaybackStateCompat.STATE_PAUSED
            PlaybackStatus.BUFFERING -> PlaybackStateCompat.STATE_BUFFERING
            PlaybackStatus.STOPPED -> PlaybackStateCompat.STATE_STOPPED
            PlaybackStatus.IDLE -> PlaybackStateCompat.STATE_NONE
    }

    fun actionMask(capabilities: AutomotiveTransportCapabilities): Long {
        var actions = 0L
        if (capabilities.canPlay) actions = actions or PlaybackStateCompat.ACTION_PLAY
        if (capabilities.canPause) actions = actions or PlaybackStateCompat.ACTION_PAUSE
        if (capabilities.canPlay && capabilities.canPause) {
            actions = actions or PlaybackStateCompat.ACTION_PLAY_PAUSE
        }
        if (capabilities.canSkipPrevious) {
            actions = actions or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
        }
        if (capabilities.canSkipNext) actions = actions or PlaybackStateCompat.ACTION_SKIP_TO_NEXT
        if (capabilities.canSeek) actions = actions or PlaybackStateCompat.ACTION_SEEK_TO
        return actions
    }
}
