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
import io.github.whoxamxl.aalyrics.ui.phone.update.UnifiedUpdateDialogContent
import io.github.whoxamxl.aalyrics.ui.phone.update.UpdateDialogAvailabilityContext
import io.github.whoxamxl.aalyrics.ui.phone.update.UpdateDialogInstallFailureUiReason
import io.github.whoxamxl.aalyrics.ui.phone.update.UpdateDialogPhase
import io.github.whoxamxl.aalyrics.ui.phone.update.UpdateDialogUiState

@Preview(
    name = "Install refresh · newer release",
    group = "UnifiedUpdateDialog",
    widthDp = 412,
    heightDp = 892,
    showBackground = true,
)
@Composable
private fun UnifiedUpdateInstallRefreshRetargetPreview() {
    UnifiedUpdateDialogPreview(
        state = UpdateDialogUiState(
            phase = UpdateDialogPhase.AVAILABLE,
            versionName = "0.3.0-beta.1",
            availabilityContext = UpdateDialogAvailabilityContext.INSTALL_REFRESH_RETARGET,
        ),
    )
}

@Preview(
    name = "Install refresh · narrow phone",
    group = "UnifiedUpdateDialog",
    widthDp = 320,
    heightDp = 720,
    showBackground = true,
)
@Composable
private fun UnifiedUpdateInstallRefreshRetargetNarrowPreview() {
    UnifiedUpdateDialogPreview(
        state = UpdateDialogUiState(
            phase = UpdateDialogPhase.AVAILABLE,
            versionName = "0.3.0-beta.1",
            availabilityContext = UpdateDialogAvailabilityContext.INSTALL_REFRESH_RETARGET,
        ),
    )
}

@Preview(
    name = "Install refresh · large font",
    group = "UnifiedUpdateDialog",
    widthDp = 412,
    heightDp = 892,
    fontScale = 1.3f,
    showBackground = true,
)
@Composable
private fun UnifiedUpdateInstallRefreshRetargetLargeFontPreview() {
    UnifiedUpdateDialogPreview(
        state = UpdateDialogUiState(
            phase = UpdateDialogPhase.AVAILABLE,
            versionName = "0.3.0-beta.1",
            availabilityContext = UpdateDialogAvailabilityContext.INSTALL_REFRESH_RETARGET,
        ),
    )
}

@Preview(
    name = "Preparing download",
    group = "UnifiedUpdateDialog",
    widthDp = 412,
    heightDp = 892,
    showBackground = true,
)
@Composable
private fun UnifiedUpdatePreparingPreview() {
    UnifiedUpdateDialogPreview(
        state = UpdateDialogUiState(
            phase = UpdateDialogPhase.PREPARING_DOWNLOAD,
            versionName = "0.3.0-alpha.1",
        ),
    )
}

@Preview(
    name = "Downloading · 64%",
    group = "UnifiedUpdateDialog",
    widthDp = 412,
    heightDp = 892,
    showBackground = true,
)
@Composable
private fun UnifiedUpdateDownloadingPreview() {
    UnifiedUpdateDialogPreview(
        state = UpdateDialogUiState(
            phase = UpdateDialogPhase.DOWNLOADING,
            versionName = "0.3.0-alpha.1",
            downloadProgress = 0.64f,
        ),
    )
}

@Preview(
    name = "Downloading · narrow phone",
    group = "UnifiedUpdateDialog",
    widthDp = 320,
    heightDp = 720,
    showBackground = true,
)
@Composable
private fun UnifiedUpdateDownloadingNarrowPreview() {
    UnifiedUpdateDialogPreview(
        state = UpdateDialogUiState(
            phase = UpdateDialogPhase.DOWNLOADING,
            versionName = "0.3.0-alpha.1",
            downloadProgress = 0.64f,
        ),
    )
}

@Preview(
    name = "Downloading · large font",
    group = "UnifiedUpdateDialog",
    widthDp = 412,
    heightDp = 892,
    fontScale = 1.3f,
    showBackground = true,
)
@Composable
private fun UnifiedUpdateDownloadingLargeFontPreview() {
    UnifiedUpdateDialogPreview(
        state = UpdateDialogUiState(
            phase = UpdateDialogPhase.DOWNLOADING,
            versionName = "0.3.0-alpha.1",
            downloadProgress = 0.64f,
        ),
    )
}

