package io.github.whoxamxl.aalyrics.ui.phone.update

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.whoxamxl.aalyrics.ui.designsystem.icon.AALyricsIcons
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsColors
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsRadius
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsSpacing
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsStroke
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsTypography
import io.github.whoxamxl.aalyrics.ui.phone.R
import io.github.whoxamxl.aalyrics.ui.phone.component.PhoneDialogHeader
import io.github.whoxamxl.aalyrics.ui.phone.component.VersionChip
import kotlin.math.roundToInt

@Composable
fun UpdateDialog(
    state: UpdateDialogUiState,
    onUpdate: () -> Unit,
    onDismissAvailable: () -> Unit,
    onRetryDownload: () -> Unit,
    onInstall: () -> Unit,
    onGrantPermission: () -> Unit,
    onDownloadFromGitHub: () -> Unit,
    onRetryInstall: () -> Unit,
) {
    val dismissible = state.phase == UpdateDialogPhase.AVAILABLE

    Dialog(
        onDismissRequest = {
            if (dismissible) {
                onDismissAvailable()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = dismissible,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = AALyricsSpacing.Space16,
                    vertical = AALyricsSpacing.Space24,
                ),
            contentAlignment = Alignment.Center,
        ) {
            UpdateDialogContent(
                state = state,
                onUpdate = onUpdate,
                onDismissAvailable = onDismissAvailable,
                onRetryDownload = onRetryDownload,
                onInstall = onInstall,
                onGrantPermission = onGrantPermission,
                onDownloadFromGitHub = onDownloadFromGitHub,
                onRetryInstall = onRetryInstall,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(
                        max = if (
                            state.phase == UpdateDialogPhase.INSTALL_PERMISSION_REQUIRED
                        ) {
                            520.dp
                        } else {
                            420.dp
                        },
                    ),
            )
        }
    }
}

@Composable
internal fun UpdateDialogContent(
    state: UpdateDialogUiState,
    onUpdate: () -> Unit,
    onDismissAvailable: () -> Unit,
    onRetryDownload: () -> Unit,
    onInstall: () -> Unit,
    onGrantPermission: () -> Unit,
    onDownloadFromGitHub: () -> Unit,
    onRetryInstall: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(AALyricsRadius.Radius24),
        color = AALyricsColors.BackgroundSurface,
        border = BorderStroke(
            width = AALyricsStroke.Thin,
            color = AALyricsColors.BorderSoft,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(AALyricsSpacing.Space24),
        ) {
            when (state.phase) {
                UpdateDialogPhase.AVAILABLE -> AvailableContent(
                    state = state,
                    onUpdate = onUpdate,
                    onDismissAvailable = onDismissAvailable,
                )

                UpdateDialogPhase.PREPARING_DOWNLOAD -> ProgressContent(
                    title = stringResource(R.string.update_dialog_preparing_download),
                    versionName = state.versionName,
                )

                UpdateDialogPhase.DOWNLOADING -> DownloadingContent(state)

                UpdateDialogPhase.DOWNLOADED -> DownloadedContent(
                    state = state,
                    onInstall = onInstall,
                )

                UpdateDialogPhase.DOWNLOAD_FAILED -> FailureContent(
                    title = stringResource(R.string.update_dialog_download_failed),
                    versionName = state.versionName,
                    reason = stringResource(R.string.update_dialog_download_failed_body),
                    onRetry = onRetryDownload,
                )

                UpdateDialogPhase.PREPARING_INSTALL -> ProgressContent(
                    title = stringResource(R.string.update_dialog_preparing_install),
                    versionName = state.versionName,
                )

                UpdateDialogPhase.INSTALL_PERMISSION_REQUIRED -> PermissionContent(
                    state = state,
                    onGrantPermission = onGrantPermission,
                    onDownloadFromGitHub = onDownloadFromGitHub,
                )

                UpdateDialogPhase.INSTALLING -> ProgressContent(
                    title = stringResource(R.string.update_dialog_installing),
                    versionName = state.versionName,
                    body = stringResource(R.string.update_dialog_installing_body),
                )

                UpdateDialogPhase.INSTALL_FAILED -> FailureContent(
                    title = stringResource(R.string.update_dialog_install_failed),
                    versionName = state.versionName,
                    reason = installFailureReasonText(state.installFailureReason),
                    onRetry = onRetryInstall,
                )
            }
        }
    }
}

