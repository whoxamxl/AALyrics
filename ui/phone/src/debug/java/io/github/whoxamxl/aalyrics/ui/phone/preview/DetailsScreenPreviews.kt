package io.github.whoxamxl.aalyrics.ui.phone.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsColors
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsTheme
import io.github.whoxamxl.aalyrics.ui.phone.details.DetailsScreen
import io.github.whoxamxl.aalyrics.ui.phone.details.DetailsScreenUiState

@Preview(name = "Typical", group = "DetailsScreen", widthDp = 412, heightDp = 760)
@Composable
private fun DetailsTypicalPreview() {
    DetailsScreenPreview(PhonePreviewFixtures.detailsTypical)
}

@Preview(name = "Verbose", group = "DetailsScreen", widthDp = 412, heightDp = 900)
@Composable
private fun DetailsVerbosePreview() {
    DetailsScreenPreview(PhonePreviewFixtures.detailsVerbose)
}

@Preview(
    name = "Verbose · undefined app category",
    group = "DetailsScreen",
    widthDp = 412,
    heightDp = 900,
)
@Composable
private fun DetailsVerboseUndefinedCategoryPreview() {
    DetailsScreenPreview(PhonePreviewFixtures.detailsVerboseSparse)
}

@Preview(name = "Partial", group = "DetailsScreen", widthDp = 412, heightDp = 760)
@Composable
private fun DetailsPartialPreview() {
    DetailsScreenPreview(PhonePreviewFixtures.detailsPartial)
}

@Preview(name = "Lyrics loading", group = "DetailsScreen", widthDp = 412, heightDp = 760)
@Composable
private fun DetailsLoadingPreview() {
    DetailsScreenPreview(PhonePreviewFixtures.detailsLoading)
}

@Preview(name = "Narrow · 320dp", group = "DetailsScreen", widthDp = 320, heightDp = 700)
@Composable
private fun DetailsNarrowPreview() {
    DetailsScreenPreview(PhonePreviewFixtures.detailsVerbose)
}

@Preview(
    name = "Enlarged font",
    group = "DetailsScreen",
    widthDp = 412,
    heightDp = 860,
    fontScale = 1.4f,
)
@Composable
private fun DetailsLargeFontPreview() {
    DetailsScreenPreview(PhonePreviewFixtures.detailsVerbose)
}

@Composable
private fun DetailsScreenPreview(state: DetailsScreenUiState) {
    AALyricsTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AALyricsColors.BackgroundBase),
        ) {
            DetailsScreen(
                state = state,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
