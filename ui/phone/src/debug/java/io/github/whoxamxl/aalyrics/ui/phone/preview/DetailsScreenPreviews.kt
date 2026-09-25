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

@Preview(
    name = "Translation · active secondary",
    group = "DetailsScreen · Translation",
    widthDp = 412,
    heightDp = 820,
)
@Composable
private fun DetailsTranslationActiveSecondaryPreview() {
    DetailsScreenPreview(PhonePreviewFixtures.detailsTranslationActiveSecondary)
}

@Preview(
    name = "Translation · no source profile",
    group = "DetailsScreen · Translation",
    widthDp = 412,
    heightDp = 820,
)
@Composable
private fun DetailsTranslationNoSourceProfilePreview() {
    DetailsScreenPreview(PhonePreviewFixtures.detailsTranslationNoSourceProfile)
}

@Preview(
    name = "Runtime · Disabled",
    group = "DetailsScreen · Translation runtime",
    widthDp = 412,
    heightDp = 860,
)
@Composable
private fun DetailsTranslationRuntimeDisabledPreview() {
    DetailsScreenPreview(PhonePreviewFixtures.detailsTranslationRuntimeDisabled)
}

@Preview(
    name = "Runtime · Idle",
    group = "DetailsScreen · Translation runtime",
    widthDp = 412,
    heightDp = 860,
)
@Composable
private fun DetailsTranslationRuntimeIdlePreview() {
    DetailsScreenPreview(PhonePreviewFixtures.detailsTranslationRuntimeIdle)
}

@Preview(
    name = "Runtime · Translating",
    group = "DetailsScreen · Translation runtime",
    widthDp = 412,
    heightDp = 900,
)
@Composable
private fun DetailsTranslationRuntimeTranslatingPreview() {
    DetailsScreenPreview(PhonePreviewFixtures.detailsTranslationRuntimeTranslating)
}

@Preview(
    name = "Runtime · Not required",
    group = "DetailsScreen · Translation runtime",
    widthDp = 412,
    heightDp = 900,
)
@Composable
private fun DetailsTranslationRuntimeNotRequiredPreview() {
    DetailsScreenPreview(PhonePreviewFixtures.detailsTranslationRuntimeNotRequired)
}

@Preview(
    name = "Runtime · Ready",
    group = "DetailsScreen · Translation runtime",
    widthDp = 412,
    heightDp = 920,
)
@Composable
private fun DetailsTranslationRuntimeReadyPreview() {
    DetailsScreenPreview(PhonePreviewFixtures.detailsTranslationRuntimeReady)
}

@Preview(
    name = "Runtime · Failed + tooltips",
    group = "DetailsScreen · Translation runtime",
    widthDp = 412,
    heightDp = 940,
)
@Composable
private fun DetailsTranslationRuntimeFailedPreview() {
    DetailsScreenPreview(PhonePreviewFixtures.detailsTranslationRuntimeFailed)
}

@Preview(
    name = "Models · Checking",
    group = "DetailsScreen · Translation models",
    widthDp = 412,
    heightDp = 880,
)
@Composable
private fun DetailsTranslationModelsCheckingPreview() {
    DetailsScreenPreview(PhonePreviewFixtures.detailsTranslationModelsChecking)
}

@Preview(
    name = "Models · built-in Ready while OFF",
    group = "DetailsScreen · Translation models",
    widthDp = 412,
    heightDp = 880,
)
@Composable
private fun DetailsTranslationBuiltInReadyWhileOffPreview() {
    DetailsScreenPreview(PhonePreviewFixtures.detailsTranslationBuiltInReadyWhileOff)
}

@Preview(
    name = "Models · downloaded Ready while OFF",
    group = "DetailsScreen · Translation models",
    widthDp = 412,
    heightDp = 880,
)
@Composable
private fun DetailsTranslationDownloadedReadyWhileOffPreview() {
    DetailsScreenPreview(PhonePreviewFixtures.detailsTranslationDownloadedReadyWhileOff)
}

@Preview(
    name = "Models · unsupported secondary",
    group = "DetailsScreen · Translation models",
    widthDp = 412,
    heightDp = 920,
)
@Composable
private fun DetailsTranslationUnsupportedSecondaryPreview() {
    DetailsScreenPreview(PhonePreviewFixtures.detailsTranslationUnsupportedSecondary)
}

@Preview(
    name = "Models · Downloading + waiting",
    group = "DetailsScreen · Translation models",
    widthDp = 412,
    heightDp = 920,
)
@Composable
private fun DetailsTranslationModelsPreparingPreview() {
    DetailsScreenPreview(PhonePreviewFixtures.detailsTranslationModelsPreparing)
}

@Preview(
    name = "Models · Failed + timed out",
    group = "DetailsScreen · Translation models",
    widthDp = 412,
    heightDp = 960,
)
@Composable
private fun DetailsTranslationModelsFailedPreview() {
    DetailsScreenPreview(PhonePreviewFixtures.detailsTranslationModelsFailed)
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
