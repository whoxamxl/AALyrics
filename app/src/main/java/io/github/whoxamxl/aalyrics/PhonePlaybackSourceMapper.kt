package io.github.whoxamxl.aalyrics

import io.github.whoxamxl.aalyrics.core.model.PlaybackSnapshot
import io.github.whoxamxl.aalyrics.platform.media.PlaybackSourceErrorReason
import io.github.whoxamxl.aalyrics.platform.media.PlaybackSourceRuntimeState
import io.github.whoxamxl.aalyrics.ui.phone.state.PlaybackSourceConnectionUiState
import io.github.whoxamxl.aalyrics.ui.phone.state.PlaybackSourceErrorUiReason

internal fun playbackSourceAppInfoPackageName(
    runtimeState: PlaybackSourceRuntimeState,
    playback: PlaybackSnapshot,
): String? =
    when (runtimeState) {
        is PlaybackSourceRuntimeState.Connected -> runtimeState.packageName
        is PlaybackSourceRuntimeState.Unavailable ->
            runtimeState.packageName ?: playback.source?.id
        PlaybackSourceRuntimeState.Connecting -> playback.source?.id
        PlaybackSourceRuntimeState.Disconnected,
        is PlaybackSourceRuntimeState.Error -> null
    }

internal data class PhonePlaybackSourcePresentationState(
    val connectionState: PlaybackSourceConnectionUiState,
    val packageName: String? = null,
    val errorReason: PlaybackSourceErrorUiReason? = null,
)

internal fun mapPhonePlaybackSourcePresentationState(
    runtimeState: PlaybackSourceRuntimeState,
    playback: PlaybackSnapshot,
    playbackSourceAppInfo: PlaybackSourceAppInfo?,
): PhonePlaybackSourcePresentationState =
    when (runtimeState) {
        PlaybackSourceRuntimeState.Connecting ->
            PhonePlaybackSourcePresentationState(
                connectionState = PlaybackSourceConnectionUiState.CONNECTING,
            )

        is PlaybackSourceRuntimeState.Connected -> {
            val packageName = runtimeState.packageName
            if (
                playback.source?.id == packageName &&
                playbackSourceAppInfo?.packageName == packageName
            ) {
                PhonePlaybackSourcePresentationState(
                    connectionState = PlaybackSourceConnectionUiState.CONNECTED,
                    packageName = packageName,
                )
            } else {
                PhonePlaybackSourcePresentationState(
                    connectionState = PlaybackSourceConnectionUiState.CONNECTING,
                    packageName = packageName,
                )
            }
        }

        PlaybackSourceRuntimeState.Disconnected ->
            PhonePlaybackSourcePresentationState(
                connectionState = PlaybackSourceConnectionUiState.DISCONNECTED,
            )

        is PlaybackSourceRuntimeState.Unavailable ->
            PhonePlaybackSourcePresentationState(
                connectionState = PlaybackSourceConnectionUiState.UNAVAILABLE,
                packageName = runtimeState.packageName,
            )

        is PlaybackSourceRuntimeState.Error ->
            PhonePlaybackSourcePresentationState(
                connectionState = PlaybackSourceConnectionUiState.ERROR,
                errorReason = runtimeState.reason.toUiReason(),
            )
    }

private fun PlaybackSourceErrorReason.toUiReason(): PlaybackSourceErrorUiReason =
    when (this) {
        PlaybackSourceErrorReason.NOTIFICATION_ACCESS_LOST ->
            PlaybackSourceErrorUiReason.NOTIFICATION_ACCESS_LOST
        PlaybackSourceErrorReason.SESSION_QUERY_FAILED ->
            PlaybackSourceErrorUiReason.SESSION_QUERY_FAILED
        PlaybackSourceErrorReason.SESSION_ATTACH_FAILED ->
            PlaybackSourceErrorUiReason.SESSION_ATTACH_FAILED
        PlaybackSourceErrorReason.UNKNOWN ->
            PlaybackSourceErrorUiReason.UNKNOWN
    }
