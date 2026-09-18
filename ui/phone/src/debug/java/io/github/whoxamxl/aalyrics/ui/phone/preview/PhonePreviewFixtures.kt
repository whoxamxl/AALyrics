package io.github.whoxamxl.aalyrics.ui.phone.preview

import io.github.whoxamxl.aalyrics.ui.phone.navigation.PhoneDestination
import io.github.whoxamxl.aalyrics.ui.phone.state.PhoneShellUiState
import io.github.whoxamxl.aalyrics.ui.phone.state.PlaybackControlsUiState

/** Deterministic debug-only inputs shared by Phone shell component Previews. */
internal object PhonePreviewFixtures {
    val playingControls = PlaybackControlsUiState(
        isPlaying = true,
        progressFraction = 0.42f,
    )
    val pausedControls = PlaybackControlsUiState(
        isPlaying = false,
        progressFraction = 0.61f,
    )
    val previousDisabledControls = PlaybackControlsUiState(
        isPlaying = true,
        previousEnabled = false,
        progressFraction = 0.08f,
    )
    val nextDisabledControls = PlaybackControlsUiState(
        isPlaying = false,
        nextEnabled = false,
        progressFraction = 0.93f,
    )
    val allDisabledControls = PlaybackControlsUiState(
        isPlaying = false,
        previousEnabled = false,
        playPauseEnabled = false,
        nextEnabled = false,
        progressFraction = 0.37f,
    )

    val typicalLyricsShell = PhoneShellUiState(
        mediaSourceLabel = "Spotify",
        playbackControls = playingControls,
    )
    val narrowLyricsShell = PhoneShellUiState(
        mediaSourceLabel = "YouTube Music",
        playbackControls = pausedControls,
    )
    val lyricsWithoutControls = PhoneShellUiState(mediaSourceLabel = null)
    val syncShell = PhoneShellUiState(
        selectedDestination = PhoneDestination.Sync,
        mediaSourceLabel = "Poweramp",
        playbackControls = pausedControls,
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
