package io.github.whoxamxl.aalyrics.ui.phone.settings

import androidx.compose.runtime.Immutable

/** One presentation-ready Translation target shown by the Phone Settings surface. */
@Immutable
data class SettingsLanguageOptionUiState(
    val id: String,
    val displayName: String,
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
)
