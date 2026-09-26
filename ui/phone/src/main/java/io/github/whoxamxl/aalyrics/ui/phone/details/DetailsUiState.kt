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

enum class DetailsTranslationRuntimeUiState {
    DISABLED,
    IDLE,
    TRANSLATING,
    NOT_REQUIRED,
    READY,
    FAILED,
}

enum class DetailsTranslationRuntimeFailureUiReason {
    LANGUAGE_PROFILING_FAILED,
    TRANSLATION_PLANNING_FAILED,
    PROVIDER_EXECUTION_FAILED,
    UNEXPECTED,
}

enum class DetailsTranslationModelPhaseUiState {
    UNSUPPORTED,
    NOT_REQUIRED,
    CHECKING,
    DOWNLOADING,
    WAITING_FOR_SYSTEM,
    READY,
    FAILED,
    TIMED_OUT,
}

@Immutable
data class DetailsTranslationModelUiState(
    val languageLabel: String,
    val phase: DetailsTranslationModelPhaseUiState,
    val failureReason: String? = null,
)

@Immutable
data class DetailsTranslationSourceModelsUiState(
    val primary: DetailsTranslationModelUiState,
    val secondary: DetailsTranslationModelUiState? = null,
)

@Immutable
data class DetailsTranslationUiState(
    val sourceLanguageLabel: String? = null,
    val targetLanguageLabel: String,
    val runtimeState: DetailsTranslationRuntimeUiState? = null,
    val runtimeFailureReason: DetailsTranslationRuntimeFailureUiReason? = null,
    val sourceModel: DetailsTranslationSourceModelsUiState? = null,
    val targetModel: DetailsTranslationModelUiState? = null,
)

@Immutable
data class DetailsVerboseProgressUiState(
    val playbackPositionLabel: String? = null,
    val currentLineNumber: Int? = null,
)

@Immutable
data class DetailsDiagnosticsUiState(
    val appPackageName: String? = null,
    val appCategory: String? = null,
    val appMinSdkVersion: Int? = null,
    val appTargetSdkVersion: Int? = null,
    val providerId: String? = null,
    val sourceId: String? = null,
    val trackReferences: List<String> = emptyList(),
    val lyricsLookupAttempt: Int? = null,
    val lyricsProviderFailures: List<String> = emptyList(),
)

@Immutable
data class DetailsScreenUiState(
    val track: DetailsTrackUiState? = null,
    val lyrics: DetailsLyricsUiState? = null,
    val lyricsStatus: DetailsLyricsUiStatus = DetailsLyricsUiStatus.UNAVAILABLE,
    val translation: DetailsTranslationUiState? = null,
    val verboseProgress: DetailsVerboseProgressUiState? = null,
    val diagnostics: DetailsDiagnosticsUiState? = null,
)
