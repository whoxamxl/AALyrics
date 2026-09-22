package io.github.whoxamxl.aalyrics.ui.phone.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsColors
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsTheme
import io.github.whoxamxl.aalyrics.ui.phone.settings.HelpFeedbackSettingsScreen

@Preview(name = "Typical", group = "HelpFeedbackSettings", widthDp = 412, heightDp = 760)
@Composable
private fun HelpFeedbackSettingsTypicalPreview() {
    HelpFeedbackSettingsPreview()
}

@Preview(name = "Narrow · 320dp", group = "HelpFeedbackSettings", widthDp = 320, heightDp = 700)
@Composable
private fun HelpFeedbackSettingsNarrowPreview() {
    HelpFeedbackSettingsPreview()
}

@Preview(
    name = "Enlarged font",
    group = "HelpFeedbackSettings",
    widthDp = 412,
    heightDp = 820,
    fontScale = 1.4f,
)
@Composable
private fun HelpFeedbackSettingsLargeFontPreview() {
    HelpFeedbackSettingsPreview()
}

@Composable
private fun HelpFeedbackSettingsPreview() {
    AALyricsTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AALyricsColors.BackgroundBase),
        ) {
            HelpFeedbackSettingsScreen(
                onDestinationSelected = {},
                onBack = {},
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
