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
import io.github.whoxamxl.aalyrics.ui.phone.update.UpdateSuccessfulDialogContent
import io.github.whoxamxl.aalyrics.ui.phone.update.UpdateSuccessfulDialogUiState

@Preview(
    name = "Typical phone",
    group = "UpdateSuccessfulDialog",
    widthDp = 412,
    heightDp = 892,
    showBackground = true,
)
@Composable
private fun UpdateSuccessfulDialogTypicalPreview() {
    UpdateSuccessfulDialogPreview()
}

@Preview(
    name = "Narrow phone",
    group = "UpdateSuccessfulDialog",
    widthDp = 320,
    heightDp = 720,
    showBackground = true,
)
@Composable
private fun UpdateSuccessfulDialogNarrowPreview() {
    UpdateSuccessfulDialogPreview()
}

@Preview(
    name = "Large font",
    group = "UpdateSuccessfulDialog",
    widthDp = 412,
    heightDp = 892,
    fontScale = 1.3f,
    showBackground = true,
)
@Composable
private fun UpdateSuccessfulDialogLargeFontPreview() {
    UpdateSuccessfulDialogPreview()
}

@Composable
private fun UpdateSuccessfulDialogPreview() {
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
            UpdateSuccessfulDialogContent(
                state = UpdateSuccessfulDialogUiState(
                    versionName = "0.3.0-alpha.1",
                ),
                onDismissRequest = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp),
            )
        }
    }
}
