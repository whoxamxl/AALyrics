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
import kotlinx.coroutines.delay

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

@Preview(
    name = "LINE · transition demo (Interactive)",
    group = "LyricsViewport",
    widthDp = 412,
    heightDp = 520,
)
@Composable
private fun LyricsViewportLineTransitionPreview() {
    AALyricsTheme {
        var state by remember {
            mutableStateOf(
                PhonePreviewFixtures.viewportLineFirst.copy(currentLineIndex = null),
            )
        }
        val scrollState = rememberScrollState()

        LaunchedEffect(Unit) {
            delay(1200)
            for (index in state.lines.indices) {
                state = state.copy(currentLineIndex = index)
                delay(2500)
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

@Preview(name = "PLAIN · estimated follow", group = "LyricsViewport", widthDp = 412, heightDp = 520)
@Composable
private fun LyricsViewportPlainPreview() {
    LyricsViewportPreview(PhonePreviewFixtures.viewportPlain)
}

@Preview(name = "LINE · intro focus row", group = "LyricsViewport", widthDp = 412, heightDp = 520)
@Composable
private fun LyricsViewportIntroFocusPreview() {
    LyricsViewportPreview(
        PhonePreviewFixtures.viewportLineFirst.copy(currentLineIndex = null),
    )
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

@Preview(name = "LINE · long wrapped row", group = "LyricsViewport", widthDp = 412, heightDp = 520)
@Composable
private fun LyricsViewportLongLinePreview() {
    LyricsViewportPreview(
        PhonePreviewFixtures.viewportLineMiddle.copy(currentLineIndex = 6),
    )
}

@Preview(name = "PLAIN · auto-scroll off", group = "LyricsViewport", widthDp = 412, heightDp = 520)
@Composable
private fun LyricsViewportPlainAutoScrollOffPreview() {
    LyricsViewportPreview(
        PhonePreviewFixtures.viewportPlain.copy(plainAutoScrollEnabled = false),
    )
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
