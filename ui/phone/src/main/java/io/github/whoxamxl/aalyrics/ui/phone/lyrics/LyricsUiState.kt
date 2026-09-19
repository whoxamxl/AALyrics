package io.github.whoxamxl.aalyrics.ui.phone.lyrics

import androidx.compose.runtime.Immutable

/** Presentation-ready current-track identity shown at the top of the Lyrics destination. */
@Immutable
data class TrackCardUiState(
    val title: String,
    val artist: String? = null,
    val providerLabel: String? = null,
    val syncLabel: String? = null,
)
