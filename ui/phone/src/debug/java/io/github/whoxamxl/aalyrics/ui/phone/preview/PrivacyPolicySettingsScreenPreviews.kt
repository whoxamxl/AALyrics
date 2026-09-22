package io.github.whoxamxl.aalyrics.ui.phone.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsColors
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsTheme
import io.github.whoxamxl.aalyrics.ui.phone.settings.PrivacyPolicySettingsScreen

@Preview(name = "Typical", group = "PrivacyPolicySettings", widthDp = 412, heightDp = 760)
@Composable
private fun PrivacyPolicySettingsTypicalPreview() {
    PrivacyPolicySettingsPreview()
}

@Preview(name = "Narrow · 320dp", group = "PrivacyPolicySettings", widthDp = 320, heightDp = 700)
@Composable
private fun PrivacyPolicySettingsNarrowPreview() {
    PrivacyPolicySettingsPreview()
}

@Preview(
    name = "Enlarged font",
    group = "PrivacyPolicySettings",
    widthDp = 412,
    heightDp = 820,
    fontScale = 1.4f,
)
@Composable
private fun PrivacyPolicySettingsLargeFontPreview() {
    PrivacyPolicySettingsPreview()
}

@Composable
private fun PrivacyPolicySettingsPreview() {
    AALyricsTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AALyricsColors.BackgroundBase),
        ) {
            PrivacyPolicySettingsScreen(
                privacyPolicyText = PhonePreviewFixtures.privacyPolicyMarkdownSample,
                onBack = {},
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
