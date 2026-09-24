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
    IDLE,
    CHECKING,
    UP_TO_DATE,
    UPDATE_AVAILABLE,
    CHECK_FAILED,
    DOWNLOAD_FAILED,
    PREPARING_INSTALL,
    INSTALL_PERMISSION_REQUIRED,
    INSTALLING,
    INSTALL_FAILED,
}

enum class AppUpdateInstallFailureUiReason {
    DEPENDENCIES_UNAVAILABLE,
    RELEASE_REFRESH_FAILED,
    INSTALLED_VERSION_INVALID,
    RETAINED_VERSION_INVALID,
    RETAINED_RELEASE_NOT_ELIGIBLE,
    RETAINED_RELEASE_NOT_NEWER,
    NO_ELIGIBLE_RELEASE,
    RETAINED_RELEASE_NO_LONGER_CURRENT,
    APK_FILE_MISSING,
    APK_NOT_CANONICAL,
    APK_UNREADABLE,
    PACKAGE_MISMATCH,
    VERSION_NOT_NEWER,
    VERSION_NAME_MISMATCH,
    SIGNING_IDENTITY_UNAVAILABLE,
    SIGNING_IDENTITY_MISMATCH,
    RECOVERY_STATE_PERSISTENCE_FAILED,
    INSTALLER_HANDOFF_FAILED,
    INSTALLER_REJECTED,
}

@Immutable
data class AppUpdateUiState(
    val phase: AppUpdateUiPhase = AppUpdateUiPhase.IDLE,
    val availableVersionName: String? = null,
    val failureReason: String? = null,
    val installFailureReason: AppUpdateInstallFailureUiReason? = null,
)

@Immutable
data class InstallPermissionDialogUiState(
    val versionName: String,
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
    val ignoreNonAudioApps: Boolean = true,
    val allowUnclassifiedApps: Boolean = false,
    val automaticallyCheckForUpdates: Boolean = true,
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
    val privacyPolicyText: String = "",
    val termsOfUseText: String = "",
    val thirdPartyLicensesText: String = "",
    val appUpdate: AppUpdateUiState = AppUpdateUiState(),
    val installPermissionDialog: InstallPermissionDialogUiState? = null,
)
