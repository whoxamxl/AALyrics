package io.github.whoxamxl.aalyrics.ui.phone.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsColors
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsSpacing
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsTheme
import io.github.whoxamxl.aalyrics.ui.phone.settings.InstallPermissionDialogContent
import io.github.whoxamxl.aalyrics.ui.phone.settings.InstallPermissionDialogUiState

@Preview(
    name = "Typical phone",
    group = "InstallPermissionDialog",
    widthDp = 412,
    heightDp = 892,
    showBackground = true,
)
@Composable
private fun InstallPermissionDialogTypicalPreview() {
    InstallPermissionDialogPreview()
}

@Preview(
    name = "Narrow phone",
    group = "InstallPermissionDialog",
    widthDp = 320,
    heightDp = 720,
    showBackground = true,
)
@Composable
private fun InstallPermissionDialogNarrowPreview() {
    InstallPermissionDialogPreview()
}

@Preview(
    name = "Large font",
    group = "InstallPermissionDialog",
    widthDp = 412,
    heightDp = 892,
    fontScale = 1.3f,
    showBackground = true,
)
@Composable
private fun InstallPermissionDialogLargeFontPreview() {
    InstallPermissionDialogPreview()
}

@Composable
private fun InstallPermissionDialogPreview() {
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
            InstallPermissionDialogContent(
                state = InstallPermissionDialogUiState(
                    versionName = "0.3.0-alpha.1",
                ),
                onDismissRequest = {},
                onGrantPermission = {},
                onDownloadFromGitHub = {},
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
