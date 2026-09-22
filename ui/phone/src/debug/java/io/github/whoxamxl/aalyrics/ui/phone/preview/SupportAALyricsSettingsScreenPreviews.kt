package io.github.whoxamxl.aalyrics.ui.phone.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsColors
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsTheme
import io.github.whoxamxl.aalyrics.ui.phone.settings.SupportAALyricsSettingsScreen

@Preview(name = "Typical", group = "SupportAALyricsSettings", widthDp = 412, heightDp = 760)
@Composable
private fun SupportAALyricsSettingsTypicalPreview() {
    SupportAALyricsSettingsPreview()
}

@Preview(
    name = "Interactive animation",
    group = "SupportAALyricsSettings",
    widthDp = 412,
    heightDp = 760,
)
@Composable
private fun SupportAALyricsSettingsInteractivePreview() {
    SupportAALyricsSettingsPreview()
}

@Preview(name = "Narrow · 320dp", group = "SupportAALyricsSettings", widthDp = 320, heightDp = 700)
@Composable
private fun SupportAALyricsSettingsNarrowPreview() {
    SupportAALyricsSettingsPreview()
}

@Preview(
    name = "Enlarged font",
    group = "SupportAALyricsSettings",
    widthDp = 412,
    heightDp = 820,
    fontScale = 1.4f,
)
@Composable
private fun SupportAALyricsSettingsLargeFontPreview() {
    SupportAALyricsSettingsPreview()
}

@Composable
private fun SupportAALyricsSettingsPreview() {
    AALyricsTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AALyricsColors.BackgroundBase),
        ) {
            SupportAALyricsSettingsScreen(
                onSupport = {},
                onBack = {},
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
