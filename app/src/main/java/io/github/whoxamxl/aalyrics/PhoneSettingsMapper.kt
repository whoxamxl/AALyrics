package io.github.whoxamxl.aalyrics

import io.github.whoxamxl.aalyrics.translation.api.TranslationLanguages
import io.github.whoxamxl.aalyrics.translation.api.TranslationModelPhase
import io.github.whoxamxl.aalyrics.translation.api.TranslationModelState
import io.github.whoxamxl.aalyrics.translation.api.TranslationSettings
import io.github.whoxamxl.aalyrics.ui.phone.settings.AndroidAutoCompatibilityUiStatus
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
            AppUpdateCheckState.Checking ->
                AppUpdateUiState(phase = AppUpdateUiPhase.CHECKING)
            AppUpdateCheckState.UpToDate ->
                AppUpdateUiState(phase = AppUpdateUiPhase.UP_TO_DATE)
            is AppUpdateCheckState.UpdateAvailable ->
                AppUpdateUiState(
                    phase = AppUpdateUiPhase.UPDATE_AVAILABLE,
                    availableVersionName = appUpdateCheckState.versionName,
                )
            AppUpdateCheckState.Failed ->
                AppUpdateUiState(phase = AppUpdateUiPhase.CHECK_FAILED)
            is AppUpdateCheckState.Downloading ->
                AppUpdateUiState(
                    phase = AppUpdateUiPhase.DOWNLOADING,
                    availableVersionName = appUpdateCheckState.versionName,
                )
            is AppUpdateCheckState.Downloaded ->
                AppUpdateUiState(
                    phase = AppUpdateUiPhase.DOWNLOADED,
                    availableVersionName = appUpdateCheckState.versionName,
                )
            is AppUpdateCheckState.DownloadFailed ->
                AppUpdateUiState(
                    phase = AppUpdateUiPhase.DOWNLOAD_FAILED,
                    availableVersionName = appUpdateCheckState.versionName,
                )
        },
    )
}