@Composable
private fun AvailableContent(
    state: UpdateDialogUiState,
    onUpdate: () -> Unit,
    onDismissAvailable: () -> Unit,
) {
    PhoneDialogHeader(
        eyebrow = stringResource(R.string.update_release_available_eyebrow),
        closeContentDescription = stringResource(
            R.string.update_release_available_close,
        ),
        onClose = onDismissAvailable,
    )

    Spacer(Modifier.height(AALyricsSpacing.Space8))

    Text(
        text = stringResource(R.string.update_release_available_title),
        style = AALyricsTypography.LyricsSupporting,
        color = AALyricsColors.TextPrimary,
    )

    Spacer(Modifier.height(AALyricsSpacing.Space16))

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(AALyricsSpacing.Space4),
        verticalArrangement = Arrangement.spacedBy(AALyricsSpacing.Space4),
    ) {
        Text(
            text = stringResource(R.string.update_release_available_body_prefix),
            style = AALyricsTypography.TrackArtist,
            color = AALyricsColors.TextSecondary,
            modifier = Modifier.align(Alignment.CenterVertically),
        )
        VersionChip(
            versionName = state.versionName,
            modifier = Modifier.align(Alignment.CenterVertically),
        )
        Text(
            text = stringResource(R.string.update_release_available_body_suffix),
            style = AALyricsTypography.TrackArtist,
            color = AALyricsColors.TextSecondary,
            modifier = Modifier.align(Alignment.CenterVertically),
        )
    }

    Spacer(Modifier.height(AALyricsSpacing.Space32))

    Button(
        onClick = onUpdate,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(text = stringResource(R.string.update_release_available_update))
    }

    Spacer(Modifier.height(AALyricsSpacing.Space12))

    OutlinedButton(
        onClick = onDismissAvailable,
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(
            width = AALyricsStroke.Thin,
            color = AALyricsColors.BorderSoft,
        ),
    ) {
        Text(
            text = stringResource(R.string.update_release_available_not_now),
            color = AALyricsColors.TextSecondary,
        )
    }
}

@Composable
private fun ProcessHeader() {
    Text(
        text = stringResource(R.string.update_dialog_eyebrow),
        style = AALyricsTypography.Label,
        color = AALyricsColors.AccentCyan,
    )
}

@Composable
private fun ProgressContent(
    title: String,
    versionName: String,
    body: String? = null,
) {
    ProcessHeader()
    Spacer(Modifier.height(AALyricsSpacing.Space12))
    Text(
        text = title,
        style = AALyricsTypography.LyricsSupporting,
        color = AALyricsColors.TextPrimary,
    )
    Spacer(Modifier.height(AALyricsSpacing.Space12))
    VersionChip(versionName = versionName)

    body?.let {
        Spacer(Modifier.height(AALyricsSpacing.Space16))
        Text(
            text = it,
            style = AALyricsTypography.TrackArtist,
            color = AALyricsColors.TextSecondary,
        )
    }

    Spacer(Modifier.height(AALyricsSpacing.Space24))
    LinearProgressIndicator(
        modifier = Modifier.fillMaxWidth(),
        color = AALyricsColors.AccentCyan,
        trackColor = AALyricsColors.OverlaySoft,
    )
}

@Composable
private fun DownloadingContent(
    state: UpdateDialogUiState,
) {
    val progress = (state.downloadProgress ?: 0f).coerceIn(0f, 1f)

    ProcessHeader()
    Spacer(Modifier.height(AALyricsSpacing.Space12))
    Text(
        text = stringResource(R.string.update_dialog_downloading),
        style = AALyricsTypography.LyricsSupporting,
        color = AALyricsColors.TextPrimary,
    )
    Spacer(Modifier.height(AALyricsSpacing.Space12))
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        VersionChip(versionName = state.versionName)
        Spacer(Modifier.weight(1f))
        Text(
            text = "${(progress * 100f).roundToInt()}%",
            style = AALyricsTypography.TrackArtist,
            color = AALyricsColors.TextSecondary,
        )
    }
    Spacer(Modifier.height(AALyricsSpacing.Space16))
    LinearProgressIndicator(
        progress = { progress },
        modifier = Modifier.fillMaxWidth(),
        color = AALyricsColors.AccentCyan,
        trackColor = AALyricsColors.OverlaySoft,
    )
}

