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
import io.github.whoxamxl.aalyrics.ui.phone.update.UpdateDialogPhase
import io.github.whoxamxl.aalyrics.ui.phone.update.UpdateDialogUiState

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
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp),
            )
        }
    }
}
