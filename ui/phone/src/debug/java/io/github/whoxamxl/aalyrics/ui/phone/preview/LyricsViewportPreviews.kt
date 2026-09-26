package io.github.whoxamxl.aalyrics.ui.phone.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
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
import io.github.whoxamxl.aalyrics.ui.phone.lyrics.KaraokeLineUiState
import io.github.whoxamxl.aalyrics.ui.phone.lyrics.LyricsViewportUiState
import io.github.whoxamxl.aalyrics.core.model.LyricsSyncType
import kotlinx.coroutines.delay

@Preview(name = "WORD · karaoke progress", group = "LyricsViewport", widthDp = 412, heightDp = 520)
@Composable
private fun LyricsViewportWordPreview() {
    LyricsViewportPreview(PhonePreviewFixtures.viewportWord)
}

@Preview(name = "WORD · before first word", group = "LyricsViewport", widthDp = 412, heightDp = 520)
@Composable
private fun LyricsViewportWordBeforeFirstPreview() {
    LyricsViewportPreview(
        PhonePreviewFixtures.viewportWord.copy(
            currentWordIndex = null,
            currentWordProgress = 0f,
            karaokeLine = KaraokeLineUiState(completedEnd = 0),
        ),
    )
}

@Preview(name = "WORD · inter-word gap", group = "LyricsViewport", widthDp = 412, heightDp = 520)
@Composable
private fun LyricsViewportWordGapPreview() {
    LyricsViewportPreview(
        PhonePreviewFixtures.viewportWord.copy(
            currentWordIndex = null,
            currentWordProgress = 0f,
            karaokeLine = KaraokeLineUiState(completedEnd = 12),
        ),
    )
}

@Preview(name = "WORD · Karaoke off", group = "LyricsViewport", widthDp = 412, heightDp = 520)
@Composable
private fun LyricsViewportWordOffPreview() {
    LyricsViewportPreview(PhonePreviewFixtures.viewportWord.copy(
        currentWordIndex = null,
        currentWordProgress = 0f,
        karaokeLine = null,
    ))
}

@Preview(name = "WORD · Karaoke with Translation", group = "LyricsViewport", widthDp = 412, heightDp = 520)
@Composable
private fun LyricsViewportWordTranslatedPreview() {
    val lines = PhonePreviewFixtures.viewportWord.lines.toMutableList()
    lines[4] = lines[4].copy(translatedText = "A quiet echo follows close behind")
    LyricsViewportPreview(PhonePreviewFixtures.viewportWord.copy(lines = lines))
}

@Preview(name = "LINE · middle", group = "LyricsViewport", widthDp = 412, heightDp = 520)
@Composable
private fun LyricsViewportLinePreview() {
    LyricsViewportPreview(PhonePreviewFixtures.viewportLineMiddle)
}

@Preview(name = "LINE · translated mixed rows", group = "LyricsViewport", widthDp = 412, heightDp = 520)
@Composable
private fun LyricsViewportTranslatedLinePreview() {
    LyricsViewportPreview(PhonePreviewFixtures.viewportTranslatedLine)
}

@Preview(name = "PLAIN · translated", group = "LyricsViewport", widthDp = 412, heightDp = 520)
@Composable
private fun LyricsViewportTranslatedPlainPreview() {
    LyricsViewportPreview(PhonePreviewFixtures.viewportTranslatedPlain)
}

@Preview(name = "LINE · translated narrow", group = "LyricsViewport", widthDp = 320, heightDp = 520)
@Composable
private fun LyricsViewportTranslatedNarrowPreview() {
    LyricsViewportPreview(PhonePreviewFixtures.viewportTranslatedLine.copy(currentLineIndex = 6))
}

@Preview(name = "LINE · translated large font", group = "LyricsViewport", widthDp = 360, heightDp = 520, fontScale = 1.4f)
@Composable
private fun LyricsViewportTranslatedLargeFontPreview() {
    LyricsViewportPreview(PhonePreviewFixtures.viewportTranslatedLine.copy(currentLineIndex = 6))
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
        val listState = rememberLazyListState()

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
                listState = listState,
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
        val listState = rememberLazyListState()

        if (startAtEnd) {
            LaunchedEffect(state.lines.size, state.syncType) {
                if (state.lines.isNotEmpty()) {
                    val openingItemOffset =
                        if (state.syncType == LyricsSyncType.PLAIN) 0 else 1
                    listState.scrollToItem(
                        state.lines.lastIndex + openingItemOffset,
                    )
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
                listState = listState,
                onInteractionModeChange = { mode ->
                    state = state.copy(interactionMode = mode)
                },
            )
        }
    }
}
