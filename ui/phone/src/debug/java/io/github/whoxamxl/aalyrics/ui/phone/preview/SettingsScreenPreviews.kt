package io.github.whoxamxl.aalyrics.ui.phone.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import io.github.whoxamxl.aalyrics.ui.phone.settings.ChangelogDialog
import io.github.whoxamxl.aalyrics.ui.phone.settings.ChangelogUiPhase
import io.github.whoxamxl.aalyrics.ui.phone.settings.ChangelogUiState
import io.github.whoxamxl.aalyrics.ui.phone.settings.SettingsScreenContent
import io.github.whoxamxl.aalyrics.ui.phone.settings.SettingsScreenUiState
import io.github.whoxamxl.aalyrics.ui.phone.settings.TranslationModelUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Preview(name = "Typical", group = "SettingsScreen", widthDp = 412, heightDp = 760)
@Composable
private fun SettingsScreenTypicalPreview() {
    SettingsScreenPreview(PhonePreviewFixtures.settingsTypical)
}

@Preview(name = "Translation off", group = "SettingsScreen", widthDp = 412, heightDp = 760)
@Composable
private fun SettingsScreenTranslationOffPreview() {
    SettingsScreenPreview(PhonePreviewFixtures.settingsTranslationOff)
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
    SettingsScreenPreview(
        initialState = PhonePreviewFixtures.settingsTypical,
        initialPickerVisible = true,
    )
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


@Preview(name = "Changelog · ready", group = "SettingsScreen", widthDp = 412, heightDp = 760)
@Composable
private fun SettingsChangelogReadyPreview() {
    AALyricsTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AALyricsColors.BackgroundBase),
        ) {
            ChangelogDialog(
                state = PhonePreviewFixtures.settingsChangelogReady.changelog,
                title = "Changelog",
                loadingLabel = "Loading release notes…",
                failureLabel = "Could not load changelog",
                retryLabel = "Retry",
                closeLabel = "Close",
                genericFailureReason = "GitHub Release notes could not be loaded.",
                onRetry = {},
                onDismissRequest = {},
            )
        }
    }
}

@Preview(name = "Changelog · failed", group = "SettingsScreen", widthDp = 412, heightDp = 760)
@Composable
private fun SettingsChangelogFailedPreview() {
    AALyricsTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AALyricsColors.BackgroundBase),
        ) {
            ChangelogDialog(
                state = PhonePreviewFixtures.settingsChangelogFailed.changelog,
                title = "Changelog",
                loadingLabel = "Loading release notes…",
                failureLabel = "Could not load changelog",
                retryLabel = "Retry",
                closeLabel = "Close",
                genericFailureReason = "GitHub Release notes could not be loaded.",
                onRetry = {},
                onDismissRequest = {},
            )
        }
    }
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
    initialPickerVisible: Boolean = false,
    modifier: Modifier = Modifier,
) {
    AALyricsTheme {
        var state by remember(initialState) { mutableStateOf(initialState) }
        var pickerVisible by remember(initialPickerVisible) {
            mutableStateOf(initialPickerVisible)
        }
        val scope = rememberCoroutineScope()

        Box(
            modifier = modifier
                .fillMaxSize()
                .background(AALyricsColors.BackgroundBase),
        ) {
            SettingsScreenContent(
                state = state,
                targetLanguagePickerVisible = pickerVisible,
                onTargetLanguagePickerVisibilityChanged = { pickerVisible = it },
                onPlainLyricsAutoScrollChanged = {
                    state = state.copy(plainLyricsAutoScrollEnabled = it)
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
                    val index = state.translationTargets.indexOfFirst { it.id == id }
                    if (index >= 0) {
                        val downloading = state.translationTargets.toMutableList().apply {
                            this[index] = this[index].copy(
                                modelState = TranslationModelUiState.DOWNLOADING,
                            )
                        }
                        state = state.copy(
                            translationTargets = downloading,
                            translationTarget = if (state.translationTarget.id == id) {
                                downloading[index]
                            } else {
                                state.translationTarget
                            },
                        )
                        scope.launch {
                            delay(1200)
                            val ready = state.translationTargets.toMutableList().apply {
                                val currentIndex = indexOfFirst { it.id == id }
                                if (currentIndex >= 0) {
                                    this[currentIndex] = this[currentIndex].copy(
                                        modelState = TranslationModelUiState.READY,
                                    )
                                }
                            }
                            state = state.copy(
                                translationTargets = ready,
                                translationTarget = if (state.translationTarget.id == id) {
                                    ready.first { it.id == id }
                                } else {
                                    state.translationTarget
                                },
                            )
                        }
                    }
                },
                onAndroidAutoCompatibilitySetup = {},
                onCheckForUpdates = {
                    state = state.copy(
                        appUpdate = AppUpdateUiState(
                            phase = AppUpdateUiPhase.CHECKING,
                        ),
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
                            phase = AppUpdateUiPhase.DOWNLOADING,
                            availableVersionName = version,
                        ),
                    )
                    scope.launch {
                        delay(1200)
                        state = state.copy(
                            appUpdate = AppUpdateUiState(
                                phase = AppUpdateUiPhase.DOWNLOADED,
                                availableVersionName = version,
                            ),
                        )
                    }
                },
                onChangelogRequested = {
                    state = state.copy(
                        changelog = ChangelogUiState(
                            phase = ChangelogUiPhase.LOADING,
                        ),
                    )
                    scope.launch {
                        delay(900)
                        state = state.copy(
                            changelog = PhonePreviewFixtures.settingsChangelogReady.changelog,
                        )
                    }
                },
                onLicenseRequested = {},
                onAdvancedRequested = {},
                onOpenGitHub = {},
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
