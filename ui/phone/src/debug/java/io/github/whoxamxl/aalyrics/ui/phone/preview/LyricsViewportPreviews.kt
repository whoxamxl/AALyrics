package io.github.whoxamxl.aalyrics.ui.phone.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsColors
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsTheme
import io.github.whoxamxl.aalyrics.ui.phone.lyrics.LyricsViewport
import io.github.whoxamxl.aalyrics.ui.phone.lyrics.LyricsViewportUiState

@Preview(name = "WORD · karaoke progress", group = "LyricsViewport", widthDp = 412, heightDp = 520)
@Composable
private fun LyricsViewportWordPreview() {
    LyricsViewportPreview(PhonePreviewFixtures.viewportWord)
}

@Preview(name = "LINE · middle", group = "LyricsViewport", widthDp = 412, heightDp = 520)
@Composable
private fun LyricsViewportLinePreview() {
    LyricsViewportPreview(PhonePreviewFixtures.viewportLineMiddle)
}

@Preview(name = "PLAIN · estimated follow", group = "LyricsViewport", widthDp = 412, heightDp = 520)
@Composable
private fun LyricsViewportPlainPreview() {
    LyricsViewportPreview(PhonePreviewFixtures.viewportPlain)
}

@Preview(name = "LINE · first row", group = "LyricsViewport", widthDp = 412, heightDp = 520)
@Composable
private fun LyricsViewportFirstPreview() {
    LyricsViewportPreview(PhonePreviewFixtures.viewportLineFirst)
}

@Preview(name = "LINE · last row", group = "LyricsViewport", widthDp = 412, heightDp = 520)
@Composable
private fun LyricsViewportLastPreview() {
    LyricsViewportPreview(PhonePreviewFixtures.viewportLineLast)
}

@Preview(name = "Browse · playback below", group = "LyricsViewport", widthDp = 412, heightDp = 520)
@Composable
private fun LyricsViewportBrowseBelowPreview() {
    LyricsViewportPreview(PhonePreviewFixtures.viewportBrowsePlaybackBelow)
}

@Preview(name = "Browse · playback above", group = "LyricsViewport", widthDp = 412, heightDp = 520)
@Composable
private fun LyricsViewportBrowseAbovePreview() {
    LyricsViewportPreview(
        initialState = PhonePreviewFixtures.viewportBrowsePlaybackAbove,
        startAtEnd = true,
    )
}

@Preview(name = "PLAIN · duration unavailable", group = "LyricsViewport", widthDp = 412, heightDp = 520)
@Composable
private fun LyricsViewportPlainNoDurationPreview() {
    LyricsViewportPreview(PhonePreviewFixtures.viewportPlainNoDuration)
}

@Composable
private fun LyricsViewportPreview(
    initialState: LyricsViewportUiState,
    startAtEnd: Boolean = false,
) {
    AALyricsTheme {
        var state by remember(initialState) { mutableStateOf(initialState) }
        val scrollState = rememberScrollState()

        if (startAtEnd) {
            LaunchedEffect(scrollState.maxValue) {
                if (scrollState.maxValue > 0) {
                    scrollState.scrollTo(scrollState.maxValue)
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AALyricsColors.BackgroundBase),
        ) {
            LyricsViewport(
                state = state,
                modifier = Modifier.fillMaxSize(),
                scrollState = scrollState,
                onInteractionModeChange = { mode ->
                    state = state.copy(interactionMode = mode)
                },
            )
        }
    }
}
