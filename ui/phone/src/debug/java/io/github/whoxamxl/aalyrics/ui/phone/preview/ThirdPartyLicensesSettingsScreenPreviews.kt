package io.github.whoxamxl.aalyrics.ui.phone.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsColors
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsTheme
import io.github.whoxamxl.aalyrics.ui.phone.settings.ThirdPartyLicensesSettingsScreen

@Preview(name = "Typical", group = "ThirdPartyLicensesSettings", widthDp = 412, heightDp = 760)
@Composable
private fun ThirdPartyLicensesSettingsTypicalPreview() {
    ThirdPartyLicensesSettingsPreview()
}

@Preview(name = "Narrow · 320dp", group = "ThirdPartyLicensesSettings", widthDp = 320, heightDp = 700)
@Composable
private fun ThirdPartyLicensesSettingsNarrowPreview() {
    ThirdPartyLicensesSettingsPreview()
}

@Preview(
    name = "Enlarged font",
    group = "ThirdPartyLicensesSettings",
    widthDp = 412,
    heightDp = 820,
    fontScale = 1.4f,
)
@Composable
private fun ThirdPartyLicensesSettingsLargeFontPreview() {
    ThirdPartyLicensesSettingsPreview()
}

@Composable
private fun ThirdPartyLicensesSettingsPreview() {
    AALyricsTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AALyricsColors.BackgroundBase),
        ) {
            ThirdPartyLicensesSettingsScreen(
                thirdPartyLicensesText = PhonePreviewFixtures.thirdPartyLicensesMarkdownSample,
                onBack = {},
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
