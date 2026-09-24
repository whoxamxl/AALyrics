package io.github.whoxamxl.aalyrics.ui.phone.update

import androidx.compose.runtime.Immutable

enum class UpdateDialogPhase {
    AVAILABLE,
    PREPARING_DOWNLOAD,
    DOWNLOADING,
    VERIFYING_DOWNLOAD,
    DOWNLOADED,
    DOWNLOAD_FAILED,
    PREPARING_INSTALL,
    INSTALL_PERMISSION_REQUIRED,
    INSTALLING,
    INSTALL_FAILED,
}

enum class UpdateInstallFailureUiReason {
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
data class UpdateDialogUiState(
    val phase: UpdateDialogPhase,
    val versionName: String,
    val downloadProgress: Float? = null,
    val installFailureReason: UpdateInstallFailureUiReason? = null,
)
