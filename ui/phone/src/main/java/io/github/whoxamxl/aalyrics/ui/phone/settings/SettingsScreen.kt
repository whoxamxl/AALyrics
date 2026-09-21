package io.github.whoxamxl.aalyrics.ui.phone.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsColors
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsSpacing
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsTypography
import io.github.whoxamxl.aalyrics.ui.phone.R

@Composable
fun SettingsScreen(
    state: SettingsScreenUiState,
    rootResetKey: Int = 0,
    onPlainLyricsAutoScrollChanged: (Boolean) -> Unit,
    onVerboseDetailsChanged: (Boolean) -> Unit,
    onTranslationEnabledChanged: (Boolean) -> Unit,
    onTranslationTargetSelected: (String) -> Unit,
    onTranslationModelDownloadRequested: (String) -> Unit,
    onClearTranslationModels: () -> Unit,
    onResetAALyrics: () -> Unit,
    onAndroidAutoCompatibilitySetup: () -> Unit,
    onCheckForUpdates: () -> Unit,
    onDownloadUpdate: () -> Unit,
    onChangelogRequested: () -> Unit,
    onSettingsEntered: () -> Unit,
    onOpenGitHub: () -> Unit,
    modifier: Modifier = Modifier,
    bottomOverlayInset: Dp = 0.dp,
) {
    var targetLanguagePickerVisible by rememberSaveable { mutableStateOf(false) }
    var changelogVisible by rememberSaveable { mutableStateOf(false) }
    var activeSubscreen by rememberSaveable {
        mutableStateOf(SettingsSubscreen.MAIN)
    }

    BackHandler(enabled = activeSubscreen != SettingsSubscreen.MAIN) {
        activeSubscreen = SettingsSubscreen.MAIN
    }

    LaunchedEffect(rootResetKey) {
        targetLanguagePickerVisible = false
        changelogVisible = false
        activeSubscreen = SettingsSubscreen.MAIN
    }

    LaunchedEffect(Unit) {
        onSettingsEntered()
    }

    when (activeSubscreen) {
        SettingsSubscreen.ADVANCED -> AdvancedSettingsScreen(
            verboseDetailsEnabled = state.verboseDetailsEnabled,
            onVerboseDetailsChanged = onVerboseDetailsChanged,
            onClearTranslationModels = onClearTranslationModels,
            onResetAALyrics = onResetAALyrics,
            onBack = { activeSubscreen = SettingsSubscreen.MAIN },
            modifier = modifier,
            bottomOverlayInset = bottomOverlayInset,
        )

        SettingsSubscreen.LICENSE -> LicenseSettingsScreen(
            licenseText = state.licenseText,
            onBack = { activeSubscreen = SettingsSubscreen.MAIN },
            modifier = modifier,
            bottomOverlayInset = bottomOverlayInset,
        )

        SettingsSubscreen.MAIN -> SettingsScreenContent(
            state = state,
            rootResetKey = rootResetKey,
            targetLanguagePickerVisible = targetLanguagePickerVisible,
            onTargetLanguagePickerVisibilityChanged = {
                targetLanguagePickerVisible = it
            },
            onPlainLyricsAutoScrollChanged = onPlainLyricsAutoScrollChanged,
            onTranslationEnabledChanged = onTranslationEnabledChanged,
            onTranslationTargetSelected = onTranslationTargetSelected,
            onTranslationModelDownloadRequested = onTranslationModelDownloadRequested,
            onAndroidAutoCompatibilitySetup = onAndroidAutoCompatibilitySetup,
            onCheckForUpdates = onCheckForUpdates,
            onDownloadUpdate = onDownloadUpdate,
            onChangelogRequested = {
                changelogVisible = true
                onChangelogRequested()
            },
            onLicenseRequested = { activeSubscreen = SettingsSubscreen.LICENSE },
            onAdvancedRequested = { activeSubscreen = SettingsSubscreen.ADVANCED },
            onOpenGitHub = onOpenGitHub,
            modifier = modifier,
            bottomOverlayInset = bottomOverlayInset,
        )
    }

    if (changelogVisible) {
        ChangelogDialog(
            state = state.changelog,
            title = stringResource(R.string.settings_changelog),
            loadingLabel = stringResource(R.string.settings_changelog_loading),
            failureLabel = stringResource(R.string.settings_changelog_failed),
            retryLabel = stringResource(R.string.settings_retry),
            closeLabel = stringResource(R.string.settings_close),
            genericFailureReason = stringResource(R.string.settings_changelog_failure_generic),
            onRetry = onChangelogRequested,
            onDismissRequest = { changelogVisible = false },
        )
    }
}

