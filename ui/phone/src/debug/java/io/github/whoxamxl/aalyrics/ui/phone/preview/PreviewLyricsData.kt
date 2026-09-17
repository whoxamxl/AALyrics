package io.github.whoxamxl.aalyrics.ui.phone.preview

import io.github.whoxamxl.aalyrics.ui.phone.navigation.PhoneDestination
import io.github.whoxamxl.aalyrics.ui.phone.state.PhoneShellUiState
import io.github.whoxamxl.aalyrics.ui.phone.state.PlaybackControlsUiState

/** Deterministic debug-only inputs for Phone shell previews. */
internal object PreviewPhoneData {
    val playingLyrics = PhoneShellUiState(
        statusText = "WORD SYNC",
        playbackControls = PlaybackControlsUiState(isPlaying = true),
    )

    val pausedLyrics = PhoneShellUiState(
        statusText = "LINE SYNC",
        playbackControls = PlaybackControlsUiState(isPlaying = false),
    )

    val syncDestination = PhoneShellUiState(
        selectedDestination = PhoneDestination.Sync,
        statusText = "PAUSED",
        playbackControls = PlaybackControlsUiState(isPlaying = false),
    )

    val disabledDetails = PhoneShellUiState(
        selectedDestination = PhoneDestination.Details,
        statusText = "NO CONTROL",
        playbackControls = PlaybackControlsUiState(
            isPlaying = false,
            previousEnabled = false,
            playPauseEnabled = false,
            nextEnabled = false,
        ),
    )

    val unavailableSettings = PhoneShellUiState(
        selectedDestination = PhoneDestination.Settings,
        statusText = "NO MEDIA",
    )

    val lyricsLines = listOf(
        "Streetlights wake along the avenue",
        "We carry the signal into the night",
        "Every window turns into a constellation",
        "Hold the rhythm while the city moves",
        "A quiet echo follows close behind",
        "Until the morning finds us here",
    )
}
