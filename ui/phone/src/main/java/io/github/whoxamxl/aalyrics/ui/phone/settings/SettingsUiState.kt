package io.github.whoxamxl.aalyrics.ui.phone.settings

import androidx.compose.runtime.Immutable

/** Presentation state for one Translation language model in the Settings picker. */
enum class TranslationModelUiState {
    BUILT_IN,
    CHECKING,
    NOT_DOWNLOADED,
    DOWNLOADING,
    READY,
    FAILED,
}

enum class TranslationModelCleanupUiState {
    IDLE,
    RUNNING,
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
    UNAVAILABLE,
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
        AppUpdateUiPhase.UNAVAILABLE,
        AppUpdateUiPhase.CHECKING,
        AppUpdateUiPhase.DOWNLOADING -> this
        else -> AppUpdateUiState()
    }

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
    val verboseDetailsEnabled: Boolean = false,
    val translationEnabled: Boolean = false,
    val translationTarget: SettingsLanguageOptionUiState,
    val translationTargets: List<SettingsLanguageOptionUiState>,
    val translationModelCleanup: TranslationModelCleanupUiState =
        TranslationModelCleanupUiState.IDLE,
    val androidAutoCompatibilityStatus: AndroidAutoCompatibilityUiStatus =
        AndroidAutoCompatibilityUiStatus.NOT_REVIEWED,
    val appVersionName: String,
    val currentYear: Int,
    val noticeText: String = "",
    val licenseText: String = "",
    val changelogText: String = "",
    val appUpdate: AppUpdateUiState = AppUpdateUiState(),
)
