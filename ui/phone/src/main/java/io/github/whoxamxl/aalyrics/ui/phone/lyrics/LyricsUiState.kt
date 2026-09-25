package io.github.whoxamxl.aalyrics.ui.phone.lyrics

import androidx.compose.runtime.Immutable
import io.github.whoxamxl.aalyrics.core.model.LyricsSyncType

enum class TrackCardLyricsStatus {
    IDLE,
    LOADING,
    READY,
    NOT_FOUND,
    FAILED,
}

/** Presentation-only Translation status reserved as the fourth Track Card row. */
@Immutable
sealed interface TrackCardTranslationUiState {
    data object Off : TrackCardTranslationUiState
    data object On : TrackCardTranslationUiState
    data object DownloadingModels : TrackCardTranslationUiState
    data object Translating : TrackCardTranslationUiState
    data object NotRequired : TrackCardTranslationUiState
    data object Failed : TrackCardTranslationUiState

    data class Ready(
        val sourceLanguageLabel: String,
        val targetLanguageLabel: String,
    ) : TrackCardTranslationUiState {
        init {
            require(sourceLanguageLabel.isNotBlank()) { "Source language label must not be blank" }
            require(targetLanguageLabel.isNotBlank()) { "Target language label must not be blank" }
        }
    }
}

/** Presentation-ready current-track identity shown at the top of the Lyrics destination. */
@Immutable
data class TrackCardUiState(
    val title: String,
    val artist: String? = null,
    val providerLabel: String? = null,
    val syncLabel: String? = null,
    val lyricsStatus: TrackCardLyricsStatus = TrackCardLyricsStatus.IDLE,
    val translation: TrackCardTranslationUiState = TrackCardTranslationUiState.Off,
)

/** Which owner currently controls the vertical position of the lyrics viewport. */
enum class LyricsViewportInteractionMode {
    FOLLOW,
    BROWSE,
}

/** One presentation-ready lyric row. Word tokens are populated only when karaoke timing is available. */
@Immutable
data class LyricsViewportLineUiState(
    val text: String,
    val words: List<String> = emptyList(),
    val translatedText: String? = null,
)

/**
 * Presentation contract consumed by [LyricsViewport].
 *
 * Runtime mapping supplies authoritative timing/progress. The viewport owns only rendering and
 * scroll interaction; it does not inspect providers or Android media-session objects.
 */
@Immutable
data class LyricsViewportUiState(
    val lines: List<LyricsViewportLineUiState>,
    val syncType: LyricsSyncType,
    val currentLineIndex: Int? = null,
    val currentWordIndex: Int? = null,
    val currentWordProgress: Float = 0f,
    val playbackProgress: Float? = null,
    val plainAutoScrollEnabled: Boolean = true,
    val interactionMode: LyricsViewportInteractionMode = LyricsViewportInteractionMode.FOLLOW,
)


/** Presentation-ready state for the complete Phone Lyrics destination. */
@Immutable
data class LyricsScreenUiState(
    val trackCard: TrackCardUiState,
    val viewport: LyricsViewportUiState,
)
