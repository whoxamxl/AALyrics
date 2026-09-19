package io.github.whoxamxl.aalyrics.ui.phone.preview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsColors
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsSpacing
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsTheme
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsTypography
import io.github.whoxamxl.aalyrics.ui.phone.lyrics.TrackCard
import io.github.whoxamxl.aalyrics.ui.phone.navigation.PhoneDestination
import io.github.whoxamxl.aalyrics.ui.phone.shell.PhoneAppShell
import io.github.whoxamxl.aalyrics.ui.phone.state.PhoneShellUiState

@Preview(
    name = "Typical Lyrics shell",
    group = "PhoneAppShell",
    widthDp = 412,
    heightDp = 892,
    showBackground = true,
)
@Composable
private fun PhoneAppShellTypicalLyricsPreview() {
    PhoneAppShellPreview(PhonePreviewFixtures.typicalLyricsShell)
}

@Preview(
    name = "Playback controls hidden",
    group = "PhoneAppShell",
    widthDp = 412,
    heightDp = 892,
    showBackground = true,
)
@Composable
private fun PhoneAppShellHiddenControlsPreview() {
    PhoneAppShellPreview(PhonePreviewFixtures.lyricsWithoutControls)
}

@Preview(
    name = "Sync selected",
    group = "PhoneAppShell",
    widthDp = 412,
    heightDp = 892,
    showBackground = true,
)
@Composable
private fun PhoneAppShellSyncPreview() {
    PhoneAppShellPreview(PhonePreviewFixtures.syncShell)
}

@Composable
private fun PhoneAppShellPreview(state: PhoneShellUiState) {
    AALyricsTheme {
        PhoneAppShell(
            state = state,
            onDestinationSelected = {},
            onPrevious = {},
            onPlayPause = {},
            onNext = {},
        ) { destination ->
            when (destination) {
                PhoneDestination.Lyrics -> PreviewLyricsBody()
                else -> PreviewDestinationBody(destination)
            }
        }
    }
}

/** Debug-only body used to assess the destination space left by the production shell. */
@Composable
private fun PreviewLyricsBody() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(AALyricsSpacing.Space16),
        verticalArrangement = Arrangement.spacedBy(AALyricsSpacing.Space16),
    ) {
        TrackCard(state = PhonePreviewFixtures.trackCardReady)

        Column(verticalArrangement = Arrangement.spacedBy(AALyricsSpacing.Space8)) {
            PhonePreviewFixtures.lyricsLines.forEachIndexed { index, line ->
                Text(
                    text = line,
                    style = if (index == 1) {
                        AALyricsTypography.LyricsSupporting
                    } else {
                        AALyricsTypography.TrackArtist
                    },
                    color = if (index == 1) {
                        AALyricsColors.TextPrimary
                    } else {
                        AALyricsColors.TextSecondary
                    },
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun PreviewDestinationBody(destination: PhoneDestination) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(AALyricsSpacing.Space24),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = destination.label,
                style = AALyricsTypography.LyricsSupporting,
                color = AALyricsColors.TextPrimary,
            )
            Spacer(Modifier.height(AALyricsSpacing.Space8))
            Text(
                text = "Destination behavior is intentionally deferred",
                style = AALyricsTypography.TrackArtist,
                color = AALyricsColors.TextSecondary,
            )
        }
    }
}
