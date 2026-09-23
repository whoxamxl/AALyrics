package io.github.whoxamxl.aalyrics.ui.phone.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsColors
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsTheme
import io.github.whoxamxl.aalyrics.ui.phone.settings.AppUpdateUiPhase
import io.github.whoxamxl.aalyrics.ui.phone.settings.AppUpdateUiState
import io.github.whoxamxl.aalyrics.ui.phone.settings.SettingsScreen
import io.github.whoxamxl.aalyrics.ui.phone.settings.SettingsScreenContent
import io.github.whoxamxl.aalyrics.ui.phone.settings.SettingsScreenUiState
import io.github.whoxamxl.aalyrics.ui.phone.settings.TranslationModelCleanupUiState
import io.github.whoxamxl.aalyrics.ui.phone.settings.TranslationModelUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Preview(name = "Typical", group = "SettingsScreen", widthDp = 412, heightDp = 760)
@Composable
private fun SettingsScreenTypicalPreview() {
    SettingsScreenPreview(PhonePreviewFixtures.settingsTypical)
}

@Preview(name = "Non-audio filter off", group = "SettingsScreen", widthDp = 412, heightDp = 760)
@Composable
private fun SettingsScreenNonAudioFilterOffPreview() {
    SettingsScreenPreview(
        PhonePreviewFixtures.settingsTypical.copy(ignoreNonAudioApps = false),
    )
}

@Preview(name = "Translation enabled", group = "SettingsScreen", widthDp = 412, heightDp = 760)
@Composable
private fun SettingsScreenTranslationOnPreview() {
    SettingsScreenPreview(PhonePreviewFixtures.settingsTranslationOn)
}

@Preview(name = "Android Auto · skipped", group = "SettingsScreen", widthDp = 412, heightDp = 760)
@Composable
private fun SettingsScreenSkippedPreview() {
    SettingsScreenPreview(PhonePreviewFixtures.settingsAndroidAutoSkipped)
}

@Preview(name = "Android Auto · not reviewed", group = "SettingsScreen", widthDp = 412, heightDp = 760)
@Composable
private fun SettingsScreenNotReviewedPreview() {
    SettingsScreenPreview(PhonePreviewFixtures.settingsAndroidAutoNotReviewed)
}

@Preview(name = "Target language picker", group = "SettingsScreen", widthDp = 412, heightDp = 760)
@Composable
private fun SettingsTargetLanguagePickerPreview() {
    SettingsScreenContentPreview(
        initialState = PhonePreviewFixtures.settingsTypical,
        initialPickerVisible = true,
    )
}

@Preview(name = "Update · idle", group = "SettingsScreen", widthDp = 412, heightDp = 900)
@Composable
private fun SettingsUpdateIdlePreview() {
    SettingsScreenPreview(PhonePreviewFixtures.settingsIdleUpdate)
}

@Preview(name = "Update · unavailable", group = "SettingsScreen", widthDp = 412, heightDp = 900)
@Composable
private fun SettingsUpdateUnavailablePreview() {
    SettingsScreenPreview(PhonePreviewFixtures.settingsUnavailableUpdate)
}

@Preview(name = "Update · checking", group = "SettingsScreen", widthDp = 412, heightDp = 900)
@Composable
private fun SettingsUpdateCheckingPreview() {
    SettingsScreenPreview(PhonePreviewFixtures.settingsCheckingUpdate)
}

@Preview(name = "Update · up to date", group = "SettingsScreen", widthDp = 412, heightDp = 900)
@Composable
private fun SettingsUpdateUpToDatePreview() {
    SettingsScreenPreview(PhonePreviewFixtures.settingsUpToDate)
}

@Preview(name = "Update · available", group = "SettingsScreen", widthDp = 412, heightDp = 900)
@Composable
private fun SettingsUpdateAvailablePreview() {
    SettingsScreenPreview(PhonePreviewFixtures.settingsUpdateAvailable)
}

