package io.github.whoxamxl.aalyrics.platform.media

import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.PlaybackState
import io.github.whoxamxl.aalyrics.core.model.PlaybackSnapshot
import io.github.whoxamxl.aalyrics.core.model.PlaybackStatus

/**
 * Thin Android adapter that translates a MediaController into the shared
 * provider-independent playback model.
 */
object MediaControllerSnapshotAdapter {
    fun snapshot(controller: MediaController): PlaybackSnapshot {
        val metadata = controller.metadata
        val description = metadata?.description
        val playbackState = controller.playbackState

        val mediaId = metadata?.getString(MediaMetadata.METADATA_KEY_MEDIA_ID)
            ?: description?.mediaId
        val mediaUri = metadata?.getString(MediaMetadata.METADATA_KEY_MEDIA_URI)
            ?: description?.mediaUri?.toString()

        return MediaSessionSnapshotNormalizer.normalize(
            MediaSessionSnapshotInput(
                sourceId = controller.packageName,
                title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE),
                displayTitle = metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE),
                artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST),
                albumArtist = metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST),
                album = metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM),
                durationMs = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION),
                mediaId = mediaId,
                mediaUri = mediaUri,
                status = playbackStatus(playbackState?.state),
                positionMs = playbackState?.position ?: 0L,
                playbackRate = playbackState?.playbackSpeed ?: 1.0f,
                positionUpdatedAtMonotonicMs = playbackState
                    ?.lastPositionUpdateTime
                    ?.takeIf { it > 0L },
            ),
        )
    }

    private fun playbackStatus(state: Int?): PlaybackStatus = when (state) {
        PlaybackState.STATE_PLAYING -> PlaybackStatus.PLAYING
        PlaybackState.STATE_PAUSED -> PlaybackStatus.PAUSED
        PlaybackState.STATE_BUFFERING -> PlaybackStatus.BUFFERING
        PlaybackState.STATE_STOPPED -> PlaybackStatus.STOPPED
        else -> PlaybackStatus.IDLE
    }
}
