package io.github.whoxamxl.aalyrics.ui.phone.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsColors
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsTheme
import io.github.whoxamxl.aalyrics.ui.phone.settings.LicenseSettingsScreen

@Preview(name = "Typical", group = "LicenseSettings", widthDp = 412, heightDp = 760)
@Composable
private fun LicenseSettingsTypicalPreview() {
    LicenseSettingsPreview()
}

@Preview(name = "Narrow · 320dp", group = "LicenseSettings", widthDp = 320, heightDp = 700)
@Composable
private fun LicenseSettingsNarrowPreview() {
    LicenseSettingsPreview()
}

@Preview(
    name = "Enlarged font",
    group = "LicenseSettings",
    widthDp = 412,
    heightDp = 820,
    fontScale = 1.4f,
)
@Composable
private fun LicenseSettingsLargeFontPreview() {
    LicenseSettingsPreview()
}

@Composable
private fun LicenseSettingsPreview() {
    AALyricsTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AALyricsColors.BackgroundBase),
        ) {
            LicenseSettingsScreen(
                noticeText = PhonePreviewFixtures.noticeSample,
                licenseText = PhonePreviewFixtures.licenseMarkdownSample,
                onBack = {},
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
