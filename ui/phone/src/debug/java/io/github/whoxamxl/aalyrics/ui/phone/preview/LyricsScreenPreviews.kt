package io.github.whoxamxl.aalyrics.ui.phone.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsColors
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsTheme
import io.github.whoxamxl.aalyrics.ui.phone.lyrics.LyricsScreen
import io.github.whoxamxl.aalyrics.ui.phone.lyrics.LyricsScreenUiState

@Preview(name = "LINE", group = "LyricsScreen", widthDp = 412, heightDp = 650)
@Composable
private fun LyricsScreenLinePreview() {
    LyricsScreenPreview(PhonePreviewFixtures.lyricsScreenLine)
}

@Preview(name = "LINE · translated", group = "LyricsScreen", widthDp = 412, heightDp = 650)
@Composable
private fun LyricsScreenTranslatedLinePreview() {
    LyricsScreenPreview(PhonePreviewFixtures.lyricsScreenTranslatedLine)
}

@Preview(name = "PLAIN · translated", group = "LyricsScreen", widthDp = 412, heightDp = 650)
@Composable
private fun LyricsScreenTranslatedPlainPreview() {
    LyricsScreenPreview(PhonePreviewFixtures.lyricsScreenTranslatedPlain)
}

@Preview(name = "WORD", group = "LyricsScreen", widthDp = 412, heightDp = 650)
@Composable
private fun LyricsScreenWordPreview() {
    LyricsScreenPreview(PhonePreviewFixtures.lyricsScreenWord)
}

@Preview(name = "PLAIN", group = "LyricsScreen", widthDp = 412, heightDp = 650)
@Composable
private fun LyricsScreenPlainPreview() {
    LyricsScreenPreview(PhonePreviewFixtures.lyricsScreenPlain)
}

@Preview(name = "Long metadata", group = "LyricsScreen", widthDp = 412, heightDp = 650)
@Composable
private fun LyricsScreenLongMetadataPreview() {
    LyricsScreenPreview(PhonePreviewFixtures.lyricsScreenLongMetadata)
}

@Preview(name = "Constrained height", group = "LyricsScreen", widthDp = 412, heightDp = 420)
@Composable
private fun LyricsScreenConstrainedPreview() {
    LyricsScreenPreview(PhonePreviewFixtures.lyricsScreenLine)
}

@Composable
internal fun LyricsScreenPreview(
    initialState: LyricsScreenUiState,
    modifier: Modifier = Modifier,
) {
    AALyricsTheme {
        var state by remember(initialState) { mutableStateOf(initialState) }

        Box(
            modifier = modifier
                .fillMaxSize()
                .background(AALyricsColors.BackgroundBase),
        ) {
            LyricsScreen(
                state = state,
                modifier = Modifier.fillMaxSize(),
                onViewportInteractionModeChange = { mode ->
                    state = state.copy(
                        viewport = state.viewport.copy(interactionMode = mode),
                    )
                },
            )
        }
    }
}
