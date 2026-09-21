package io.github.whoxamxl.aalyrics

import io.github.whoxamxl.aalyrics.translation.api.TranslationLanguages
import io.github.whoxamxl.aalyrics.translation.api.TranslationModelPhase
import io.github.whoxamxl.aalyrics.translation.api.TranslationModelState
import io.github.whoxamxl.aalyrics.translation.api.TranslationSettings
import io.github.whoxamxl.aalyrics.ui.phone.settings.AndroidAutoCompatibilityUiStatus
import io.github.whoxamxl.aalyrics.ui.phone.settings.AppUpdateUiPhase
import io.github.whoxamxl.aalyrics.ui.phone.settings.AppUpdateUiState
import io.github.whoxamxl.aalyrics.ui.phone.settings.ChangelogUiPhase
import io.github.whoxamxl.aalyrics.ui.phone.settings.ChangelogUiState
import io.github.whoxamxl.aalyrics.ui.phone.settings.SettingsLanguageOptionUiState
import io.github.whoxamxl.aalyrics.ui.phone.settings.SettingsScreenUiState
import io.github.whoxamxl.aalyrics.ui.phone.settings.TranslationModelUiState
import java.util.Locale

internal fun mapPhoneSettingsState(
    translationSettings: TranslationSettings,
    translationModelStates: Map<String, TranslationModelState>,
    verboseDetailsEnabled: Boolean,
    plainLyricsAutoScrollEnabled: Boolean,
    androidAutoStatus: AndroidAutoCompatibilityUiStatus,
    appVersionName: String,
    currentYear: Int,
    licenseText: String,
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
                modelState.phase == TranslationModelPhase.READY -> TranslationModelUiState.READY
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
        verboseDetailsEnabled = verboseDetailsEnabled,
        translationEnabled = translationSettings.enabled,
        translationTarget = selected,
        translationTargets = targets,
        androidAutoCompatibilityStatus = androidAutoStatus,
        appVersionName = appVersionName,
        currentYear = currentYear,
        licenseText = licenseText,
        appUpdate = AppUpdateUiState(phase = AppUpdateUiPhase.UNAVAILABLE),
        changelog = ChangelogUiState(phase = ChangelogUiPhase.UNAVAILABLE),
    )
}
