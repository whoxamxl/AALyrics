package io.github.whoxamxl.aalyrics.ui.phone.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsTheme
import io.github.whoxamxl.aalyrics.ui.phone.setup.AndroidAutoCompatibilitySetupScreen

@Preview(
    name = "Typical phone",
    group = "AndroidAutoCompatibilitySetup",
    widthDp = 412,
    heightDp = 892,
    showBackground = true,
)
@Composable
private fun AndroidAutoCompatibilitySetupTypicalPreview() {
    AndroidAutoCompatibilitySetupPreview()
}

@Preview(
    name = "Narrow phone",
    group = "AndroidAutoCompatibilitySetup",
    widthDp = 320,
    heightDp = 720,
    showBackground = true,
)
@Composable
private fun AndroidAutoCompatibilitySetupNarrowPreview() {
    AndroidAutoCompatibilitySetupPreview()
}

@Preview(
    name = "Large font",
    group = "AndroidAutoCompatibilitySetup",
    widthDp = 412,
    heightDp = 892,
    fontScale = 1.3f,
    showBackground = true,
)
@Composable
private fun AndroidAutoCompatibilitySetupLargeFontPreview() {
    AndroidAutoCompatibilitySetupPreview()
}

@Composable
private fun AndroidAutoCompatibilitySetupPreview() {
    AALyricsTheme {
        AndroidAutoCompatibilitySetupScreen(
            onEnabled = {},
            onContinueWithout = {},
        )
    }
}
