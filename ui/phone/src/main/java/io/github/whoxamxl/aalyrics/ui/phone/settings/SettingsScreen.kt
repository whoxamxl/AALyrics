package io.github.whoxamxl.aalyrics.ui.phone.settings

import androidx.compose.foundation.ScrollState
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
    onIgnoreNonAudioAppsChanged: (Boolean) -> Unit,
    onAllowUnclassifiedAppsChanged: (Boolean) -> Unit,
    onAutomaticallyCheckForUpdatesChanged: (Boolean) -> Unit = {},
    onVerboseDetailsChanged: (Boolean) -> Unit,
    onTranslationEnabledChanged: (Boolean) -> Unit,
    onTranslationTargetSelected: (String) -> Unit,
    onTranslationModelDownloadRequested: (String) -> Unit,
    onClearTranslationModels: () -> Unit,
    onDismissTranslationModelCleanupFailure: () -> Unit,
    onResetAALyrics: () -> Unit,
    onAndroidAutoCompatibilitySetup: () -> Unit,
    onCheckForUpdates: () -> Unit,
    onOpenGitHub: () -> Unit,
    onHelpFeedback: (HelpFeedbackDestination) -> Unit,
    onSupportAALyrics: () -> Unit,
    modifier: Modifier = Modifier,
    bottomOverlayInset: Dp = 0.dp,
) {
    var targetLanguagePickerVisible by rememberSaveable { mutableStateOf(false) }
    var activeSubscreen by rememberSaveable {
        mutableStateOf(SettingsSubscreen.MAIN)
    }
    val mainScrollState = rememberScrollState()

    BackHandler(enabled = activeSubscreen != SettingsSubscreen.MAIN) {
        activeSubscreen = activeSubscreen.backDestination()
    }

    LaunchedEffect(rootResetKey) {
        targetLanguagePickerVisible = false
        activeSubscreen = settingsRootSubscreen()
        mainScrollState.scrollTo(0)
    }

    when (activeSubscreen) {
        SettingsSubscreen.ADVANCED -> AdvancedSettingsScreen(
            ignoreNonAudioApps = state.ignoreNonAudioApps,
            allowUnclassifiedApps = state.allowUnclassifiedApps,
            verboseDetailsEnabled = state.verboseDetailsEnabled,
            onAllowUnclassifiedAppsChanged = onAllowUnclassifiedAppsChanged,
            onVerboseDetailsChanged = onVerboseDetailsChanged,
            cleanupState = state.translationModelCleanup,
            onClearTranslationModels = onClearTranslationModels,
            onDismissTranslationModelCleanupFailure =
                onDismissTranslationModelCleanupFailure,
            onResetAALyrics = onResetAALyrics,
            onBack = { activeSubscreen = SettingsSubscreen.MAIN },
            modifier = modifier,
            bottomOverlayInset = bottomOverlayInset,
        )

        SettingsSubscreen.CHANGELOG -> ChangelogSettingsScreen(
            changelogText = state.changelogText,
            onBack = { activeSubscreen = SettingsSubscreen.MAIN },
            modifier = modifier,
            bottomOverlayInset = bottomOverlayInset,
        )

        SettingsSubscreen.PRIVACY_POLICY -> PrivacyPolicySettingsScreen(
            privacyPolicyText = state.privacyPolicyText,
            onBack = { activeSubscreen = SettingsSubscreen.MAIN },
            modifier = modifier,
            bottomOverlayInset = bottomOverlayInset,
        )

        SettingsSubscreen.TERMS_OF_USE -> TermsOfUseSettingsScreen(
            termsOfUseText = state.termsOfUseText,
            onBack = { activeSubscreen = SettingsSubscreen.MAIN },
            modifier = modifier,
            bottomOverlayInset = bottomOverlayInset,
        )

        SettingsSubscreen.LICENSE -> LicenseSettingsScreen(
            noticeText = state.noticeText,
            licenseText = state.licenseText,
            thirdPartyLicensesText = state.thirdPartyLicensesText,
            onBack = { activeSubscreen = SettingsSubscreen.MAIN },
            modifier = modifier,
            bottomOverlayInset = bottomOverlayInset,
        )

        SettingsSubscreen.HELP_FEEDBACK -> HelpFeedbackSettingsScreen(
            onDestinationSelected = onHelpFeedback,
            onBack = { activeSubscreen = SettingsSubscreen.MAIN },
            modifier = modifier,
            bottomOverlayInset = bottomOverlayInset,
        )

        SettingsSubscreen.SUPPORT_AALYRICS -> SupportAALyricsSettingsScreen(
            onSupport = onSupportAALyrics,
            onBack = { activeSubscreen = SettingsSubscreen.MAIN },
            modifier = modifier,
            bottomOverlayInset = bottomOverlayInset,
        )

        SettingsSubscreen.MAIN -> SettingsScreenContent(
            state = state,
            scrollState = mainScrollState,
            targetLanguagePickerVisible = targetLanguagePickerVisible,
            onTargetLanguagePickerVisibilityChanged = {
                targetLanguagePickerVisible = it
            },
            onPlainLyricsAutoScrollChanged = onPlainLyricsAutoScrollChanged,
            onIgnoreNonAudioAppsChanged = onIgnoreNonAudioAppsChanged,
            onAutomaticallyCheckForUpdatesChanged =
                onAutomaticallyCheckForUpdatesChanged,
            onTranslationEnabledChanged = onTranslationEnabledChanged,
            onTranslationTargetSelected = onTranslationTargetSelected,
            onTranslationModelDownloadRequested = onTranslationModelDownloadRequested,
            onAndroidAutoCompatibilitySetup = onAndroidAutoCompatibilitySetup,
            onCheckForUpdates = onCheckForUpdates,
            onChangelogRequested = { activeSubscreen = SettingsSubscreen.CHANGELOG },
            onPrivacyPolicyRequested = {
                activeSubscreen = SettingsSubscreen.PRIVACY_POLICY
            },
            onTermsOfUseRequested = {
                activeSubscreen = SettingsSubscreen.TERMS_OF_USE
            },
            onLicenseRequested = { activeSubscreen = SettingsSubscreen.LICENSE },
            onHelpFeedbackRequested = {
                activeSubscreen = SettingsSubscreen.HELP_FEEDBACK
            },
            onSupportAALyricsRequested = {
                activeSubscreen = SettingsSubscreen.SUPPORT_AALYRICS
            },
            onAdvancedRequested = { activeSubscreen = SettingsSubscreen.ADVANCED },
            onOpenGitHub = onOpenGitHub,
            modifier = modifier,
            bottomOverlayInset = bottomOverlayInset,
        )
    }

}

