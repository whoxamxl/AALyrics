package io.github.whoxamxl.aalyrics.ui.phone.settings

import androidx.compose.runtime.Immutable

/** Presentation state for one Translation language model in the Settings picker. */
enum class TranslationModelUiState {
    BUILT_IN,
    NOT_DOWNLOADED,
    DOWNLOADING,
    READY,
    FAILED,
}

/** One presentation-ready Translation target shown by the Phone Settings surface. */
@Immutable
data class SettingsLanguageOptionUiState(
    val id: String,
    val displayName: String,
    val modelState: TranslationModelUiState = TranslationModelUiState.NOT_DOWNLOADED,
)

/** User acknowledgement shown for the legacy Android Auto compatibility setup. */
enum class AndroidAutoCompatibilityUiStatus {
    NOT_REVIEWED,
    ENABLED,
    SKIPPED,
}

/** Presentation-ready state consumed by [SettingsScreen]. */
@Immutable
data class SettingsScreenUiState(
    val plainLyricsAutoScrollEnabled: Boolean = true,
    val translationEnabled: Boolean = true,
    val translationTarget: SettingsLanguageOptionUiState,
    val translationTargets: List<SettingsLanguageOptionUiState>,
    val androidAutoCompatibilityStatus: AndroidAutoCompatibilityUiStatus =
        AndroidAutoCompatibilityUiStatus.NOT_REVIEWED,
    val appVersionName: String,
)
