package io.github.whoxamxl.aalyrics.ui.phone.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsColors
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
    name = "Lyrics selected · Gesture navigation context",
    group = "PhoneNavigationBar",
    widthDp = 412,
    heightDp = 86,
    showBackground = true,
)
@Composable
private fun PhoneNavigationGestureContextPreview() {
    AALyricsTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(AALyricsColors.BackgroundChrome),
        ) {
            PhoneNavigationBar(
                selectedDestination = PhoneDestination.Lyrics,
                onDestinationSelected = {},
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp),
                contentAlignment = Alignment.BottomCenter,
            ) {
                Box(
                    modifier = Modifier
                        .width(96.dp)
                        .height(4.dp)
                        .background(
                            color = AALyricsColors.TextTertiary,
                            shape = RoundedCornerShape(2.dp),
                        ),
                )
            }
        }
    }
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
