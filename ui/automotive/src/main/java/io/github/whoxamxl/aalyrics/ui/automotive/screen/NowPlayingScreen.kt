package io.github.whoxamxl.aalyrics.ui.automotive.screen

import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat
import io.github.whoxamxl.aalyrics.core.model.PlaybackStatus
import io.github.whoxamxl.aalyrics.ui.automotive.state.AutomotiveLyricsUiState

internal object NowPlayingScreen {
    fun metadata(state: AutomotiveLyricsUiState): MediaMetadataCompat =
        MediaMetadataCompat.Builder().apply {
            state.trackTitle?.let { putString(MediaMetadataCompat.METADATA_KEY_TITLE, it) }
            state.artist?.let { putString(MediaMetadataCompat.METADATA_KEY_ARTIST, it) }
            state.album?.let { putString(MediaMetadataCompat.METADATA_KEY_ALBUM, it) }
            state.durationMs?.let { putLong(MediaMetadataCompat.METADATA_KEY_DURATION, it) }
            putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, state.displayTitle)
            putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, state.subtitle)
        }.build()

    fun playbackState(state: AutomotiveLyricsUiState): PlaybackStateCompat {
        val frameworkState = when (state.playbackStatus) {
            PlaybackStatus.PLAYING -> PlaybackStateCompat.STATE_PLAYING
            PlaybackStatus.PAUSED -> PlaybackStateCompat.STATE_PAUSED
            PlaybackStatus.BUFFERING -> PlaybackStateCompat.STATE_BUFFERING
            PlaybackStatus.STOPPED -> PlaybackStateCompat.STATE_STOPPED
            PlaybackStatus.IDLE -> PlaybackStateCompat.STATE_NONE
        }
        val speed = if (state.playbackStatus == PlaybackStatus.PLAYING) state.playbackRate else 0f

        return PlaybackStateCompat.Builder()
            .setState(frameworkState, state.positionMs, speed)
            .setActions(
                PlaybackStateCompat.ACTION_PLAY or
                    PlaybackStateCompat.ACTION_PAUSE or
                    PlaybackStateCompat.ACTION_PLAY_PAUSE or
                    PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                    PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                    PlaybackStateCompat.ACTION_SEEK_TO,
            )
            .build()
    }
}
