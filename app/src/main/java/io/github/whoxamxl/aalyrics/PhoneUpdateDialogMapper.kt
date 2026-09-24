package io.github.whoxamxl.aalyrics

import io.github.whoxamxl.aalyrics.ui.phone.update.UpdateDialogInstallFailureUiReason
import io.github.whoxamxl.aalyrics.ui.phone.update.UpdateDialogPhase
import io.github.whoxamxl.aalyrics.ui.phone.update.UpdateDialogUiState

internal fun mapPhoneUpdateDialogState(
    updateState: AppUpdateCheckState,
    releasePrompt: UpdateReleasePrompt? = null,
): UpdateDialogUiState? {
    val processState = when (updateState) {
        AppUpdateCheckState.Idle,
        is AppUpdateCheckState.Checking,
        is AppUpdateCheckState.UpToDate,
        is AppUpdateCheckState.Failed -> null

        is AppUpdateCheckState.UpdateAvailable ->
            if (updateState.origin == UpdateCheckOrigin.INSTALL_REFRESH) {
                UpdateDialogUiState(
                    phase = UpdateDialogPhase.AVAILABLE,
                    versionName = updateState.versionName,
                )
            } else {
                null
            }

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
                phase = UpdateDialogPhase.VERIFYING,
                versionName = updateState.versionName,
            )

        is AppUpdateCheckState.Downloaded ->
            UpdateDialogUiState(
                phase = UpdateDialogPhase.READY_TO_INSTALL,
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
                phase = UpdateDialogPhase.PERMISSION_REQUIRED,
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
                installFailureReason = updateState.reason.toUpdateDialogReason(),
            )
    }

    if (processState != null) {
        return processState
    }

    return releasePrompt?.let { prompt ->
        val available = updateState as? AppUpdateCheckState.UpdateAvailable
        if (
            available == null ||
            available.origin == UpdateCheckOrigin.INSTALL_REFRESH ||
            available.versionName != prompt.versionName ||
            available.origin != prompt.origin
        ) {
            null
        } else {
            UpdateDialogUiState(
                phase = UpdateDialogPhase.AVAILABLE,
                versionName = prompt.versionName,
            )
        }
    }
}

private fun AppUpdateInstallFailureReason.toUpdateDialogReason():
    UpdateDialogInstallFailureUiReason =
    when (this) {
        AppUpdateInstallFailureReason.DEPENDENCIES_UNAVAILABLE ->
            UpdateDialogInstallFailureUiReason.DEPENDENCIES_UNAVAILABLE
        AppUpdateInstallFailureReason.RELEASE_REFRESH_FAILED ->
            UpdateDialogInstallFailureUiReason.RELEASE_REFRESH_FAILED
        AppUpdateInstallFailureReason.INSTALLED_VERSION_INVALID ->
            UpdateDialogInstallFailureUiReason.INSTALLED_VERSION_INVALID
        AppUpdateInstallFailureReason.RETAINED_VERSION_INVALID ->
            UpdateDialogInstallFailureUiReason.RETAINED_VERSION_INVALID
        AppUpdateInstallFailureReason.RETAINED_RELEASE_NOT_ELIGIBLE ->
            UpdateDialogInstallFailureUiReason.RETAINED_RELEASE_NOT_ELIGIBLE
        AppUpdateInstallFailureReason.RETAINED_RELEASE_NOT_NEWER ->
            UpdateDialogInstallFailureUiReason.RETAINED_RELEASE_NOT_NEWER
        AppUpdateInstallFailureReason.NO_ELIGIBLE_RELEASE ->
            UpdateDialogInstallFailureUiReason.NO_ELIGIBLE_RELEASE
        AppUpdateInstallFailureReason.RETAINED_RELEASE_NO_LONGER_CURRENT ->
            UpdateDialogInstallFailureUiReason.RETAINED_RELEASE_NO_LONGER_CURRENT
        AppUpdateInstallFailureReason.APK_FILE_MISSING ->
            UpdateDialogInstallFailureUiReason.APK_FILE_MISSING
        AppUpdateInstallFailureReason.APK_NOT_CANONICAL ->
            UpdateDialogInstallFailureUiReason.APK_NOT_CANONICAL
        AppUpdateInstallFailureReason.APK_UNREADABLE ->
            UpdateDialogInstallFailureUiReason.APK_UNREADABLE
        AppUpdateInstallFailureReason.PACKAGE_MISMATCH ->
            UpdateDialogInstallFailureUiReason.PACKAGE_MISMATCH
        AppUpdateInstallFailureReason.VERSION_NOT_NEWER ->
            UpdateDialogInstallFailureUiReason.VERSION_NOT_NEWER
        AppUpdateInstallFailureReason.VERSION_NAME_MISMATCH ->
            UpdateDialogInstallFailureUiReason.VERSION_NAME_MISMATCH
        AppUpdateInstallFailureReason.SIGNING_IDENTITY_UNAVAILABLE ->
            UpdateDialogInstallFailureUiReason.SIGNING_IDENTITY_UNAVAILABLE
        AppUpdateInstallFailureReason.SIGNING_IDENTITY_MISMATCH ->
            UpdateDialogInstallFailureUiReason.SIGNING_IDENTITY_MISMATCH
        AppUpdateInstallFailureReason.RECOVERY_STATE_PERSISTENCE_FAILED ->
            UpdateDialogInstallFailureUiReason.RECOVERY_STATE_PERSISTENCE_FAILED
        AppUpdateInstallFailureReason.INSTALLER_HANDOFF_FAILED ->
            UpdateDialogInstallFailureUiReason.INSTALLER_HANDOFF_FAILED
        AppUpdateInstallFailureReason.INSTALLER_REJECTED ->
            UpdateDialogInstallFailureUiReason.INSTALLER_REJECTED
    }
