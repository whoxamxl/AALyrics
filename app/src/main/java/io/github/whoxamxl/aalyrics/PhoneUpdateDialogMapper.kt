package io.github.whoxamxl.aalyrics

import io.github.whoxamxl.aalyrics.ui.phone.update.UpdateDialogPhase
import io.github.whoxamxl.aalyrics.ui.phone.update.UpdateDialogUiState
import io.github.whoxamxl.aalyrics.ui.phone.update.UpdateInstallFailureUiReason

internal fun mapPhoneUpdateDialogState(
    updateState: AppUpdateCheckState,
    releasePrompt: UpdateReleasePrompt?,
): UpdateDialogUiState? {
    releasePrompt?.let { prompt ->
        return UpdateDialogUiState(
            phase = UpdateDialogPhase.AVAILABLE,
            versionName = prompt.versionName,
        )
    }

    return when (updateState) {
        is AppUpdateCheckState.PreparingDownload ->
            UpdateDialogUiState(
                phase = UpdateDialogPhase.PREPARING_DOWNLOAD,
                versionName = updateState.versionName,
            )

        is AppUpdateCheckState.Downloading ->
            UpdateDialogUiState(
                phase = UpdateDialogPhase.DOWNLOADING,
                versionName = updateState.versionName,
                downloadProgress = if (updateState.totalBytes > 0L) {
                    (
                        updateState.downloadedBytes.toDouble() /
                            updateState.totalBytes.toDouble()
                        )
                        .coerceIn(0.0, 1.0)
                        .toFloat()
                } else {
                    0f
                },
            )

        is AppUpdateCheckState.VerifyingDownload ->
            UpdateDialogUiState(
                phase = UpdateDialogPhase.VERIFYING_DOWNLOAD,
                versionName = updateState.versionName,
            )

        is AppUpdateCheckState.Downloaded ->
            UpdateDialogUiState(
                phase = UpdateDialogPhase.DOWNLOADED,
                versionName = updateState.versionName,
            )

        is AppUpdateCheckState.DownloadFailed ->
            UpdateDialogUiState(
                phase = UpdateDialogPhase.DOWNLOAD_FAILED,
                versionName = updateState.versionName,
            )

        is AppUpdateCheckState.PreparingInstall ->
            UpdateDialogUiState(
                phase = UpdateDialogPhase.PREPARING_INSTALL,
                versionName = updateState.versionName,
            )

        is AppUpdateCheckState.InstallPermissionRequired ->
            UpdateDialogUiState(
                phase = UpdateDialogPhase.INSTALL_PERMISSION_REQUIRED,
                versionName = updateState.versionName,
            )

        is AppUpdateCheckState.Installing ->
            UpdateDialogUiState(
                phase = UpdateDialogPhase.INSTALLING,
                versionName = updateState.versionName,
            )

        is AppUpdateCheckState.InstallFailed ->
            UpdateDialogUiState(
                phase = UpdateDialogPhase.INSTALL_FAILED,
                versionName = updateState.versionName,
                installFailureReason = updateState.reason.toUiReason(),
            )

        AppUpdateCheckState.Idle,
        is AppUpdateCheckState.Checking,
        is AppUpdateCheckState.UpToDate,
        is AppUpdateCheckState.UpdateAvailable,
        is AppUpdateCheckState.Failed -> null
    }
}

private fun AppUpdateInstallFailureReason.toUiReason(): UpdateInstallFailureUiReason =
    when (this) {
        AppUpdateInstallFailureReason.DEPENDENCIES_UNAVAILABLE ->
            UpdateInstallFailureUiReason.DEPENDENCIES_UNAVAILABLE
        AppUpdateInstallFailureReason.RELEASE_REFRESH_FAILED ->
            UpdateInstallFailureUiReason.RELEASE_REFRESH_FAILED
        AppUpdateInstallFailureReason.INSTALLED_VERSION_INVALID ->
            UpdateInstallFailureUiReason.INSTALLED_VERSION_INVALID
        AppUpdateInstallFailureReason.RETAINED_VERSION_INVALID ->
            UpdateInstallFailureUiReason.RETAINED_VERSION_INVALID
        AppUpdateInstallFailureReason.RETAINED_RELEASE_NOT_ELIGIBLE ->
            UpdateInstallFailureUiReason.RETAINED_RELEASE_NOT_ELIGIBLE
        AppUpdateInstallFailureReason.RETAINED_RELEASE_NOT_NEWER ->
            UpdateInstallFailureUiReason.RETAINED_RELEASE_NOT_NEWER
        AppUpdateInstallFailureReason.NO_ELIGIBLE_RELEASE ->
            UpdateInstallFailureUiReason.NO_ELIGIBLE_RELEASE
        AppUpdateInstallFailureReason.RETAINED_RELEASE_NO_LONGER_CURRENT ->
            UpdateInstallFailureUiReason.RETAINED_RELEASE_NO_LONGER_CURRENT
        AppUpdateInstallFailureReason.APK_FILE_MISSING ->
            UpdateInstallFailureUiReason.APK_FILE_MISSING
        AppUpdateInstallFailureReason.APK_NOT_CANONICAL ->
            UpdateInstallFailureUiReason.APK_NOT_CANONICAL
        AppUpdateInstallFailureReason.APK_UNREADABLE ->
            UpdateInstallFailureUiReason.APK_UNREADABLE
        AppUpdateInstallFailureReason.PACKAGE_MISMATCH ->
            UpdateInstallFailureUiReason.PACKAGE_MISMATCH
        AppUpdateInstallFailureReason.VERSION_NOT_NEWER ->
            UpdateInstallFailureUiReason.VERSION_NOT_NEWER
        AppUpdateInstallFailureReason.VERSION_NAME_MISMATCH ->
            UpdateInstallFailureUiReason.VERSION_NAME_MISMATCH
        AppUpdateInstallFailureReason.SIGNING_IDENTITY_UNAVAILABLE ->
            UpdateInstallFailureUiReason.SIGNING_IDENTITY_UNAVAILABLE
        AppUpdateInstallFailureReason.SIGNING_IDENTITY_MISMATCH ->
            UpdateInstallFailureUiReason.SIGNING_IDENTITY_MISMATCH
        AppUpdateInstallFailureReason.RECOVERY_STATE_PERSISTENCE_FAILED ->
            UpdateInstallFailureUiReason.RECOVERY_STATE_PERSISTENCE_FAILED
        AppUpdateInstallFailureReason.INSTALLER_HANDOFF_FAILED ->
            UpdateInstallFailureUiReason.INSTALLER_HANDOFF_FAILED
        AppUpdateInstallFailureReason.INSTALLER_REJECTED ->
            UpdateInstallFailureUiReason.INSTALLER_REJECTED
    }
