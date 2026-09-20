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
    val modelFailureReason: String? = null,
)

/** Presentation lifecycle for checking/downloading an AALyrics GitHub Release update. */
enum class AppUpdateUiPhase {
    IDLE,
    CHECKING,
    UP_TO_DATE,
    UPDATE_AVAILABLE,
    CHECK_FAILED,
    DOWNLOADING,
    DOWNLOADED,
    DOWNLOAD_FAILED,
}

@Immutable
data class AppUpdateUiState(
    val phase: AppUpdateUiPhase = AppUpdateUiPhase.IDLE,
    val availableVersionName: String? = null,
    val failureReason: String? = null,
)

/**
 * Normalizes transient update results when the user enters Settings again.
 *
 * Active work survives destination changes. Completed/stale results do not.
 */
fun AppUpdateUiState.normalizedForSettingsEntry(): AppUpdateUiState =
    when (phase) {
        AppUpdateUiPhase.CHECKING,
        AppUpdateUiPhase.DOWNLOADING -> this
        else -> AppUpdateUiState()
    }

enum class ChangelogUiPhase {
    IDLE,
    LOADING,
    READY,
    FAILED,
}

@Immutable
data class ChangelogUiState(
    val phase: ChangelogUiPhase = ChangelogUiPhase.IDLE,
    val releaseVersionName: String? = null,
    val body: String? = null,
    val failureReason: String? = null,
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
    val currentYear: Int,
    val appUpdate: AppUpdateUiState = AppUpdateUiState(),
    val changelog: ChangelogUiState = ChangelogUiState(),
)