@Preview(name = "Update · failed", group = "SettingsScreen", widthDp = 412, heightDp = 900)
@Composable
private fun SettingsUpdateFailedPreview() {
    SettingsScreenPreview(PhonePreviewFixtures.settingsUpdateFailed)
}

@Preview(name = "Update · preparing download", group = "SettingsScreen", widthDp = 412, heightDp = 900)
@Composable
private fun SettingsUpdatePreparingDownloadPreview() {
    SettingsScreenPreview(PhonePreviewFixtures.settingsPreparingUpdateDownload)
}

@Preview(name = "Update · downloading", group = "SettingsScreen", widthDp = 412, heightDp = 900)
@Composable
private fun SettingsUpdateDownloadingPreview() {
    SettingsScreenPreview(PhonePreviewFixtures.settingsDownloadingUpdate)
}

@Preview(name = "Update · downloaded", group = "SettingsScreen", widthDp = 412, heightDp = 900)
@Composable
private fun SettingsUpdateDownloadedPreview() {
    SettingsScreenPreview(PhonePreviewFixtures.settingsDownloadedUpdate)
}

@Preview(name = "Update · download failed", group = "SettingsScreen", widthDp = 412, heightDp = 900)
@Composable
private fun SettingsUpdateDownloadFailedPreview() {
    SettingsScreenPreview(PhonePreviewFixtures.settingsDownloadFailed)
}

@Preview(name = "Update · preparing install", group = "SettingsScreen", widthDp = 412, heightDp = 900)
@Composable
private fun SettingsUpdatePreparingInstallPreview() {
    SettingsScreenPreview(PhonePreviewFixtures.settingsPreparingInstall)
}

@Preview(name = "Update · install permission", group = "SettingsScreen", widthDp = 412, heightDp = 900)
@Composable
private fun SettingsUpdateInstallPermissionPreview() {
    SettingsScreenPreview(PhonePreviewFixtures.settingsInstallPermissionRequired)
}

@Preview(name = "Permission return · denied", group = "SettingsScreen", widthDp = 412, heightDp = 900)
@Composable
private fun SettingsPermissionReturnDeniedPreview() {
    SettingsScreenPreview(PhonePreviewFixtures.settingsInstallPermissionRequired)
}

@Preview(name = "Permission return · granted", group = "SettingsScreen", widthDp = 412, heightDp = 900)
@Composable
private fun SettingsPermissionReturnGrantedPreview() {
    SettingsScreenPreview(PhonePreviewFixtures.settingsInstallingUpdate)
}

@Preview(name = "Update · installing", group = "SettingsScreen", widthDp = 412, heightDp = 900)
@Composable
private fun SettingsUpdateInstallingPreview() {
    SettingsScreenPreview(PhonePreviewFixtures.settingsInstallingUpdate)
}

@Preview(name = "Update · install failed · signing mismatch", group = "SettingsScreen", widthDp = 412, heightDp = 900)
@Composable
private fun SettingsUpdateInstallFailedSigningMismatchPreview() {
    SettingsScreenPreview(PhonePreviewFixtures.settingsInstallFailedSigningMismatch)
}


@Preview(name = "Narrow · 320dp", group = "SettingsScreen", widthDp = 320, heightDp = 700)
@Composable
private fun SettingsScreenNarrowPreview() {
    SettingsScreenPreview(PhonePreviewFixtures.settingsTypical)
}

@Preview(
    name = "Enlarged font",
    group = "SettingsScreen",
    widthDp = 412,
    heightDp = 820,
    fontScale = 1.4f,
)
@Composable
private fun SettingsScreenLargeFontPreview() {
    SettingsScreenPreview(PhonePreviewFixtures.settingsTypical)
}