@Composable
internal fun SettingsScreenContent(
    state: SettingsScreenUiState,
    scrollState: ScrollState,
    targetLanguagePickerVisible: Boolean,
    onTargetLanguagePickerVisibilityChanged: (Boolean) -> Unit,
    onPlainLyricsAutoScrollChanged: (Boolean) -> Unit,
    onIgnoreNonAudioAppsChanged: (Boolean) -> Unit,
    onAutomaticallyCheckForUpdatesChanged: (Boolean) -> Unit,
    onTranslationEnabledChanged: (Boolean) -> Unit,
    onTranslationTargetSelected: (String) -> Unit,
    onTranslationModelDownloadRequested: (String) -> Unit,
    onAndroidAutoCompatibilitySetup: () -> Unit,
    onCheckForUpdates: () -> Unit,
    onChangelogRequested: () -> Unit,
    onPrivacyPolicyRequested: () -> Unit,
    onTermsOfUseRequested: () -> Unit,
    onLicenseRequested: () -> Unit,
    onHelpFeedbackRequested: () -> Unit,
    onSupportAALyricsRequested: () -> Unit,
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

            SettingsDivider()

            SettingsSwitchRow(
                title = stringResource(R.string.settings_ignore_non_audio_apps),
                checked = state.ignoreNonAudioApps,
                onCheckedChange = onIgnoreNonAudioAppsChanged,
                infoText = stringResource(R.string.settings_ignore_non_audio_apps_info),
                infoContentDescription =
                    stringResource(R.string.settings_ignore_non_audio_apps_info_description),
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
                infoText = stringResource(R.string.settings_translation_target_info),
                infoContentDescription =
                    stringResource(R.string.settings_translation_target_info_description),
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
            SettingsSwitchRow(
                title = stringResource(
                    R.string.settings_automatically_check_for_updates,
                ),
                checked = state.automaticallyCheckForUpdates,
                onCheckedChange = onAutomaticallyCheckForUpdatesChanged,
                infoText = stringResource(
                    R.string.settings_automatically_check_for_updates_info,
                ),
                infoContentDescription = stringResource(
                    R.string.settings_automatically_check_for_updates_info_description,
                ),
            )

            SettingsDivider()

            AppUpdateRow(
                versionLabel = stringResource(R.string.settings_version),
                currentVersionName = state.appVersionName,
                state = state.appUpdate,
                checkLabel = stringResource(R.string.settings_check_for_updates),
                checkingLabel = stringResource(R.string.settings_checking_for_updates),
                upToDateLabel = stringResource(R.string.settings_up_to_date),
                retryLabel = stringResource(R.string.settings_retry),
                checkFailedLabel = stringResource(R.string.settings_update_check_failed),
                failureInfoContentDescription =
                    stringResource(R.string.settings_update_failure_info),
                genericFailureReason =
                    stringResource(R.string.settings_update_failure_generic),
                onCheckForUpdates = onCheckForUpdates,
            )

            SettingsDivider()

            SettingsNavigationRow(
                title = stringResource(R.string.settings_changelog),
                value = null,
                onClick = onChangelogRequested,
            )

            SettingsDivider()

            SettingsExternalLinkRow(
                title = stringResource(R.string.settings_source_code),
                value = stringResource(R.string.settings_github),
                onClick = onOpenGitHub,
            )
        }

        Spacer(Modifier.height(AALyricsSpacing.Space20))

        SettingsSection(
            title = stringResource(R.string.settings_section_about_support),
        ) {
            SettingsNavigationRow(
                title = stringResource(R.string.settings_privacy_policy),
                value = null,
                onClick = onPrivacyPolicyRequested,
            )

            SettingsDivider()

            SettingsNavigationRow(
                title = stringResource(R.string.settings_terms_of_use),
                value = null,
                onClick = onTermsOfUseRequested,
            )

            SettingsDivider()

            SettingsNavigationRow(
                title = stringResource(R.string.settings_license),
                value = null,
                onClick = onLicenseRequested,
            )

            SettingsDivider()

            SettingsNavigationRow(
                title = stringResource(R.string.settings_help_feedback),
                value = null,
                onClick = onHelpFeedbackRequested,
            )

            SettingsDivider()

            SettingsNavigationRow(
                title = stringResource(R.string.settings_support_aalyrics),
                value = null,
                onClick = onSupportAALyricsRequested,
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
