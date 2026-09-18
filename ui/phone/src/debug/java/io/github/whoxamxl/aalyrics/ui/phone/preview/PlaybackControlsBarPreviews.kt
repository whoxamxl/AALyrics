package io.github.whoxamxl.aalyrics.ui.phone.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsColors
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsTheme
import io.github.whoxamxl.aalyrics.ui.phone.shell.PlaybackControlsBar
import io.github.whoxamxl.aalyrics.ui.phone.state.PlaybackControlsUiState

@Preview(name = "Playing", group = "PlaybackControlsBar", widthDp = 412, showBackground = true)
@Composable
private fun PlaybackControlsPlayingPreview() {
    PlaybackControlsPreview(PhonePreviewFixtures.playingControls)
}

@Preview(name = "Paused", group = "PlaybackControlsBar", widthDp = 412, showBackground = true)
@Composable
private fun PlaybackControlsPausedPreview() {
    PlaybackControlsPreview(PhonePreviewFixtures.pausedControls)
}

@Preview(name = "Previous disabled", group = "PlaybackControlsBar", widthDp = 412, showBackground = true)
@Composable
private fun PlaybackControlsPreviousDisabledPreview() {
    PlaybackControlsPreview(PhonePreviewFixtures.previousDisabledControls)
}

@Preview(name = "Next disabled", group = "PlaybackControlsBar", widthDp = 412, showBackground = true)
@Composable
private fun PlaybackControlsNextDisabledPreview() {
    PlaybackControlsPreview(PhonePreviewFixtures.nextDisabledControls)
}

@Preview(name = "All disabled", group = "PlaybackControlsBar", widthDp = 412, showBackground = true)
@Composable
private fun PlaybackControlsAllDisabledPreview() {
    PlaybackControlsPreview(PhonePreviewFixtures.allDisabledControls)
}

@Composable
private fun PlaybackControlsPreview(state: PlaybackControlsUiState) {
    AALyricsTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(AALyricsColors.BackgroundBase),
        ) {
            PlaybackControlsBar(
                state = state,
                onPrevious = {},
                onPlayPause = {},
                onNext = {},
            )
        }
    }
}
