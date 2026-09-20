package io.github.whoxamxl.aalyrics.ui.phone.details

import androidx.compose.runtime.Immutable
import io.github.whoxamxl.aalyrics.core.model.LyricsSyncType

enum class DetailsLyricsUiStatus {
    LOADING,
    READY,
    NOT_FOUND,
    FAILED,
    UNAVAILABLE,
}

@Immutable
data class DetailsTrackUiState(
    val title: String,
    val artist: String? = null,
    val album: String? = null,
    val durationLabel: String? = null,
    val playbackSourceLabel: String? = null,
)

@Immutable
data class DetailsLyricsUiState(
    val providerDisplayName: String? = null,
    val syncType: LyricsSyncType,
    val languageLabel: String? = null,
    val lineCount: Int,
)

@Immutable
data class DetailsDiagnosticsUiState(
    val providerId: String? = null,
    val sourceId: String? = null,
    val trackReferences: List<String> = emptyList(),
)

@Immutable
data class DetailsScreenUiState(
    val track: DetailsTrackUiState? = null,
    val lyrics: DetailsLyricsUiState? = null,
    val lyricsStatus: DetailsLyricsUiStatus = DetailsLyricsUiStatus.UNAVAILABLE,
    val diagnostics: DetailsDiagnosticsUiState? = null,
)
