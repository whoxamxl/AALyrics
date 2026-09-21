package io.github.whoxamxl.aalyrics.ui.phone.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import io.github.whoxamxl.aalyrics.ui.phone.R

/** Second-level Settings surface for narrowly scoped debug, storage, and reset actions. */
@Composable
fun AdvancedSettingsScreen(
    verboseDetailsEnabled: Boolean,
    cleanupState: TranslationModelCleanupUiState,
    onVerboseDetailsChanged: (Boolean) -> Unit,
    onClearTranslationModels: () -> Unit,
    onDismissTranslationModelCleanupFailure: () -> Unit,
    onResetAALyrics: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    bottomOverlayInset: Dp = 0.dp,
) {
    var clearModelsDialogVisible by rememberSaveable { mutableStateOf(false) }
    var resetDialogVisible by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                start = AALyricsSpacing.Space16,
                top = AALyricsSpacing.Space8,
                end = AALyricsSpacing.Space16,
                bottom = bottomOverlayInset + AALyricsSpacing.Space16,
            ),
    ) {
        SettingsSubscreenHeader(
            title = stringResource(R.string.settings_advanced),
            backContentDescription = stringResource(R.string.settings_back),
            onBack = onBack,
        )

        Spacer(Modifier.height(AALyricsSpacing.Space12))

        SettingsSection(
            title = stringResource(R.string.settings_section_debug),
        ) {
            SettingsSwitchRow(
                title = stringResource(R.string.settings_verbose_details),
                checked = verboseDetailsEnabled,
                onCheckedChange = onVerboseDetailsChanged,
            )
        }

        Spacer(Modifier.height(AALyricsSpacing.Space20))

        SettingsSection(
            title = stringResource(R.string.settings_section_experimental),
        ) {
            SettingsSwitchRow(
                title = stringResource(R.string.settings_karaoke_mode),
                checked = false,
                onCheckedChange = {},
                enabled = false,
                infoText = stringResource(R.string.settings_karaoke_unavailable),
                infoContentDescription =
                    stringResource(R.string.settings_karaoke_unavailable_description),
            )
        }

        Spacer(Modifier.height(AALyricsSpacing.Space20))

        SettingsSection(
            title = stringResource(R.string.settings_section_storage),
        ) {
            SettingsActionRow(
                title = stringResource(R.string.settings_clear_translation_models),
                actionLabel = stringResource(R.string.settings_clear),
                infoText = stringResource(R.string.settings_clear_translation_models_info),
                infoContentDescription =
                    stringResource(R.string.settings_clear_translation_models_info_description),
                onClick = {
                    if (cleanupState != TranslationModelCleanupUiState.RUNNING) {
                        clearModelsDialogVisible = true
                    }
                },
            )
        }

        Spacer(Modifier.height(AALyricsSpacing.Space20))

        SettingsSection(
            title = stringResource(R.string.settings_section_reset),
        ) {
            SettingsActionRow(
                title = stringResource(R.string.settings_reset_aalyrics),
                actionLabel = stringResource(R.string.settings_reset),
                actionColor = AALyricsColors.Error,
                infoText = stringResource(R.string.settings_reset_aalyrics_info),
                infoContentDescription =
                    stringResource(R.string.settings_reset_aalyrics_info_description),
                onClick = { resetDialogVisible = true },
            )
        }
    }

    if (clearModelsDialogVisible) {
        SettingsConfirmationDialog(
            title = stringResource(R.string.settings_clear_translation_models_dialog_title),
            text = stringResource(R.string.settings_clear_translation_models_dialog_body),
            confirmLabel = stringResource(R.string.settings_clear),
            dismissLabel = stringResource(R.string.settings_cancel),
            confirmColor = AALyricsColors.Error,
            onConfirm = {
                clearModelsDialogVisible = false
                onClearTranslationModels()
            },
            onDismissRequest = { clearModelsDialogVisible = false },
        )
    }

    if (cleanupState == TranslationModelCleanupUiState.FAILED) {
        SettingsConfirmationDialog(
            title = stringResource(R.string.settings_clear_translation_models_failed_title),
            text = stringResource(R.string.settings_clear_translation_models_failed_body),
            confirmLabel = stringResource(R.string.settings_retry),
            dismissLabel = stringResource(R.string.settings_close),
            confirmColor = AALyricsColors.Error,
            onConfirm = onClearTranslationModels,
            onDismissRequest = onDismissTranslationModelCleanupFailure,
        )
    }

    if (resetDialogVisible) {
        SettingsConfirmationDialog(
            title = stringResource(R.string.settings_reset_aalyrics_dialog_title),
            text = stringResource(R.string.settings_reset_aalyrics_dialog_body),
            confirmLabel = stringResource(R.string.settings_reset),
            dismissLabel = stringResource(R.string.settings_cancel),
            confirmColor = AALyricsColors.Error,
            onConfirm = {
                resetDialogVisible = false
                onResetAALyrics()
            },
            onDismissRequest = { resetDialogVisible = false },
        )
    }
}
