package io.github.whoxamxl.aalyrics

import io.github.whoxamxl.aalyrics.translation.api.TranslationLanguages
import io.github.whoxamxl.aalyrics.translation.api.TranslationModelPhase
import io.github.whoxamxl.aalyrics.translation.api.TranslationModelState
import io.github.whoxamxl.aalyrics.translation.api.TranslationSettings
import io.github.whoxamxl.aalyrics.ui.phone.settings.AndroidAutoCompatibilityUiStatus
import io.github.whoxamxl.aalyrics.ui.phone.settings.AppUpdateInstallFailureUiReason
import io.github.whoxamxl.aalyrics.ui.phone.settings.AppUpdateUiPhase
import io.github.whoxamxl.aalyrics.ui.phone.settings.AppUpdateUiState
import io.github.whoxamxl.aalyrics.ui.phone.settings.SettingsLanguageOptionUiState
import io.github.whoxamxl.aalyrics.ui.phone.settings.SettingsScreenUiState
import io.github.whoxamxl.aalyrics.ui.phone.settings.TranslationModelCleanupUiState
import io.github.whoxamxl.aalyrics.ui.phone.settings.TranslationModelUiState
import java.util.Locale

internal fun mapPhoneSettingsState(
    translationSettings: TranslationSettings,
    translationModelStates: Map<String, TranslationModelState>,
    verboseDetailsEnabled: Boolean,
    plainLyricsAutoScrollEnabled: Boolean,
    ignoreNonAudioApps: Boolean,
    allowUnclassifiedApps: Boolean,
    automaticallyCheckForUpdates: Boolean = true,
    androidAutoStatus: AndroidAutoCompatibilityUiStatus,
    appVersionName: String,
    currentYear: Int,
    noticeText: String,
    licenseText: String,
    changelogText: String,
    privacyPolicyText: String,
    termsOfUseText: String = "",
    thirdPartyLicensesText: String = "",
    translationModelCleanupState: TranslationModelCleanupState =
        TranslationModelCleanupState.IDLE,
    appUpdateCheckState: AppUpdateCheckState = AppUpdateCheckState.Idle,
    displayLocale: Locale = Locale.getDefault(),
): SettingsScreenUiState {
    val targets = TranslationLanguages.supportedTargets.map { languageTag ->
        val modelState = translationModelStates[languageTag]
        SettingsLanguageOptionUiState(
            id = languageTag,
            displayName = TranslationLanguages.displayName(languageTag, displayLocale),
            modelState = when {
                languageTag == "en" -> TranslationModelUiState.BUILT_IN
                modelState == null -> TranslationModelUiState.NOT_DOWNLOADED
                modelState.phase == TranslationModelPhase.CHECKING ->
                    TranslationModelUiState.CHECKING
                modelState.phase == TranslationModelPhase.READY ->
                    TranslationModelUiState.READY
                modelState.phase == TranslationModelPhase.FAILED ||
                    modelState.phase == TranslationModelPhase.TIMED_OUT ->
                    TranslationModelUiState.FAILED
                else -> TranslationModelUiState.DOWNLOADING
            },
            modelFailureReason = modelState?.error,
        )
    }
    val selected = targets.first { it.id == translationSettings.targetLanguage }

    return SettingsScreenUiState(
        plainLyricsAutoScrollEnabled = plainLyricsAutoScrollEnabled,
        ignoreNonAudioApps = ignoreNonAudioApps,
        allowUnclassifiedApps = allowUnclassifiedApps,
        automaticallyCheckForUpdates = automaticallyCheckForUpdates,
        verboseDetailsEnabled = verboseDetailsEnabled,
        translationEnabled = translationSettings.enabled,
        translationTarget = selected,
        translationTargets = targets,
        translationModelCleanup = when (translationModelCleanupState) {
            TranslationModelCleanupState.IDLE -> TranslationModelCleanupUiState.IDLE
            TranslationModelCleanupState.RUNNING -> TranslationModelCleanupUiState.RUNNING
            TranslationModelCleanupState.FAILED -> TranslationModelCleanupUiState.FAILED
        },
        androidAutoCompatibilityStatus = androidAutoStatus,
        appVersionName = appVersionName,
        currentYear = currentYear,
        noticeText = noticeText,
        licenseText = licenseText,
        changelogText = changelogText,
        privacyPolicyText = privacyPolicyText,
        termsOfUseText = termsOfUseText,
        thirdPartyLicensesText = thirdPartyLicensesText,
        appUpdate = when (appUpdateCheckState) {
            AppUpdateCheckState.Idle ->
                AppUpdateUiState(phase = AppUpdateUiPhase.IDLE)
            is AppUpdateCheckState.Checking ->
                if (appUpdateCheckState.origin == UpdateCheckOrigin.MANUAL) {
                    AppUpdateUiState(phase = AppUpdateUiPhase.CHECKING)
                } else {
                    AppUpdateUiState(phase = AppUpdateUiPhase.IDLE)
                }
            is AppUpdateCheckState.UpToDate ->
                if (appUpdateCheckState.origin == UpdateCheckOrigin.MANUAL) {
                    AppUpdateUiState(phase = AppUpdateUiPhase.UP_TO_DATE)
                } else {
                    AppUpdateUiState(phase = AppUpdateUiPhase.IDLE)
                }
            is AppUpdateCheckState.UpdateAvailable ->
                if (appUpdateCheckState.origin == UpdateCheckOrigin.INSTALL_REFRESH) {
                    AppUpdateUiState(
                        phase = AppUpdateUiPhase.UPDATE_AVAILABLE,
                        availableVersionName = appUpdateCheckState.versionName,
                    )
                } else {
                    AppUpdateUiState(phase = AppUpdateUiPhase.IDLE)
                }
            is AppUpdateCheckState.Failed ->
                if (appUpdateCheckState.origin == UpdateCheckOrigin.MANUAL) {
                    AppUpdateUiState(phase = AppUpdateUiPhase.CHECK_FAILED)
                } else {
                    AppUpdateUiState(phase = AppUpdateUiPhase.IDLE)
                }
            is AppUpdateCheckState.PreparingDownload,
            is AppUpdateCheckState.Downloading,
            is AppUpdateCheckState.VerifyingDownload ->
                AppUpdateUiState(phase = AppUpdateUiPhase.IDLE)
            is AppUpdateCheckState.Downloaded ->
                AppUpdateUiState(phase = AppUpdateUiPhase.IDLE)
            is AppUpdateCheckState.DownloadFailed ->
                AppUpdateUiState(phase = AppUpdateUiPhase.IDLE)
            is AppUpdateCheckState.PreparingInstall,
            is AppUpdateCheckState.InstallPermissionRequired,
            is AppUpdateCheckState.Installing ->
                AppUpdateUiState(phase = AppUpdateUiPhase.IDLE)
            is AppUpdateCheckState.InstallFailed ->
                AppUpdateUiState(
                    phase = AppUpdateUiPhase.INSTALL_FAILED,
                    availableVersionName = appUpdateCheckState.versionName,
                    installFailureReason = appUpdateCheckState.reason.toUiReason(),
                )
        },
    )
}

