package io.github.whoxamxl.aalyrics.ui.phone.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsColors
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsSpacing
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsTheme
import io.github.whoxamxl.aalyrics.ui.phone.update.UpdateDialogContent
import io.github.whoxamxl.aalyrics.ui.phone.update.UpdateDialogPhase
import io.github.whoxamxl.aalyrics.ui.phone.update.UpdateDialogUiState
import io.github.whoxamxl.aalyrics.ui.phone.update.UpdateInstallFailureUiReason

@Preview(name = "Available", group = "UpdateDialog", widthDp = 412, heightDp = 892)
@Composable
private fun UpdateDialogAvailablePreview() {
    UpdateDialogPreview(
        UpdateDialogUiState(
            phase = UpdateDialogPhase.AVAILABLE,
            versionName = "0.3.0-alpha.1",
        ),
    )
}

@Preview(name = "Downloading 64%", group = "UpdateDialog", widthDp = 412, heightDp = 892)
@Composable
private fun UpdateDialogDownloadingPreview() {
    UpdateDialogPreview(
        UpdateDialogUiState(
            phase = UpdateDialogPhase.DOWNLOADING,
            versionName = "0.3.0-alpha.1",
            downloadProgress = 0.64f,
        ),
    )
}

@Preview(name = "Ready to install", group = "UpdateDialog", widthDp = 412, heightDp = 892)
@Composable
private fun UpdateDialogDownloadedPreview() {
    UpdateDialogPreview(
        UpdateDialogUiState(
            phase = UpdateDialogPhase.DOWNLOADED,
            versionName = "0.3.0-alpha.1",
        ),
    )
}

@Preview(name = "Permission required", group = "UpdateDialog", widthDp = 412, heightDp = 892)
@Composable
private fun UpdateDialogPermissionPreview() {
    UpdateDialogPreview(
        UpdateDialogUiState(
            phase = UpdateDialogPhase.INSTALL_PERMISSION_REQUIRED,
            versionName = "0.3.0-alpha.1",
        ),
        maxWidth = 520.dp,
    )
}

@Preview(name = "Download failed", group = "UpdateDialog", widthDp = 412, heightDp = 892)
@Composable
private fun UpdateDialogDownloadFailedPreview() {
    UpdateDialogPreview(
        UpdateDialogUiState(
            phase = UpdateDialogPhase.DOWNLOAD_FAILED,
            versionName = "0.3.0-alpha.1",
        ),
    )
}

@Preview(name = "Install failed · signing", group = "UpdateDialog", widthDp = 412, heightDp = 892)
@Composable
private fun UpdateDialogInstallFailedPreview() {
    UpdateDialogPreview(
        UpdateDialogUiState(
            phase = UpdateDialogPhase.INSTALL_FAILED,
            versionName = "0.3.0-alpha.1",
            installFailureReason = UpdateInstallFailureUiReason.SIGNING_IDENTITY_MISMATCH,
        ),
    )
}

@Preview(name = "Narrow · available", group = "UpdateDialog", widthDp = 320, heightDp = 720)
@Composable
private fun UpdateDialogNarrowPreview() {
    UpdateDialogAvailablePreview()
}

@Preview(
    name = "Large font · permission",
    group = "UpdateDialog",
    widthDp = 412,
    heightDp = 892,
    fontScale = 1.3f,
)
@Composable
private fun UpdateDialogLargeFontPreview() {
    UpdateDialogPermissionPreview()
}

@Composable
private fun UpdateDialogPreview(
    state: UpdateDialogUiState,
    maxWidth: androidx.compose.ui.unit.Dp = 420.dp,
) {
    AALyricsTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AALyricsColors.BackgroundBase)
                .padding(
                    horizontal = AALyricsSpacing.Space16,
                    vertical = AALyricsSpacing.Space24,
                ),
            contentAlignment = Alignment.Center,
        ) {
            UpdateDialogContent(
                state = state,
                onUpdate = {},
                onDismissAvailable = {},
                onRetryDownload = {},
                onInstall = {},
                onGrantPermission = {},
                onDownloadFromGitHub = {},
                onRetryInstall = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = maxWidth),
            )
        }
    }
}
