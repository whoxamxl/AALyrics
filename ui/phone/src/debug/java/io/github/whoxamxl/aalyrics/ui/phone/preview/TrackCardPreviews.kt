package io.github.whoxamxl.aalyrics.ui.phone.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.tooling.preview.Preview
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsColors
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsTheme
import io.github.whoxamxl.aalyrics.ui.phone.lyrics.TrackCard
import io.github.whoxamxl.aalyrics.ui.phone.lyrics.TrackCardUiState

@Preview(name = "Translation off · slot reserved", group = "TrackCard · Translation", widthDp = 412, showBackground = true)
@Composable
private fun TrackCardReadyPreview() {
    TrackCardPreview(
        state = PhonePreviewFixtures.trackCardReady,
        showArtwork = true,
    )
}

@Preview(name = "Translation enabled", group = "TrackCard · Translation", widthDp = 412, showBackground = true)
@Composable
private fun TrackCardTranslationOnPreview() {
    TrackCardPreview(state = PhonePreviewFixtures.trackCardTranslationOn, showArtwork = true)
}

@Preview(name = "Downloading models", group = "TrackCard · Translation", widthDp = 412, showBackground = true)
@Composable
private fun TrackCardTranslationDownloadingModelsPreview() {
    TrackCardPreview(
        state = PhonePreviewFixtures.trackCardTranslationDownloadingModels,
        showArtwork = true,
    )
}

@Preview(name = "Translating", group = "TrackCard · Translation", widthDp = 412, showBackground = true)
@Composable
private fun TrackCardTranslationTranslatingPreview() {
    TrackCardPreview(state = PhonePreviewFixtures.trackCardTranslationTranslating, showArtwork = true)
}

@Preview(name = "Ready EN → JA", group = "TrackCard · Translation", widthDp = 412, showBackground = true)
@Composable
private fun TrackCardTranslationReadyPreview() {
    TrackCardPreview(state = PhonePreviewFixtures.trackCardTranslationReady, showArtwork = true)
}

@Preview(name = "Not required", group = "TrackCard · Translation", widthDp = 412, showBackground = true)
@Composable
private fun TrackCardTranslationNotRequiredPreview() {
    TrackCardPreview(state = PhonePreviewFixtures.trackCardTranslationNotRequired, showArtwork = true)
}

@Preview(name = "Failed + Retry", group = "TrackCard · Translation", widthDp = 412, showBackground = true)
@Composable
private fun TrackCardTranslationFailedPreview() {
    TrackCardPreview(state = PhonePreviewFixtures.trackCardTranslationFailed, showArtwork = true)
}

@Preview(name = "Artwork placeholder", group = "TrackCard", widthDp = 412, showBackground = true)
@Composable
private fun TrackCardArtworkPlaceholderPreview() {
    TrackCardPreview(state = PhonePreviewFixtures.trackCardReady)
}

@Preview(name = "Long title marquee + drag", group = "TrackCard", widthDp = 412, showBackground = true)
@Composable
private fun TrackCardLongTitlePreview() {
    TrackCardPreview(
        state = PhonePreviewFixtures.trackCardLongTitle,
        showArtwork = true,
    )
}

@Preview(name = "Long artist marquee + drag", group = "TrackCard", widthDp = 412, showBackground = true)
@Composable
private fun TrackCardLongArtistPreview() {
    TrackCardPreview(
        state = PhonePreviewFixtures.trackCardLongArtist,
        showArtwork = true,
    )
}

@Preview(name = "Long title + artist marquee + drag", group = "TrackCard", widthDp = 412, showBackground = true)
@Composable
private fun TrackCardLongTitleAndArtistPreview() {
    TrackCardPreview(
        state = PhonePreviewFixtures.trackCardLongTitleAndArtist,
        showArtwork = true,
    )
}

@Preview(name = "Loading lyrics", group = "TrackCard", widthDp = 412, showBackground = true)
@Composable
private fun TrackCardLoadingPreview() {
    TrackCardPreview(
        state = PhonePreviewFixtures.trackCardLoading,
        showArtwork = true,
    )
}

@Preview(name = "Lyrics not found", group = "TrackCard", widthDp = 412, showBackground = true)
@Composable
private fun TrackCardNotFoundPreview() {
    TrackCardPreview(
        state = PhonePreviewFixtures.trackCardNotFound,
        showArtwork = true,
    )
}

@Preview(name = "Lyrics failed", group = "TrackCard", widthDp = 412, showBackground = true)
@Composable
private fun TrackCardFailedPreview() {
    TrackCardPreview(
        state = PhonePreviewFixtures.trackCardFailed,
        showArtwork = true,
    )
}

@Preview(name = "No provider or sync", group = "TrackCard", widthDp = 412, showBackground = true)
@Composable
private fun TrackCardNoMetadataPreview() {
    TrackCardPreview(
        state = PhonePreviewFixtures.trackCardNoMetadata,
        showArtwork = true,
    )
}

@Preview(name = "No artist", group = "TrackCard", widthDp = 412, showBackground = true)
@Composable
private fun TrackCardNoArtistPreview() {
    TrackCardPreview(state = PhonePreviewFixtures.trackCardNoArtist)
}

@Composable
private fun TrackCardPreview(
    state: TrackCardUiState,
    showArtwork: Boolean = false,
) {
    AALyricsTheme {
        if (showArtwork) {
            TrackCard(
                state = state,
                onTranslationRetry = {},
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    AALyricsColors.BackgroundSurface,
                                    AALyricsColors.AccentBlue,
                                    AALyricsColors.AccentCyan,
                                ),
                            ),
                        ),
                )
            }
        } else {
            TrackCard(
                state = state,
                onTranslationRetry = {},
            )
        }
    }
}