private fun AppUpdateInstallFailureReason.toUiReason(): AppUpdateInstallFailureUiReason =
    when (this) {
        AppUpdateInstallFailureReason.DEPENDENCIES_UNAVAILABLE ->
            AppUpdateInstallFailureUiReason.DEPENDENCIES_UNAVAILABLE
        AppUpdateInstallFailureReason.RELEASE_REFRESH_FAILED ->
            AppUpdateInstallFailureUiReason.RELEASE_REFRESH_FAILED
        AppUpdateInstallFailureReason.INSTALLED_VERSION_INVALID ->
            AppUpdateInstallFailureUiReason.INSTALLED_VERSION_INVALID
        AppUpdateInstallFailureReason.RETAINED_VERSION_INVALID ->
            AppUpdateInstallFailureUiReason.RETAINED_VERSION_INVALID
        AppUpdateInstallFailureReason.RETAINED_RELEASE_NOT_ELIGIBLE ->
            AppUpdateInstallFailureUiReason.RETAINED_RELEASE_NOT_ELIGIBLE
        AppUpdateInstallFailureReason.RETAINED_RELEASE_NOT_NEWER ->
            AppUpdateInstallFailureUiReason.RETAINED_RELEASE_NOT_NEWER
        AppUpdateInstallFailureReason.NO_ELIGIBLE_RELEASE ->
            AppUpdateInstallFailureUiReason.NO_ELIGIBLE_RELEASE
        AppUpdateInstallFailureReason.RETAINED_RELEASE_NO_LONGER_CURRENT ->
            AppUpdateInstallFailureUiReason.RETAINED_RELEASE_NO_LONGER_CURRENT
        AppUpdateInstallFailureReason.APK_FILE_MISSING ->
            AppUpdateInstallFailureUiReason.APK_FILE_MISSING
        AppUpdateInstallFailureReason.APK_NOT_CANONICAL ->
            AppUpdateInstallFailureUiReason.APK_NOT_CANONICAL
        AppUpdateInstallFailureReason.APK_UNREADABLE ->
            AppUpdateInstallFailureUiReason.APK_UNREADABLE
        AppUpdateInstallFailureReason.PACKAGE_MISMATCH ->
            AppUpdateInstallFailureUiReason.PACKAGE_MISMATCH
        AppUpdateInstallFailureReason.VERSION_NOT_NEWER ->
            AppUpdateInstallFailureUiReason.VERSION_NOT_NEWER
        AppUpdateInstallFailureReason.VERSION_NAME_MISMATCH ->
            AppUpdateInstallFailureUiReason.VERSION_NAME_MISMATCH
        AppUpdateInstallFailureReason.SIGNING_IDENTITY_UNAVAILABLE ->
            AppUpdateInstallFailureUiReason.SIGNING_IDENTITY_UNAVAILABLE
        AppUpdateInstallFailureReason.SIGNING_IDENTITY_MISMATCH ->
            AppUpdateInstallFailureUiReason.SIGNING_IDENTITY_MISMATCH
        AppUpdateInstallFailureReason.RECOVERY_STATE_PERSISTENCE_FAILED ->
            AppUpdateInstallFailureUiReason.RECOVERY_STATE_PERSISTENCE_FAILED
        AppUpdateInstallFailureReason.INSTALLER_HANDOFF_FAILED ->
            AppUpdateInstallFailureUiReason.INSTALLER_HANDOFF_FAILED
        AppUpdateInstallFailureReason.INSTALLER_REJECTED ->
            AppUpdateInstallFailureUiReason.INSTALLER_REJECTED
    }