@Composable
private fun DownloadedContent(
    state: UpdateDialogUiState,
    onInstall: () -> Unit,
) {
    ProcessHeader()
    Spacer(Modifier.height(AALyricsSpacing.Space12))
    Text(
        text = stringResource(R.string.update_dialog_downloaded),
        style = AALyricsTypography.LyricsSupporting,
        color = AALyricsColors.TextPrimary,
    )
    Spacer(Modifier.height(AALyricsSpacing.Space12))
    VersionChip(versionName = state.versionName)
    Spacer(Modifier.height(AALyricsSpacing.Space16))
    Text(
        text = stringResource(R.string.update_dialog_downloaded_body),
        style = AALyricsTypography.TrackArtist,
        color = AALyricsColors.TextSecondary,
    )
    Spacer(Modifier.height(AALyricsSpacing.Space32))
    Button(
        onClick = onInstall,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(text = stringResource(R.string.update_dialog_install))
    }
}

@Composable
private fun FailureContent(
    title: String,
    versionName: String,
    reason: String,
    onRetry: () -> Unit,
) {
    ProcessHeader()
    Spacer(Modifier.height(AALyricsSpacing.Space12))
    Text(
        text = title,
        style = AALyricsTypography.LyricsSupporting,
        color = AALyricsColors.Error,
    )
    Spacer(Modifier.height(AALyricsSpacing.Space12))
    VersionChip(versionName = versionName)
    Spacer(Modifier.height(AALyricsSpacing.Space16))
    Text(
        text = reason,
        style = AALyricsTypography.TrackArtist,
        color = AALyricsColors.TextSecondary,
    )
    Spacer(Modifier.height(AALyricsSpacing.Space32))
    Button(
        onClick = onRetry,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = AALyricsIcons.Retry,
                contentDescription = null,
                modifier = Modifier.size(AALyricsSpacing.Space16),
            )
            Spacer(Modifier.size(AALyricsSpacing.Space8))
            Text(text = stringResource(R.string.update_dialog_retry))
        }
    }
}

@Composable
private fun PermissionContent(
    state: UpdateDialogUiState,
    onGrantPermission: () -> Unit,
    onDownloadFromGitHub: () -> Unit,
) {
    ProcessHeader()
    Spacer(Modifier.height(AALyricsSpacing.Space12))
    Text(
        text = stringResource(R.string.settings_install_permission_dialog_version),
        style = AALyricsTypography.Label,
        color = AALyricsColors.TextSecondary,
    )
    Spacer(Modifier.height(AALyricsSpacing.Space4))
    VersionChip(versionName = state.versionName)
    Spacer(Modifier.height(AALyricsSpacing.Space20))
    Text(
        text = stringResource(R.string.settings_install_permission_dialog_title),
        style = AALyricsTypography.LyricsSupporting,
        color = AALyricsColors.TextPrimary,
    )
    Spacer(Modifier.height(AALyricsSpacing.Space16))
    Text(
        text = stringResource(R.string.settings_install_permission_dialog_body),
        style = AALyricsTypography.TrackArtist,
        color = AALyricsColors.TextSecondary,
    )
    Spacer(Modifier.height(AALyricsSpacing.Space20))
    Surface(
        shape = RoundedCornerShape(AALyricsRadius.Radius12),
        color = AALyricsColors.OverlaySoft,
    ) {
        Text(
            text = stringResource(R.string.settings_install_permission_dialog_reason),
            style = AALyricsTypography.TrackArtist,
            color = AALyricsColors.TextSecondary,
            modifier = Modifier.padding(AALyricsSpacing.Space16),
        )
    }
    Spacer(Modifier.height(AALyricsSpacing.Space32))
    Button(
        onClick = onGrantPermission,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(text = stringResource(R.string.settings_install_permission_dialog_grant))
    }
    Spacer(Modifier.height(AALyricsSpacing.Space12))
    OutlinedButton(
        onClick = onDownloadFromGitHub,
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(
            width = AALyricsStroke.Thin,
            color = AALyricsColors.BorderSoft,
        ),
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(
                    R.string.settings_install_permission_dialog_download_github,
                ),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.size(AALyricsSpacing.Space8))
            Icon(
                imageVector = AALyricsIcons.ExternalLink,
                contentDescription = null,
                modifier = Modifier.size(AALyricsSpacing.Space16),
            )
        }
    }
}