@Composable
internal fun SettingsScreenContent(
    state: SettingsScreenUiState,
    rootResetKey: Int = 0,
    targetLanguagePickerVisible: Boolean,
    onTargetLanguagePickerVisibilityChanged: (Boolean) -> Unit,
    onPlainLyricsAutoScrollChanged: (Boolean) -> Unit,
    onTranslationEnabledChanged: (Boolean) -> Unit,
    onTranslationTargetSelected: (String) -> Unit,
    onTranslationModelDownloadRequested: (String) -> Unit,
    onAndroidAutoCompatibilitySetup: () -> Unit,
    onCheckForUpdates: () -> Unit,
    onDownloadUpdate: () -> Unit,
    onChangelogRequested: () -> Unit,
    onLicenseRequested: () -> Unit,
    onAdvancedRequested: () -> Unit,
    onOpenGitHub: () -> Unit,
    modifier: Modifier = Modifier,
    bottomOverlayInset: Dp = 0.dp,
) {
    var draftTargetLanguageId by rememberSaveable(
        state.translationTarget.id,
        targetLanguagePickerVisible,
    ) {
        mutableStateOf(state.translationTarget.id)
    }
    val scrollState = rememberScrollState()

    LaunchedEffect(rootResetKey) {
        scrollState.scrollTo(0)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(
                start = AALyricsSpacing.Space16,
                top = AALyricsSpacing.Space16,
                end = AALyricsSpacing.Space16,
                bottom = bottomOverlayInset + AALyricsSpacing.Space16,
            ),
    ) {
        Text(
            text = stringResource(R.string.settings_title),
            style = AALyricsTypography.LyricsSupporting,
            color = AALyricsColors.TextPrimary,
        )

        Spacer(Modifier.height(AALyricsSpacing.Space20))

        SettingsSection(
            title = stringResource(R.string.settings_section_lyrics),
        ) {
            SettingsSwitchRow(
                title = stringResource(R.string.settings_plain_auto_scroll),
                checked = state.plainLyricsAutoScrollEnabled,
                onCheckedChange = onPlainLyricsAutoScrollChanged,
                infoText = stringResource(R.string.settings_plain_auto_scroll_info),
                infoContentDescription = stringResource(R.string.settings_plain_auto_scroll_info_description),
            )
        }

        Spacer(Modifier.height(AALyricsSpacing.Space20))

        SettingsSection(
            title = stringResource(R.string.settings_section_translation),
        ) {
            SettingsSwitchRow(
                title = stringResource(R.string.settings_translation_enabled),
                checked = state.translationEnabled,
                onCheckedChange = onTranslationEnabledChanged,
            )

            SettingsDivider()

            SettingsNavigationRow(
                title = stringResource(R.string.settings_translation_target),
                value = state.translationTarget.displayName,
                onClick = { onTargetLanguagePickerVisibilityChanged(true) },
            )
        }

        Spacer(Modifier.height(AALyricsSpacing.Space20))

        SettingsSection(
            title = stringResource(R.string.settings_section_android_auto),
        ) {
            SettingsNavigationRow(
                title = stringResource(R.string.settings_android_auto_compatibility),
                value = androidAutoStatusLabel(state.androidAutoCompatibilityStatus),
                valueColor = androidAutoStatusColor(state.androidAutoCompatibilityStatus),
                onClick = onAndroidAutoCompatibilitySetup,
            )
        }

        Spacer(Modifier.height(AALyricsSpacing.Space20))

        SettingsSection(
            title = stringResource(R.string.settings_section_app),
        ) {
            AppUpdateRow(
                versionLabel = stringResource(R.string.settings_version),
                currentVersionName = state.appVersionName,
                state = state.appUpdate,
                checkLabel = stringResource(R.string.settings_check_for_updates),
                checkingLabel = stringResource(R.string.settings_checking_for_updates),
                upToDateLabel = stringResource(R.string.settings_up_to_date),
                updateAvailableLabel = stringResource(R.string.settings_update_available),
                downloadLabel = stringResource(R.string.settings_download_update),
                downloadingLabel = stringResource(R.string.settings_downloading_update),
                downloadedLabel = stringResource(R.string.settings_update_downloaded),
                retryLabel = stringResource(R.string.settings_retry),
                checkFailedLabel = stringResource(R.string.settings_update_check_failed),
                downloadFailedLabel = stringResource(R.string.settings_update_download_failed),
                failureInfoContentDescription =
                    stringResource(R.string.settings_update_failure_info),
                genericFailureReason =
                    stringResource(R.string.settings_update_failure_generic),
                unavailableLabel = stringResource(R.string.settings_not_available_yet),
                onCheckForUpdates = onCheckForUpdates,
                onDownloadUpdate = onDownloadUpdate,
            )

            SettingsDivider()

            SettingsNavigationRow(
                title = stringResource(R.string.settings_changelog),
                value = if (state.changelog.phase == ChangelogUiPhase.UNAVAILABLE) {
                    stringResource(R.string.settings_not_available_yet)
                } else {
                    null
                },
                enabled = state.changelog.phase != ChangelogUiPhase.UNAVAILABLE,
                onClick = onChangelogRequested,
            )

            SettingsDivider()

            SettingsExternalLinkRow(
                title = stringResource(R.string.settings_source_code),
                value = stringResource(R.string.settings_github),
                onClick = onOpenGitHub,
            )

            SettingsDivider()

            SettingsNavigationRow(
                title = stringResource(R.string.settings_license),
                value = null,
                onClick = onLicenseRequested,
            )
        }

        Spacer(Modifier.height(AALyricsSpacing.Space20))

        SettingsSection(title = null) {
            SettingsNavigationRow(
                title = stringResource(R.string.settings_advanced),
                value = null,
                onClick = onAdvancedRequested,
            )
        }

        SettingsBrandFooter(
            appName = stringResource(R.string.settings_brand_name),
            versionLabel = stringResource(R.string.settings_version),
            versionName = state.appVersionName,
            currentYear = state.currentYear,
            copyrightOwner = stringResource(R.string.settings_brand_copyright_owner),
            logoContentDescription = stringResource(R.string.settings_brand_logo_description),
            onOpenGitHub = onOpenGitHub,
        )
    }

    if (targetLanguagePickerVisible) {
        TargetLanguagePicker(
            options = state.translationTargets,
            selectedId = draftTargetLanguageId,
            title = stringResource(R.string.settings_translation_target),
            downloadContentDescription =
                stringResource(R.string.settings_translation_model_download),
            checkingContentDescription =
                stringResource(R.string.settings_translation_model_checking),
            downloadingContentDescription =
                stringResource(R.string.settings_translation_model_downloading),
            retryContentDescription =
                stringResource(R.string.settings_translation_model_retry),
            failureInfoContentDescription =
                stringResource(R.string.settings_translation_model_failure_info),
            genericFailureReason =
                stringResource(R.string.settings_translation_model_failure_generic),
            confirmLabel = stringResource(R.string.settings_done),
            dismissLabel = stringResource(R.string.settings_cancel),
            onSelected = { draftTargetLanguageId = it },
            onDownloadRequested = onTranslationModelDownloadRequested,
            onConfirm = {
                onTranslationTargetSelected(draftTargetLanguageId)
                onTargetLanguagePickerVisibilityChanged(false)
            },
            onDismissRequest = {
                onTargetLanguagePickerVisibilityChanged(false)
            },
        )
    }
}

@Composable
private fun androidAutoStatusLabel(
    status: AndroidAutoCompatibilityUiStatus,
): String = when (status) {
    AndroidAutoCompatibilityUiStatus.ENABLED ->
        stringResource(R.string.settings_android_auto_status_enabled)
    AndroidAutoCompatibilityUiStatus.SKIPPED ->
        stringResource(R.string.settings_android_auto_status_skipped)
    AndroidAutoCompatibilityUiStatus.NOT_REVIEWED ->
        stringResource(R.string.settings_android_auto_status_not_reviewed)
}

private fun androidAutoStatusColor(
    status: AndroidAutoCompatibilityUiStatus,
) = when (status) {
    AndroidAutoCompatibilityUiStatus.ENABLED -> AALyricsColors.Success
    AndroidAutoCompatibilityUiStatus.SKIPPED -> AALyricsColors.Warning
    AndroidAutoCompatibilityUiStatus.NOT_REVIEWED -> AALyricsColors.TextSecondary
}


private enum class SettingsSubscreen {
    MAIN,
    ADVANCED,
    LICENSE,
}
