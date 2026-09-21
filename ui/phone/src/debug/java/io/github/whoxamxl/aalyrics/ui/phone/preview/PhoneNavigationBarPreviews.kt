package io.github.whoxamxl.aalyrics.ui.phone.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsTheme
import io.github.whoxamxl.aalyrics.ui.phone.navigation.PhoneDestination
import io.github.whoxamxl.aalyrics.ui.phone.shell.PhoneNavigationBar

@Preview(name = "Lyrics selected", group = "PhoneNavigationBar", widthDp = 412, showBackground = true)
@Composable
private fun PhoneNavigationLyricsPreview() {
    PhoneNavigationPreview(PhoneDestination.Lyrics)
}

@Preview(name = "Sync selected", group = "PhoneNavigationBar", widthDp = 412, showBackground = true)
@Composable
private fun PhoneNavigationSyncPreview() {
    PhoneNavigationPreview(PhoneDestination.Sync)
}

@Preview(name = "Details selected", group = "PhoneNavigationBar", widthDp = 412, showBackground = true)
@Composable
private fun PhoneNavigationDetailsPreview() {
    PhoneNavigationPreview(PhoneDestination.Details)
}

@Preview(name = "Settings selected", group = "PhoneNavigationBar", widthDp = 412, showBackground = true)
@Composable
private fun PhoneNavigationSettingsPreview() {
    PhoneNavigationPreview(PhoneDestination.Settings)
}



@Preview(
    name = "Lyrics selected · System navigation inset",
    group = "PhoneNavigationBar",
    widthDp = 412,
    heightDp = 160,
    showBackground = true,
    showSystemUi = true,
)
@Composable
private fun PhoneNavigationSystemInsetPreview() {
    PhoneNavigationPreview(PhoneDestination.Lyrics)
}


@Composable
private fun PhoneNavigationPreview(selectedDestination: PhoneDestination) {
    AALyricsTheme {
        PhoneNavigationBar(
            selectedDestination = selectedDestination,
            onDestinationSelected = {},
        )
    }
}
