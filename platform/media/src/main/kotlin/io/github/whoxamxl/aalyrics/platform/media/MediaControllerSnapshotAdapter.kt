package io.github.whoxamxl.aalyrics.platform.media

import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.PlaybackState
import android.os.SystemClock
import android.util.Log
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
        val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
        val displayTitle = metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
        val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
        val albumArtist = metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
        val displaySubtitle = metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE)
        val album = metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM)

        Log.d(
            METADATA_LOG_TAG,
            "package=${controller.packageName} | " +
                "TITLE=$title | " +
                "ARTIST=$artist | " +
                "ALBUM_ARTIST=$albumArtist | " +
                "DISPLAY_TITLE=$displayTitle | " +
                "DISPLAY_SUBTITLE=$displaySubtitle | " +
                "ALBUM=$album | " +
                "DESCRIPTION_TITLE=${description?.title} | " +
                "DESCRIPTION_SUBTITLE=${description?.subtitle} | " +
                "DESCRIPTION_DESCRIPTION=${description?.description}",
        )

        val mediaId = metadata?.getString(MediaMetadata.METADATA_KEY_MEDIA_ID)
            ?: description?.mediaId
        val mediaUri = metadata?.getString(MediaMetadata.METADATA_KEY_MEDIA_URI)
            ?: description?.mediaUri?.toString()

        return MediaSessionSnapshotNormalizer.normalize(
            MediaSessionSnapshotInput(
                sourceId = controller.packageName,
                title = title,
                displayTitle = displayTitle,
                artist = artist,
                albumArtist = albumArtist,
                album = album,
                durationMs = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION),
                mediaId = mediaId,
                mediaUri = mediaUri,
                status = playbackStatus(playbackState?.state),
                positionMs = playbackState?.position ?: 0L,
                playbackRate = playbackState?.playbackSpeed ?: 1.0f,
                positionUpdatedAtMonotonicMs = playbackState
                    ?.lastPositionUpdateTime
                    ?.takeIf { it > 0L },
                positionSampledAtMonotonicMs = SystemClock.elapsedRealtime(),
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

    private const val METADATA_LOG_TAG = "AALyricsMetadata"
}
