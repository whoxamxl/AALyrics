package io.github.whoxamxl.aalyrics.ui.phone.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsColors
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsTheme
import io.github.whoxamxl.aalyrics.ui.phone.settings.ChangelogSettingsScreen

@Preview(name = "Typical", group = "ChangelogSettings", widthDp = 412, heightDp = 760)
@Composable
private fun ChangelogSettingsTypicalPreview() {
    ChangelogSettingsPreview()
}

@Preview(name = "Narrow · 320dp", group = "ChangelogSettings", widthDp = 320, heightDp = 700)
@Composable
private fun ChangelogSettingsNarrowPreview() {
    ChangelogSettingsPreview()
}

@Preview(
    name = "Enlarged font",
    group = "ChangelogSettings",
    widthDp = 412,
    heightDp = 820,
    fontScale = 1.4f,
)
@Composable
private fun ChangelogSettingsLargeFontPreview() {
    ChangelogSettingsPreview()
}

@Composable
private fun ChangelogSettingsPreview() {
    AALyricsTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AALyricsColors.BackgroundBase),
        ) {
            ChangelogSettingsScreen(
                changelogText = PhonePreviewFixtures.changelogMarkdownSample,
                onBack = {},
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
