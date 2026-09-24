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
fun UnifiedUpdateDialog(
    state: UpdateDialogUiState,
    onInstall: () -> Unit,
    onDownloadUpdate: () -> Unit,
    onRetryDownload: () -> Unit,
    onRetryInstall: () -> Unit,
    onGrantInstallPermission: () -> Unit,
    onDownloadFromGitHub: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    require(state.isUnifiedUpdateDialogState()) {
        "UnifiedUpdateDialog cannot render ${state.phase} / ${state.availabilityContext}"
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            dismissOnBackPress = true,
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
            UnifiedUpdateDialogContent(
                state = state,
                onInstall = onInstall,
                onDownloadUpdate = onDownloadUpdate,
                onRetryDownload = onRetryDownload,
                onRetryInstall = onRetryInstall,
                onGrantInstallPermission = onGrantInstallPermission,
                onDownloadFromGitHub = onDownloadFromGitHub,
                onDismissRequest = onDismissRequest,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp),
            )
        }
    }
}

@Composable
internal fun UnifiedUpdateDialogContent(
    state: UpdateDialogUiState,
    onInstall: () -> Unit,
    onDownloadUpdate: () -> Unit,
    onRetryDownload: () -> Unit,
    onRetryInstall: () -> Unit,
    onGrantInstallPermission: () -> Unit,
    onDownloadFromGitHub: () -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    require(state.isUnifiedUpdateDialogState()) {
        "UnifiedUpdateDialog cannot render ${state.phase} / ${state.availabilityContext}"
    }

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
                .verticalScroll(rememberScrollState())
                .padding(AALyricsSpacing.Space24),
        ) {
            PhoneDialogHeader(
                eyebrow = stringResource(R.string.update_process_eyebrow),
                closeContentDescription = stringResource(
                    R.string.update_process_close,
                ),
                onClose = onDismissRequest,
            )

            Spacer(Modifier.height(AALyricsSpacing.Space8))

            Text(
                text = stringResource(
                    when (state.phase) {
                        UpdateDialogPhase.AVAILABLE ->
                            R.string.update_process_retarget_title
                        UpdateDialogPhase.DOWNLOADING ->
                            R.string.update_process_downloading_title
                        UpdateDialogPhase.VERIFYING ->
                            R.string.update_process_verifying_title
                        UpdateDialogPhase.READY_TO_INSTALL ->
                            R.string.update_process_ready_title
                        UpdateDialogPhase.DOWNLOAD_FAILED ->
                            R.string.update_process_download_failed_title
                        UpdateDialogPhase.PREPARING_INSTALL ->
                            R.string.update_process_preparing_install_title
                        UpdateDialogPhase.PERMISSION_REQUIRED ->
                            R.string.update_process_permission_title
                        UpdateDialogPhase.INSTALLING ->
                            R.string.update_process_installing_title
                        UpdateDialogPhase.INSTALL_FAILED ->
                            R.string.update_process_install_failed_title
                        else ->
                            R.string.update_process_preparing_title
                    },
                ),
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
                when (state.phase) {
                    UpdateDialogPhase.AVAILABLE -> {
                        Text(
                            text = stringResource(
                                R.string.update_process_retarget_body_suffix,
                            ),
                            style = AALyricsTypography.TrackArtist,
                            color = AALyricsColors.TextSecondary,
                            modifier = Modifier.align(Alignment.CenterVertically),
                        )
                    }

                    UpdateDialogPhase.READY_TO_INSTALL -> {
                        Text(
                            text = stringResource(R.string.update_process_ready_body_suffix),
                            style = AALyricsTypography.TrackArtist,
                            color = AALyricsColors.TextSecondary,
                            modifier = Modifier.align(Alignment.CenterVertically),
                        )
                    }

                    UpdateDialogPhase.DOWNLOAD_FAILED -> {
                        Text(
                            text = stringResource(
                                R.string.update_process_download_failed_body_suffix,
                            ),
                            style = AALyricsTypography.TrackArtist,
                            color = AALyricsColors.TextSecondary,
                            modifier = Modifier.align(Alignment.CenterVertically),
                        )
                    }

                    UpdateDialogPhase.INSTALL_FAILED -> {
                        Text(
                            text = stringResource(
                                R.string.update_process_install_failed_body_suffix,
                            ),
                            style = AALyricsTypography.TrackArtist,
                            color = AALyricsColors.TextSecondary,
                            modifier = Modifier.align(Alignment.CenterVertically),
                        )
                    }

                    else -> Unit
                }
            }

            Spacer(Modifier.height(AALyricsSpacing.Space24))

            when (state.phase) {
                UpdateDialogPhase.AVAILABLE -> {
                    Button(
                        onClick = onDownloadUpdate,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = stringResource(R.string.update_process_download),
                        )
                    }
                }

                UpdateDialogPhase.DOWNLOADING -> {
                    val progress = (state.downloadProgress ?: 0f).coerceIn(0f, 1f)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.update_process_downloading_label),
                            style = AALyricsTypography.TrackArtist,
                            color = AALyricsColors.TextSecondary,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = "${(progress * 100f).roundToInt()}%",
                            style = AALyricsTypography.TrackArtist,
                            color = AALyricsColors.TextSecondary,
                        )
                    }

                    Spacer(Modifier.height(AALyricsSpacing.Space8))

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth(),
                        color = AALyricsColors.AccentCyan,
                        trackColor = AALyricsColors.OverlaySoft,
                    )
                }

                UpdateDialogPhase.PREPARING_DOWNLOAD -> {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = AALyricsColors.AccentCyan,
                        trackColor = AALyricsColors.OverlaySoft,
                    )
                }

                UpdateDialogPhase.VERIFYING -> {
                    Text(
                        text = stringResource(R.string.update_process_verifying_label),
                        style = AALyricsTypography.TrackArtist,
                        color = AALyricsColors.TextSecondary,
                    )

                    Spacer(Modifier.height(AALyricsSpacing.Space8))

                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = AALyricsColors.AccentCyan,
                        trackColor = AALyricsColors.OverlaySoft,
                    )
                }

                UpdateDialogPhase.READY_TO_INSTALL -> {
                    Button(
                        onClick = onInstall,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = stringResource(R.string.update_process_install),
                        )
                    }
                }

                UpdateDialogPhase.DOWNLOAD_FAILED -> {
                    Text(
                        text = stringResource(R.string.update_process_download_failed_reason),
                        style = AALyricsTypography.TrackArtist,
                        color = AALyricsColors.TextSecondary,
                    )

                    Spacer(Modifier.height(AALyricsSpacing.Space16))

                    Button(
                        onClick = onRetryDownload,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = stringResource(R.string.update_process_retry),
                        )
                    }
                }

                UpdateDialogPhase.PREPARING_INSTALL -> {
                    Text(
                        text = stringResource(R.string.update_process_preparing_install_label),
                        style = AALyricsTypography.TrackArtist,
                        color = AALyricsColors.TextSecondary,
                    )

                    Spacer(Modifier.height(AALyricsSpacing.Space8))

                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = AALyricsColors.AccentCyan,
                        trackColor = AALyricsColors.OverlaySoft,
                    )
                }

                UpdateDialogPhase.PERMISSION_REQUIRED -> {
                    Text(
                        text = stringResource(R.string.update_process_permission_body),
                        style = AALyricsTypography.TrackArtist,
                        color = AALyricsColors.TextSecondary,
                    )

                    Spacer(Modifier.height(AALyricsSpacing.Space20))

                    Surface(
                        shape = RoundedCornerShape(AALyricsRadius.Radius12),
                        color = AALyricsColors.OverlaySoft,
                    ) {
                        Text(
                            text = stringResource(R.string.update_process_permission_reason),
                            style = AALyricsTypography.TrackArtist,
                            color = AALyricsColors.TextSecondary,
                            modifier = Modifier.padding(AALyricsSpacing.Space16),
                        )
                    }

                    Spacer(Modifier.height(AALyricsSpacing.Space24))

                    Button(
                        onClick = onGrantInstallPermission,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = stringResource(R.string.update_process_grant_permission),
                        )
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
                                    R.string.update_process_download_from_github,
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

                UpdateDialogPhase.INSTALLING -> {
                    Text(
                        text = stringResource(R.string.update_process_installing_label),
                        style = AALyricsTypography.TrackArtist,
                        color = AALyricsColors.TextSecondary,
                    )

                    Spacer(Modifier.height(AALyricsSpacing.Space8))

                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = AALyricsColors.AccentCyan,
                        trackColor = AALyricsColors.OverlaySoft,
                    )
                }

                UpdateDialogPhase.INSTALL_FAILED -> {
                    Text(
                        text = updateDialogInstallFailureReasonText(
                            state.installFailureReason,
                        ),
                        style = AALyricsTypography.TrackArtist,
                        color = AALyricsColors.TextSecondary,
                    )

                    Spacer(Modifier.height(AALyricsSpacing.Space16))

                    Button(
                        onClick = onRetryInstall,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = stringResource(R.string.update_process_retry),
                        )
                    }
                }

                else -> Unit
            }
        }
    }
}


