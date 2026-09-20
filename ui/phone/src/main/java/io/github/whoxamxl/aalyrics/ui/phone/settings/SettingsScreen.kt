package io.github.whoxamxl.aalyrics.ui.phone.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
    onPlainLyricsAutoScrollChanged: (Boolean) -> Unit,
    onTranslationEnabledChanged: (Boolean) -> Unit,
    onTranslationTargetSelected: (String) -> Unit,
    onTranslationModelDownloadRequested: (String) -> Unit,
    onAndroidAutoCompatibilitySetup: () -> Unit,
    onCheckForUpdates: () -> Unit,
    onDownloadUpdate: () -> Unit,
    onOpenGitHub: () -> Unit,
    modifier: Modifier = Modifier,
    bottomOverlayInset: Dp = 0.dp,
) {
    var targetLanguagePickerVisible by rememberSaveable { mutableStateOf(false) }
    var aboutVisible by rememberSaveable { mutableStateOf(false) }

    SettingsScreenContent(
        state = state,
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
        onAboutRequested = { aboutVisible = true },
        modifier = modifier,
        bottomOverlayInset = bottomOverlayInset,
    )

    if (aboutVisible) {
        AboutDialog(
            title = stringResource(R.string.settings_about_title),
            body = stringResource(R.string.settings_about_body),
            versionLabel = stringResource(R.string.settings_current_version),
            versionName = state.appVersionName,
            githubLabel = stringResource(R.string.settings_github),
            closeLabel = stringResource(R.string.settings_close),
            onOpenGitHub = onOpenGitHub,
            onDismissRequest = { aboutVisible = false },
        )
    }
}

@Composable
internal fun SettingsScreenContent(
    state: SettingsScreenUiState,
    targetLanguagePickerVisible: Boolean,
    onTargetLanguagePickerVisibilityChanged: (Boolean) -> Unit,
    onPlainLyricsAutoScrollChanged: (Boolean) -> Unit,
    onTranslationEnabledChanged: (Boolean) -> Unit,
    onTranslationTargetSelected: (String) -> Unit,
    onTranslationModelDownloadRequested: (String) -> Unit,
    onAndroidAutoCompatibilitySetup: () -> Unit,
    onCheckForUpdates: () -> Unit,
    onDownloadUpdate: () -> Unit,
    onAboutRequested: () -> Unit,
    modifier: Modifier = Modifier,
    bottomOverlayInset: Dp = 0.dp,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
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
                onCheckForUpdates = onCheckForUpdates,
                onDownloadUpdate = onDownloadUpdate,
            )

            SettingsDivider()

            SettingsNavigationRow(
                title = stringResource(R.string.settings_about),
                value = null,
                onClick = onAboutRequested,
            )
        }
    }

    if (targetLanguagePickerVisible) {
        TargetLanguagePicker(
            options = state.translationTargets,
            selectedId = state.translationTarget.id,
            title = stringResource(R.string.settings_translation_target),
            downloadContentDescription =
                stringResource(R.string.settings_translation_model_download),
            downloadingContentDescription =
                stringResource(R.string.settings_translation_model_downloading),
            retryContentDescription =
                stringResource(R.string.settings_translation_model_retry),
            failureInfoContentDescription =
                stringResource(R.string.settings_translation_model_failure_info),
            genericFailureReason =
                stringResource(R.string.settings_translation_model_failure_generic),
            onSelected = onTranslationTargetSelected,
            onDownloadRequested = onTranslationModelDownloadRequested,
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