@Composable
private fun installFailureReasonText(
    reason: UpdateInstallFailureUiReason?,
): String =
    when (reason) {
        UpdateInstallFailureUiReason.DEPENDENCIES_UNAVAILABLE ->
            stringResource(R.string.settings_update_install_failure_dependencies_unavailable)
        UpdateInstallFailureUiReason.RELEASE_REFRESH_FAILED ->
            stringResource(R.string.settings_update_install_failure_release_refresh_failed)
        UpdateInstallFailureUiReason.INSTALLED_VERSION_INVALID ->
            stringResource(R.string.settings_update_install_failure_installed_version_invalid)
        UpdateInstallFailureUiReason.RETAINED_VERSION_INVALID ->
            stringResource(R.string.settings_update_install_failure_retained_version_invalid)
        UpdateInstallFailureUiReason.RETAINED_RELEASE_NOT_ELIGIBLE ->
            stringResource(R.string.settings_update_install_failure_retained_release_not_eligible)
        UpdateInstallFailureUiReason.RETAINED_RELEASE_NOT_NEWER ->
            stringResource(R.string.settings_update_install_failure_retained_release_not_newer)
        UpdateInstallFailureUiReason.NO_ELIGIBLE_RELEASE ->
            stringResource(R.string.settings_update_install_failure_no_eligible_release)
        UpdateInstallFailureUiReason.RETAINED_RELEASE_NO_LONGER_CURRENT ->
            stringResource(
                R.string.settings_update_install_failure_retained_release_no_longer_current,
            )
        UpdateInstallFailureUiReason.APK_FILE_MISSING ->
            stringResource(R.string.settings_update_install_failure_apk_missing)
        UpdateInstallFailureUiReason.APK_NOT_CANONICAL ->
            stringResource(R.string.settings_update_install_failure_apk_not_canonical)
        UpdateInstallFailureUiReason.APK_UNREADABLE ->
            stringResource(R.string.settings_update_install_failure_apk_unreadable)
        UpdateInstallFailureUiReason.PACKAGE_MISMATCH ->
            stringResource(R.string.settings_update_install_failure_package_mismatch)
        UpdateInstallFailureUiReason.VERSION_NOT_NEWER ->
            stringResource(R.string.settings_update_install_failure_version_not_newer)
        UpdateInstallFailureUiReason.VERSION_NAME_MISMATCH ->
            stringResource(R.string.settings_update_install_failure_version_name_mismatch)
        UpdateInstallFailureUiReason.SIGNING_IDENTITY_UNAVAILABLE ->
            stringResource(R.string.settings_update_install_failure_signing_unavailable)
        UpdateInstallFailureUiReason.SIGNING_IDENTITY_MISMATCH ->
            stringResource(R.string.settings_update_install_failure_signing_mismatch)
        UpdateInstallFailureUiReason.RECOVERY_STATE_PERSISTENCE_FAILED ->
            stringResource(R.string.settings_update_install_failure_recovery_state)
        UpdateInstallFailureUiReason.INSTALLER_HANDOFF_FAILED ->
            stringResource(R.string.settings_update_install_failure_handoff)
        UpdateInstallFailureUiReason.INSTALLER_REJECTED ->
            stringResource(R.string.settings_update_install_failure_installer_rejected)
        null -> stringResource(R.string.settings_update_install_failure_generic)
    }
