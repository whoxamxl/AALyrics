package io.github.whoxamxl.aalyrics.ui.phone.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsColors
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsTheme
import io.github.whoxamxl.aalyrics.ui.phone.lyrics.LyricsViewport
import io.github.whoxamxl.aalyrics.ui.phone.lyrics.TrackCard
import io.github.whoxamxl.aalyrics.ui.phone.navigation.PhoneDestination
import io.github.whoxamxl.aalyrics.ui.phone.shell.PhoneAppShell
import io.github.whoxamxl.aalyrics.ui.phone.shell.PhoneNavigationBar
import io.github.whoxamxl.aalyrics.ui.phone.shell.PhoneTopBar
import io.github.whoxamxl.aalyrics.ui.phone.shell.PlaybackBar

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

@Preview(name = "Track Card · 320dp", group = "Responsive", widthDp = 320, showBackground = true)
@Composable
private fun TrackCardNarrowPreview() {
    AALyricsTheme {
        TrackCard(state = PhonePreviewFixtures.trackCardLongTitleAndArtist)
    }
}

@Preview(
    name = "Lyrics Viewport · 320dp",
    group = "Responsive",
    widthDp = 320,
    heightDp = 500,
    showBackground = true,
)
@Composable
private fun LyricsViewportNarrowPreview() {
    AALyricsTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AALyricsColors.BackgroundBase),
        ) {
            LyricsViewport(state = PhonePreviewFixtures.viewportLineMiddle)
        }
    }
}

@Preview(
    name = "Lyrics Viewport · short",
    group = "Responsive",
    widthDp = 412,
    heightDp = 320,
    showBackground = true,
)
@Composable
private fun LyricsViewportShortPreview() {
    AALyricsTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AALyricsColors.BackgroundBase),
        ) {
            LyricsViewport(state = PhonePreviewFixtures.viewportWord)
        }
    }
}

@Preview(
    name = "Lyrics Viewport · tall",
    group = "Responsive",
    widthDp = 412,
    heightDp = 720,
    showBackground = true,
)
@Composable
private fun LyricsViewportTallPreview() {
    AALyricsTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AALyricsColors.BackgroundBase),
        ) {
            LyricsViewport(state = PhonePreviewFixtures.viewportPlain)
        }
    }
}

@Preview(name = "Playback · 320dp", group = "Responsive", widthDp = 320, showBackground = true)
@Composable
private fun PlaybackControlsNarrowPreview() {
    AALyricsTheme {
        PlaybackBar(
            state = PhonePreviewFixtures.longMetadataSurface,
            progressFraction = PhonePreviewFixtures.longMetadataSurface.progressFraction,
            onExpand = {},
            onPlayPause = {},
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
            onDestinationReselected = {},
            onPrevious = {},
            onPlayPause = {},
            onNext = {},
            onSeekTo = {},
            onQueueItemSelected = {},
            onOpenPlaybackApp = {},
            onTranslationEnabledChanged = {},
        ) { _, bottomOverlayInset ->
            PreviewLyricsDestination(
                initialState = PhonePreviewFixtures.lyricsScreenLine,
                bottomOverlayInset = bottomOverlayInset,
            )
        }
    }
}
