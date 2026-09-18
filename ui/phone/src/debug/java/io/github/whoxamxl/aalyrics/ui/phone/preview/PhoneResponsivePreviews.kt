package io.github.whoxamxl.aalyrics.ui.phone.preview

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsTheme
import io.github.whoxamxl.aalyrics.ui.phone.navigation.PhoneDestination
import io.github.whoxamxl.aalyrics.ui.phone.shell.PhoneAppShell
import io.github.whoxamxl.aalyrics.ui.phone.shell.PhoneNavigationBar
import io.github.whoxamxl.aalyrics.ui.phone.shell.PhoneTopBar
import io.github.whoxamxl.aalyrics.ui.phone.shell.PlaybackControlsBar

/**
 * Responsive-only checks kept separate from the primary component Preview files.
 * Open this file when narrow-width validation is needed.
 */
@Preview(name = "Top bar · 320dp", group = "Responsive", widthDp = 320, showBackground = true)
@Composable
private fun PhoneTopBarNarrowPreview() {
    AALyricsTheme {
        PhoneTopBar(mediaSourceLabel = "YouTube Music")
    }
}

@Preview(name = "Playback · 320dp", group = "Responsive", widthDp = 320, showBackground = true)
@Composable
private fun PlaybackControlsNarrowPreview() {
    AALyricsTheme {
        PlaybackControlsBar(
            state = PhonePreviewFixtures.playingControls,
            onPrevious = {},
            onPlayPause = {},
            onNext = {},
        )
    }
}

@Preview(name = "Navigation · 320dp", group = "Responsive", widthDp = 320, showBackground = true)
@Composable
private fun PhoneNavigationNarrowPreview() {
    AALyricsTheme {
        PhoneNavigationBar(
            selectedDestination = PhoneDestination.Details,
            onDestinationSelected = {},
        )
    }
}

@Preview(
    name = "Shell · 320dp",
    group = "Responsive",
    widthDp = 320,
    heightDp = 700,
    showBackground = true,
)
@Composable
private fun PhoneAppShellNarrowPreview() {
    AALyricsTheme {
        PhoneAppShell(
            state = PhonePreviewFixtures.narrowLyricsShell,
            onDestinationSelected = {},
            onPrevious = {},
            onPlayPause = {},
            onNext = {},
        ) {
            Box(Modifier.fillMaxSize())
        }
    }
}
