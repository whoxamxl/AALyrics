package io.github.whoxamxl.aalyrics.ui.phone.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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

@Composable
internal fun InstallPermissionDialog(
    state: InstallPermissionDialogUiState,
    onDismissRequest: () -> Unit,
    onGrantPermission: () -> Unit,
    onDownloadFromGitHub: () -> Unit,
) {
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
            InstallPermissionDialogContent(
                state = state,
                onDismissRequest = onDismissRequest,
                onGrantPermission = onGrantPermission,
                onDownloadFromGitHub = onDownloadFromGitHub,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp),
            )
        }
    }
}

@Composable
internal fun InstallPermissionDialogContent(
    state: InstallPermissionDialogUiState,
    onDismissRequest: () -> Unit,
    onGrantPermission: () -> Unit,
    onDownloadFromGitHub: () -> Unit,
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
            PhoneDialogHeader(
                eyebrow = stringResource(
                    R.string.settings_install_permission_dialog_eyebrow,
                ),
                closeContentDescription = stringResource(
                    R.string.settings_install_permission_dialog_close,
                ),
                onClose = onDismissRequest,
            )

            Text(
                text = stringResource(
                    R.string.settings_install_permission_dialog_version,
                    state.versionName,
                ),
                style = AALyricsTypography.Label,
                color = AALyricsColors.TextSecondary,
            )

            Spacer(Modifier.height(AALyricsSpacing.Space20))

            Text(
                text = stringResource(
                    R.string.settings_install_permission_dialog_title,
                ),
                style = AALyricsTypography.LyricsSupporting,
                color = AALyricsColors.TextPrimary,
            )

            Spacer(Modifier.height(AALyricsSpacing.Space16))

            Text(
                text = stringResource(
                    R.string.settings_install_permission_dialog_body,
                ),
                style = AALyricsTypography.TrackArtist,
                color = AALyricsColors.TextSecondary,
            )

            Spacer(Modifier.height(AALyricsSpacing.Space20))

            Surface(
                shape = RoundedCornerShape(AALyricsRadius.Radius12),
                color = AALyricsColors.OverlaySoft,
            ) {
                Text(
                    text = stringResource(
                        R.string.settings_install_permission_dialog_reason,
                    ),
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
                Text(
                    text = stringResource(
                        R.string.settings_install_permission_dialog_grant,
                    ),
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
    }
}
