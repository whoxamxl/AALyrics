package io.github.whoxamxl.aalyrics

import io.github.whoxamxl.aalyrics.core.model.PlaybackSnapshot
import io.github.whoxamxl.aalyrics.platform.media.PlaybackControlState
import io.github.whoxamxl.aalyrics.ui.phone.state.PlaybackQueueItemUiState
import io.github.whoxamxl.aalyrics.ui.phone.state.PlaybackSurfaceUiState

/** Application-owned mapping from playback/runtime state into Phone presentation state. */
internal fun mapPhonePlaybackSurfaceState(
    playback: PlaybackSnapshot,
    controlState: PlaybackControlState,
    translationEnabled: Boolean,
    canLaunchSourcePackage: Boolean,
): PlaybackSurfaceUiState? {
    val track = playback.track ?: return null
    val sourcePackageName = playback.source?.id
    val controlsMatchPlayback = sourcePackageName != null &&
        controlState.sourcePackageName == sourcePackageName
    val capabilities = controlState.capabilities.takeIf { controlsMatchPlayback }

    return PlaybackSurfaceUiState(
        isPlaying = playback.isPlaying,
        title = track.title,
        artist = track.artists
            .takeIf { it.isNotEmpty() }
            ?.joinToString(separator = ", "),
        positionMs = playback.positionMs,
        durationMs = track.durationMs,
        playbackRate = playback.playbackRate,
        positionUpdatedAtMonotonicMs = playback.positionUpdatedAtMonotonicMs,
        canPlay = capabilities?.canPlay == true,
        canPause = capabilities?.canPause == true,
        canSkipPrevious = capabilities?.canSkipPrevious == true,
        canSkipNext = capabilities?.canSkipNext == true,
        canSeek = capabilities?.canSeek == true,
        queue = if (controlsMatchPlayback) {
            controlState.queue.map { item ->
                PlaybackQueueItemUiState(
                    id = item.id,
                    title = item.title,
                    subtitle = item.subtitle,
                )
            }
        } else {
            emptyList()
        },
        canOpenPlaybackApp = controlsMatchPlayback &&
            (controlState.hasSessionActivity || canLaunchSourcePackage),
        translationEnabled = translationEnabled,
    )
}