@Preview(
    name = "Verifying update",
    group = "UnifiedUpdateDialog",
    widthDp = 412,
    heightDp = 892,
    showBackground = true,
)
@Composable
private fun UnifiedUpdateVerifyingPreview() {
    UnifiedUpdateDialogPreview(
        state = UpdateDialogUiState(
            phase = UpdateDialogPhase.VERIFYING,
            versionName = "0.3.0-alpha.1",
        ),
    )
}

@Preview(
    name = "Verifying · narrow phone",
    group = "UnifiedUpdateDialog",
    widthDp = 320,
    heightDp = 720,
    showBackground = true,
)
@Composable
private fun UnifiedUpdateVerifyingNarrowPreview() {
    UnifiedUpdateDialogPreview(
        state = UpdateDialogUiState(
            phase = UpdateDialogPhase.VERIFYING,
            versionName = "0.3.0-alpha.1",
        ),
    )
}

@Preview(
    name = "Verifying · large font",
    group = "UnifiedUpdateDialog",
    widthDp = 412,
    heightDp = 892,
    fontScale = 1.3f,
    showBackground = true,
)
@Composable
private fun UnifiedUpdateVerifyingLargeFontPreview() {
    UnifiedUpdateDialogPreview(
        state = UpdateDialogUiState(
            phase = UpdateDialogPhase.VERIFYING,
            versionName = "0.3.0-alpha.1",
        ),
    )
}

@Preview(
    name = "Ready to install",
    group = "UnifiedUpdateDialog",
    widthDp = 412,
    heightDp = 892,
    showBackground = true,
)
@Composable
private fun UnifiedUpdateReadyToInstallPreview() {
    UnifiedUpdateDialogPreview(
        state = UpdateDialogUiState(
            phase = UpdateDialogPhase.READY_TO_INSTALL,
            versionName = "0.3.0-alpha.1",
        ),
    )
}

@Preview(
    name = "Ready to install · narrow phone",
    group = "UnifiedUpdateDialog",
    widthDp = 320,
    heightDp = 720,
    showBackground = true,
)
@Composable
private fun UnifiedUpdateReadyToInstallNarrowPreview() {
    UnifiedUpdateDialogPreview(
        state = UpdateDialogUiState(
            phase = UpdateDialogPhase.READY_TO_INSTALL,
            versionName = "0.3.0-alpha.1",
        ),
    )
}

@Preview(
    name = "Ready to install · large font",
    group = "UnifiedUpdateDialog",
    widthDp = 412,
    heightDp = 892,
    fontScale = 1.3f,
    showBackground = true,
)
@Composable
private fun UnifiedUpdateReadyToInstallLargeFontPreview() {
    UnifiedUpdateDialogPreview(
        state = UpdateDialogUiState(
            phase = UpdateDialogPhase.READY_TO_INSTALL,
            versionName = "0.3.0-alpha.1",
        ),
    )
}

@Preview(
    name = "Download failed",
    group = "UnifiedUpdateDialog",
    widthDp = 412,
    heightDp = 892,
    showBackground = true,
)
@Composable
private fun UnifiedUpdateDownloadFailedPreview() {
    UnifiedUpdateDialogPreview(
        state = UpdateDialogUiState(
            phase = UpdateDialogPhase.DOWNLOAD_FAILED,
            versionName = "0.3.0-alpha.1",
        ),
    )
}

@Preview(
    name = "Download failed · narrow phone",
    group = "UnifiedUpdateDialog",
    widthDp = 320,
    heightDp = 720,
    showBackground = true,
)
@Composable
private fun UnifiedUpdateDownloadFailedNarrowPreview() {
    UnifiedUpdateDialogPreview(
        state = UpdateDialogUiState(
            phase = UpdateDialogPhase.DOWNLOAD_FAILED,
            versionName = "0.3.0-alpha.1",
        ),
    )
}

@Preview(
    name = "Download failed · large font",
    group = "UnifiedUpdateDialog",
    widthDp = 412,
    heightDp = 892,
    fontScale = 1.3f,
    showBackground = true,
)
@Composable
private fun UnifiedUpdateDownloadFailedLargeFontPreview() {
    UnifiedUpdateDialogPreview(
        state = UpdateDialogUiState(
            phase = UpdateDialogPhase.DOWNLOAD_FAILED,
            versionName = "0.3.0-alpha.1",
        ),
    )
}

