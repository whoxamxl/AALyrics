package io.github.whoxamxl.aalyrics.ui.phone.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsTheme
import io.github.whoxamxl.aalyrics.ui.phone.setup.NotificationAccessSetupScreen

@Preview(
    name = "Typical phone",
    group = "NotificationAccessSetup",
    widthDp = 412,
    heightDp = 892,
    showBackground = true,
)
@Composable
private fun NotificationAccessSetupTypicalPreview() {
    NotificationAccessSetupPreview()
}

@Preview(
    name = "Narrow phone",
    group = "NotificationAccessSetup",
    widthDp = 320,
    heightDp = 720,
    showBackground = true,
)
@Composable
private fun NotificationAccessSetupNarrowPreview() {
    NotificationAccessSetupPreview()
}

@Preview(
    name = "Large font",
    group = "NotificationAccessSetup",
    widthDp = 412,
    heightDp = 892,
    fontScale = 1.3f,
    showBackground = true,
)
@Composable
private fun NotificationAccessSetupLargeFontPreview() {
    NotificationAccessSetupPreview()
}

@Composable
private fun NotificationAccessSetupPreview() {
    AALyricsTheme {
        NotificationAccessSetupScreen(onGrantAccess = {})
    }
}
