package io.github.whoxamxl.aalyrics.ui.phone.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsColors
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsTheme
import io.github.whoxamxl.aalyrics.ui.phone.settings.TermsOfUseSettingsScreen

@Preview(name = "Typical", group = "TermsOfUseSettings", widthDp = 412, heightDp = 760)
@Composable
private fun TermsOfUseSettingsTypicalPreview() {
    TermsOfUseSettingsPreview()
}

@Preview(name = "Narrow · 320dp", group = "TermsOfUseSettings", widthDp = 320, heightDp = 700)
@Composable
private fun TermsOfUseSettingsNarrowPreview() {
    TermsOfUseSettingsPreview()
}

@Preview(
    name = "Enlarged font",
    group = "TermsOfUseSettings",
    widthDp = 412,
    heightDp = 820,
    fontScale = 1.4f,
)
@Composable
private fun TermsOfUseSettingsLargeFontPreview() {
    TermsOfUseSettingsPreview()
}

@Composable
private fun TermsOfUseSettingsPreview() {
    AALyricsTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AALyricsColors.BackgroundBase),
        ) {
            TermsOfUseSettingsScreen(
                termsOfUseText = PhonePreviewFixtures.termsOfUseMarkdownSample,
                onBack = {},
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