@Composable
private fun updateDialogInstallFailureReasonText(
    reason: UpdateDialogInstallFailureUiReason?,
): String =
    when (reason) {
        UpdateDialogInstallFailureUiReason.DEPENDENCIES_UNAVAILABLE ->
            stringResource(R.string.update_process_install_failure_dependencies_unavailable)
        UpdateDialogInstallFailureUiReason.RELEASE_REFRESH_FAILED ->
            stringResource(R.string.update_process_install_failure_release_refresh_failed)
        UpdateDialogInstallFailureUiReason.INSTALLED_VERSION_INVALID ->
            stringResource(R.string.update_process_install_failure_installed_version_invalid)
        UpdateDialogInstallFailureUiReason.RETAINED_VERSION_INVALID ->
            stringResource(R.string.update_process_install_failure_retained_version_invalid)
        UpdateDialogInstallFailureUiReason.RETAINED_RELEASE_NOT_ELIGIBLE ->
            stringResource(R.string.update_process_install_failure_retained_release_not_eligible)
        UpdateDialogInstallFailureUiReason.RETAINED_RELEASE_NOT_NEWER ->
            stringResource(R.string.update_process_install_failure_retained_release_not_newer)
        UpdateDialogInstallFailureUiReason.NO_ELIGIBLE_RELEASE ->
            stringResource(R.string.update_process_install_failure_no_eligible_release)
        UpdateDialogInstallFailureUiReason.RETAINED_RELEASE_NO_LONGER_CURRENT ->
            stringResource(
                R.string.update_process_install_failure_retained_release_no_longer_current,
            )
        UpdateDialogInstallFailureUiReason.APK_FILE_MISSING ->
            stringResource(R.string.update_process_install_failure_apk_missing)
        UpdateDialogInstallFailureUiReason.APK_NOT_CANONICAL ->
            stringResource(R.string.update_process_install_failure_apk_not_canonical)
        UpdateDialogInstallFailureUiReason.APK_UNREADABLE ->
            stringResource(R.string.update_process_install_failure_apk_unreadable)
        UpdateDialogInstallFailureUiReason.PACKAGE_MISMATCH ->
            stringResource(R.string.update_process_install_failure_package_mismatch)
        UpdateDialogInstallFailureUiReason.VERSION_NOT_NEWER ->
            stringResource(R.string.update_process_install_failure_version_not_newer)
        UpdateDialogInstallFailureUiReason.VERSION_NAME_MISMATCH ->
            stringResource(R.string.update_process_install_failure_version_name_mismatch)
        UpdateDialogInstallFailureUiReason.SIGNING_IDENTITY_UNAVAILABLE ->
            stringResource(R.string.update_process_install_failure_signing_unavailable)
        UpdateDialogInstallFailureUiReason.SIGNING_IDENTITY_MISMATCH ->
            stringResource(R.string.update_process_install_failure_signing_mismatch)
        UpdateDialogInstallFailureUiReason.RECOVERY_STATE_PERSISTENCE_FAILED ->
            stringResource(R.string.update_process_install_failure_recovery_state)
        UpdateDialogInstallFailureUiReason.INSTALLER_HANDOFF_FAILED ->
            stringResource(R.string.update_process_install_failure_handoff)
        UpdateDialogInstallFailureUiReason.INSTALLER_REJECTED ->
            stringResource(R.string.update_process_install_failure_installer_rejected)
        null -> stringResource(R.string.update_process_install_failure_generic)
    }

internal fun UpdateDialogUiState.isDismissibleProcessPresentation(): Boolean =
    isUnifiedUpdateDialogState()

internal fun UpdateDialogUiState.isUnifiedUpdateDialogState(): Boolean =
    phase.isUnifiedProcessPhase() ||
        (
            phase == UpdateDialogPhase.AVAILABLE &&
                availabilityContext == UpdateDialogAvailabilityContext.INSTALL_REFRESH_RETARGET
            )

internal fun UpdateDialogPhase.isUnifiedProcessPhase(): Boolean =
    this == UpdateDialogPhase.PREPARING_DOWNLOAD ||
        this == UpdateDialogPhase.DOWNLOADING ||
        this == UpdateDialogPhase.VERIFYING ||
        this == UpdateDialogPhase.READY_TO_INSTALL ||
        this == UpdateDialogPhase.DOWNLOAD_FAILED ||
        this == UpdateDialogPhase.PREPARING_INSTALL ||
        this == UpdateDialogPhase.PERMISSION_REQUIRED ||
        this == UpdateDialogPhase.INSTALLING ||
        this == UpdateDialogPhase.INSTALL_FAILED
