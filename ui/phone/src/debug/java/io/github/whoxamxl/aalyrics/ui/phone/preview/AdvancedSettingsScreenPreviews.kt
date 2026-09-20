package io.github.whoxamxl.aalyrics.ui.phone.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsColors
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsTheme
import io.github.whoxamxl.aalyrics.ui.phone.settings.AdvancedSettingsScreen

@Preview(name = "Verbose off", group = "AdvancedSettings", widthDp = 412, heightDp = 760)
@Composable
private fun AdvancedVerboseOffPreview() {
    AdvancedSettingsPreview(initialVerboseDetailsEnabled = false)
}

@Preview(name = "Verbose on", group = "AdvancedSettings", widthDp = 412, heightDp = 760)
@Composable
private fun AdvancedVerboseOnPreview() {
    AdvancedSettingsPreview(initialVerboseDetailsEnabled = true)
}

@Preview(
    name = "Narrow · 320dp",
    group = "AdvancedSettings",
    widthDp = 320,
    heightDp = 700,
)
@Composable
private fun AdvancedNarrowPreview() {
    AdvancedSettingsPreview(initialVerboseDetailsEnabled = false)
}

@Composable
private fun AdvancedSettingsPreview(initialVerboseDetailsEnabled: Boolean) {
    AALyricsTheme {
        var verboseDetailsEnabled by remember(initialVerboseDetailsEnabled) {
            mutableStateOf(initialVerboseDetailsEnabled)
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AALyricsColors.BackgroundBase),
        ) {
            AdvancedSettingsScreen(
                verboseDetailsEnabled = verboseDetailsEnabled,
                onVerboseDetailsChanged = { verboseDetailsEnabled = it },
                onBack = {},
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