@Preview(
    name = "Preparing installation",
    group = "UnifiedUpdateDialog",
    widthDp = 412,
    heightDp = 892,
    showBackground = true,
)
@Composable
private fun UnifiedUpdatePreparingInstallPreview() {
    UnifiedUpdateDialogPreview(
        state = UpdateDialogUiState(
            phase = UpdateDialogPhase.PREPARING_INSTALL,
            versionName = "0.3.0-alpha.1",
        ),
    )
}

@Preview(
    name = "Permission required",
    group = "UnifiedUpdateDialog",
    widthDp = 412,
    heightDp = 892,
    showBackground = true,
)
@Composable
private fun UnifiedUpdatePermissionRequiredPreview() {
    UnifiedUpdateDialogPreview(
        state = UpdateDialogUiState(
            phase = UpdateDialogPhase.PERMISSION_REQUIRED,
            versionName = "0.3.0-alpha.1",
        ),
    )
}

@Preview(
    name = "Permission required · narrow phone",
    group = "UnifiedUpdateDialog",
    widthDp = 320,
    heightDp = 720,
    showBackground = true,
)
@Composable
private fun UnifiedUpdatePermissionRequiredNarrowPreview() {
    UnifiedUpdateDialogPreview(
        state = UpdateDialogUiState(
            phase = UpdateDialogPhase.PERMISSION_REQUIRED,
            versionName = "0.3.0-alpha.1",
        ),
    )
}

@Preview(
    name = "Permission required · large font",
    group = "UnifiedUpdateDialog",
    widthDp = 412,
    heightDp = 892,
    fontScale = 1.3f,
    showBackground = true,
)
@Composable
private fun UnifiedUpdatePermissionRequiredLargeFontPreview() {
    UnifiedUpdateDialogPreview(
        state = UpdateDialogUiState(
            phase = UpdateDialogPhase.PERMISSION_REQUIRED,
            versionName = "0.3.0-alpha.1",
        ),
    )
}

@Preview(
    name = "Installing update",
    group = "UnifiedUpdateDialog",
    widthDp = 412,
    heightDp = 892,
    showBackground = true,
)
@Composable
private fun UnifiedUpdateInstallingPreview() {
    UnifiedUpdateDialogPreview(
        state = UpdateDialogUiState(
            phase = UpdateDialogPhase.INSTALLING,
            versionName = "0.3.0-alpha.1",
        ),
    )
}

@Preview(
    name = "Installation failed",
    group = "UnifiedUpdateDialog",
    widthDp = 412,
    heightDp = 892,
    showBackground = true,
)
@Composable
private fun UnifiedUpdateInstallFailedPreview() {
    UnifiedUpdateDialogPreview(
        state = UpdateDialogUiState(
            phase = UpdateDialogPhase.INSTALL_FAILED,
            versionName = "0.3.0-alpha.1",
            installFailureReason =
                UpdateDialogInstallFailureUiReason.SIGNING_IDENTITY_MISMATCH,
        ),
    )
}

@Preview(
    name = "Installation failed · narrow phone",
    group = "UnifiedUpdateDialog",
    widthDp = 320,
    heightDp = 720,
    showBackground = true,
)
@Composable
private fun UnifiedUpdateInstallFailedNarrowPreview() {
    UnifiedUpdateDialogPreview(
        state = UpdateDialogUiState(
            phase = UpdateDialogPhase.INSTALL_FAILED,
            versionName = "0.3.0-alpha.1",
            installFailureReason =
                UpdateDialogInstallFailureUiReason.SIGNING_IDENTITY_MISMATCH,
        ),
    )
}

@Preview(
    name = "Installation failed · large font",
    group = "UnifiedUpdateDialog",
    widthDp = 412,
    heightDp = 892,
    fontScale = 1.3f,
    showBackground = true,
)
@Composable
private fun UnifiedUpdateInstallFailedLargeFontPreview() {
    UnifiedUpdateDialogPreview(
        state = UpdateDialogUiState(
            phase = UpdateDialogPhase.INSTALL_FAILED,
            versionName = "0.3.0-alpha.1",
            installFailureReason =
                UpdateDialogInstallFailureUiReason.SIGNING_IDENTITY_MISMATCH,
        ),
    )
}

@Composable
private fun UnifiedUpdateDialogPreview(
    state: UpdateDialogUiState,
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
            UnifiedUpdateDialogContent(
                state = state,
                onInstall = {},
                onDownloadUpdate = {},
                onRetryDownload = {},
                onRetryInstall = {},
                onGrantInstallPermission = {},
                onDownloadFromGitHub = {},
                onDismissRequest = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp),
            )
        }
    }
}
