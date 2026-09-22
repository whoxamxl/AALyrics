package io.github.whoxamxl.aalyrics

import io.github.whoxamxl.aalyrics.core.model.PlaybackSnapshot
import io.github.whoxamxl.aalyrics.platform.media.PlaybackControlState

/**
 * Returns true only when playback, selected-session control state, and resolved app metadata all
 * identify the same currently selected media-session package.
 */
internal fun isPlaybackSourceConnected(
    playback: PlaybackSnapshot,
    controlState: PlaybackControlState,
    playbackSourceAppInfo: PlaybackSourceAppInfo?,
): Boolean {
    val playbackPackageName = playback.source?.id ?: return false
    return controlState.sourcePackageName == playbackPackageName &&
        playbackSourceAppInfo?.packageName == playbackPackageName
}