@Composable
internal fun SettingsScreenPreview(
    initialState: SettingsScreenUiState,
    modifier: Modifier = Modifier,
) {
    AALyricsTheme {
        var state by remember(initialState) { mutableStateOf(initialState) }
        val scope = rememberCoroutineScope()

        Box(
            modifier = modifier
                .fillMaxSize()
                .background(AALyricsColors.BackgroundBase),
        ) {
            SettingsScreen(
                state = state,
                onPlainLyricsAutoScrollChanged = {
                    state = state.copy(plainLyricsAutoScrollEnabled = it)
                },
                onIgnoreNonAudioAppsChanged = {
                    state = state.copy(ignoreNonAudioApps = it)
                },
                onAllowUnclassifiedAppsChanged = {
                    state = state.copy(allowUnclassifiedApps = it)
                },
                onVerboseDetailsChanged = {
                    state = state.copy(verboseDetailsEnabled = it)
                },
                onTranslationEnabledChanged = {
                    state = state.copy(translationEnabled = it)
                },
                onTranslationTargetSelected = { id ->
                    state.translationTargets
                        .firstOrNull { it.id == id }
                        ?.let { target ->
                            state = state.copy(translationTarget = target)
                        }
                },
                onTranslationModelDownloadRequested = { id ->
                    state = state.withPreviewModelDownload(id)
                },
                onClearTranslationModels = {
                    val clearedTargets = state.translationTargets.map { option ->
                        if (option.id == "en") {
                            option.copy(
                                modelState = TranslationModelUiState.BUILT_IN,
                                modelFailureReason = null,
                            )
                        } else {
                            option.copy(
                                modelState = TranslationModelUiState.NOT_DOWNLOADED,
                                modelFailureReason = null,
                            )
                        }
                    }
                    state = state.copy(
                        translationEnabled = false,
                        translationTarget = clearedTargets.first { it.id == "en" },
                        translationTargets = clearedTargets,
                        translationModelCleanup = TranslationModelCleanupUiState.IDLE,
                    )
                },
                onDismissTranslationModelCleanupFailure = {
                    state = state.copy(
                        translationModelCleanup = TranslationModelCleanupUiState.IDLE,
                    )
                },
                onResetAALyrics = {
                    val english = state.translationTargets.first { it.id == "en" }
                    state = state.copy(
                        plainLyricsAutoScrollEnabled = true,
                        ignoreNonAudioApps = true,
                        allowUnclassifiedApps = false,
                        verboseDetailsEnabled = false,
                        translationEnabled = false,
                        translationTarget = english,
                        androidAutoCompatibilityStatus =
                            io.github.whoxamxl.aalyrics.ui.phone.settings.AndroidAutoCompatibilityUiStatus.NOT_REVIEWED,
                        appUpdate = AppUpdateUiState(phase = AppUpdateUiPhase.IDLE),
                    )
                },
                onAndroidAutoCompatibilitySetup = {},
                onCheckForUpdates = {
                    state = state.copy(
                        appUpdate = AppUpdateUiState(phase = AppUpdateUiPhase.CHECKING),
                    )
                    scope.launch {
                        delay(1200)
                        state = state.copy(
                            appUpdate = AppUpdateUiState(
                                phase = AppUpdateUiPhase.UPDATE_AVAILABLE,
                                availableVersionName = "0.1.2",
                            ),
                        )
                    }
                },
                onDownloadUpdate = {
                    val version = state.appUpdate.availableVersionName ?: "0.1.2"
                    state = state.copy(
                        appUpdate = AppUpdateUiState(
                            phase = AppUpdateUiPhase.PREPARING_DOWNLOAD,
                            availableVersionName = version,
                        ),
                    )
                    scope.launch {
                        delay(500)
                        state = state.copy(
                            appUpdate = AppUpdateUiState(
                                phase = AppUpdateUiPhase.DOWNLOADING,
                                availableVersionName = version,
                                downloadProgress = 0f,
                            ),
                        )
                        listOf(0.18f, 0.43f, 0.71f, 1f).forEach { progress ->
                            delay(300)
                            state = state.copy(
                                appUpdate = AppUpdateUiState(
                                    phase = AppUpdateUiPhase.DOWNLOADING,
                                    availableVersionName = version,
                                    downloadProgress = progress,
                                ),
                            )
                        }
                        state = state.copy(
                            appUpdate = AppUpdateUiState(
                                phase = AppUpdateUiPhase.DOWNLOADED,
                                availableVersionName = version,
                            ),
                        )
                    }
                },
                onInstallUpdate = {
                    val version = state.appUpdate.availableVersionName ?: "0.1.2"
                    state = state.copy(
                        appUpdate = AppUpdateUiState(
                            phase = AppUpdateUiPhase.PREPARING_INSTALL,
                            availableVersionName = version,
                        ),
                    )
                    scope.launch {
                        delay(600)
                        state = state.copy(
                            appUpdate = AppUpdateUiState(
                                phase = AppUpdateUiPhase.INSTALL_PERMISSION_REQUIRED,
                                availableVersionName = version,
                            ),
                        )
                    }
                },
                onOpenInstallSettings = {
                    val version = state.appUpdate.availableVersionName ?: "0.1.2"
                    state = state.copy(
                        appUpdate = AppUpdateUiState(
                            phase = AppUpdateUiPhase.PREPARING_INSTALL,
                            availableVersionName = version,
                        ),
                    )
                    scope.launch {
                        delay(350)
                        state = state.copy(
                            appUpdate = AppUpdateUiState(
                                phase = AppUpdateUiPhase.INSTALLING,
                                availableVersionName = version,
                            ),
                        )
                    }
                },
                onOpenGitHub = {},
                onHelpFeedback = {},
                onSupportAALyrics = {},
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun SettingsScreenContentPreview(
    initialState: SettingsScreenUiState,
    initialPickerVisible: Boolean,
) {
    AALyricsTheme {
        var state by remember(initialState) { mutableStateOf(initialState) }
        var pickerVisible by remember(initialPickerVisible) {
            mutableStateOf(initialPickerVisible)
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AALyricsColors.BackgroundBase),
        ) {
            SettingsScreenContent(
                state = state,
                scrollState = rememberScrollState(),
                targetLanguagePickerVisible = pickerVisible,
                onTargetLanguagePickerVisibilityChanged = { pickerVisible = it },
                onPlainLyricsAutoScrollChanged = {
                    state = state.copy(plainLyricsAutoScrollEnabled = it)
                },
                onIgnoreNonAudioAppsChanged = {
                    state = state.copy(ignoreNonAudioApps = it)
                },
                onTranslationEnabledChanged = {
                    state = state.copy(translationEnabled = it)
                },
                onTranslationTargetSelected = { id ->
                    state.translationTargets
                        .firstOrNull { it.id == id }
                        ?.let { target ->
                            state = state.copy(translationTarget = target)
                        }
                },
                onTranslationModelDownloadRequested = { id ->
                    state = state.withPreviewModelDownload(id)
                },
                onAndroidAutoCompatibilitySetup = {},
                onCheckForUpdates = {},
                onDownloadUpdate = {},
                onInstallUpdate = {},
                onChangelogRequested = {},
                onPrivacyPolicyRequested = {},
                onTermsOfUseRequested = {},
                onLicenseRequested = {},
                onHelpFeedbackRequested = {},
                onSupportAALyricsRequested = {},
                onAdvancedRequested = {},
                onOpenGitHub = {},
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

private fun SettingsScreenUiState.withPreviewModelDownload(
    id: String,
): SettingsScreenUiState {
    val index = translationTargets.indexOfFirst { it.id == id }
    if (index < 0) return this

    val downloading = translationTargets.toMutableList().apply {
        this[index] = this[index].copy(
            modelState = TranslationModelUiState.DOWNLOADING,
        )
    }

    return copy(
        translationTargets = downloading,
        translationTarget = if (translationTarget.id == id) {
            downloading[index]
        } else {
            translationTarget
        },
    )
}
