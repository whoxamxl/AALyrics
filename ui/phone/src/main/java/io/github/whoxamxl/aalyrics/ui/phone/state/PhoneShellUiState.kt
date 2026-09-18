package io.github.whoxamxl.aalyrics.ui.phone.state

import androidx.compose.runtime.Immutable
import io.github.whoxamxl.aalyrics.ui.phone.navigation.PhoneDestination

/** Presentation-only state for persistent Phone shell chrome. */
@Immutable
data class PhoneShellUiState(
    val selectedDestination: PhoneDestination = PhoneDestination.Home,
    val mediaSourceLabel: String? = null,
    val playbackControls: PlaybackControlsUiState? = null,
)

/** Presentation-only transport availability. Platform transport ownership stays outside UI. */
@Immutable
data class PlaybackControlsUiState(
    val isPlaying: Boolean,
    val previousEnabled: Boolean = true,
    val playPauseEnabled: Boolean = true,
    val nextEnabled: Boolean = true,
    /** Current track position as a normalized 0f..1f value; null when unavailable. */
    val progressFraction: Float? = null,
)
