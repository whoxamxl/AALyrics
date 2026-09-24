package io.github.whoxamxl.aalyrics.ui.phone.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsColors
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsSpacing
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsTheme
import io.github.whoxamxl.aalyrics.ui.phone.component.VersionChip

@Preview(
    name = "Channel matrix",
    group = "VersionChip",
    widthDp = 412,
    heightDp = 360,
    showBackground = true,
)
@Composable
private fun VersionChipChannelMatrixPreview() {
    VersionChipPreviewContent()
}

@Preview(
    name = "Narrow phone",
    group = "VersionChip",
    widthDp = 320,
    heightDp = 360,
    showBackground = true,
)
@Composable
private fun VersionChipNarrowPreview() {
    VersionChipPreviewContent()
}

@Preview(
    name = "Large font",
    group = "VersionChip",
    widthDp = 412,
    heightDp = 420,
    fontScale = 1.3f,
    showBackground = true,
)
@Composable
private fun VersionChipLargeFontPreview() {
    VersionChipPreviewContent()
}

@Composable
private fun VersionChipPreviewContent() {
    AALyricsTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AALyricsColors.BackgroundBase)
                .padding(AALyricsSpacing.Space24),
            verticalArrangement = Arrangement.spacedBy(AALyricsSpacing.Space12),
        ) {
            VersionChip(versionName = "0.2.0-alpha.1-dev+abcdef0")
            VersionChip(versionName = "v0.2.0-alpha.1")
            VersionChip(versionName = "0.2.0-beta.1")
            VersionChip(versionName = "0.2.0-rc.1")
            VersionChip(versionName = "0.2.0")
        }
    }
}
