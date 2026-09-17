package io.github.whoxamxl.aalyrics.ui.phone.preview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsColors
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsRadius
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsSpacing
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsTheme
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsTypography
import io.github.whoxamxl.aalyrics.ui.phone.navigation.PhoneDestination
import io.github.whoxamxl.aalyrics.ui.phone.shell.PhoneAppShell
import io.github.whoxamxl.aalyrics.ui.phone.state.PhoneShellUiState

@Preview(
    name = "Lyrics · playing",
    widthDp = 412,
    heightDp = 892,
    showBackground = true,
    showSystemUi = true,
)
@Composable
private fun PlayingLyricsShellPreview() {
    PhoneShellPreview(PreviewPhoneData.playingLyrics)
}

@Preview(
    name = "Lyrics · narrow paused",
    widthDp = 320,
    heightDp = 700,
    showBackground = true,
    showSystemUi = true,
)
@Composable
private fun NarrowPausedLyricsShellPreview() {
    PhoneShellPreview(PreviewPhoneData.pausedLyrics)
}

@Preview(name = "Sync · selected", widthDp = 412, heightDp = 892, showBackground = true)
@Composable
private fun SyncShellPreview() {
    PhoneShellPreview(PreviewPhoneData.syncDestination)
}

@Preview(name = "Details · disabled controls", widthDp = 412, heightDp = 892, showBackground = true)
@Composable
private fun DisabledControlsShellPreview() {
    PhoneShellPreview(PreviewPhoneData.disabledDetails)
}

@Preview(name = "Settings · unavailable controls", widthDp = 412, heightDp = 892, showBackground = true)
@Composable
private fun UnavailableControlsShellPreview() {
    PhoneShellPreview(PreviewPhoneData.unavailableSettings)
}

@Composable
private fun PhoneShellPreview(state: PhoneShellUiState) {
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

/** Debug-only body used to assess how much room the production shell leaves for Lyrics. */
@Composable
private fun PreviewLyricsBody() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(AALyricsSpacing.Space16),
        verticalArrangement = Arrangement.spacedBy(AALyricsSpacing.Space16),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(AALyricsRadius.Radius16),
            color = AALyricsColors.BackgroundSurfaceStrong,
        ) {
            Row(
                modifier = Modifier.padding(AALyricsSpacing.Space12),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier.size(AALyricsSpacing.Space64),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(AALyricsRadius.Radius12),
                    color = AALyricsColors.OverlaySoft,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "AA",
                            style = AALyricsTypography.AppTitle,
                            color = AALyricsColors.AccentCyan,
                        )
                    }
                }
                Spacer(Modifier.width(AALyricsSpacing.Space12))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Midnight Signals",
                        style = AALyricsTypography.TrackTitle,
                        color = AALyricsColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "The Northbound Lights",
                        style = AALyricsTypography.TrackArtist,
                        color = AALyricsColors.TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(AALyricsSpacing.Space4))
                    Text(
                        text = "Sample source · Word synced",
                        style = AALyricsTypography.Label,
                        color = AALyricsColors.AccentCyan,
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(AALyricsSpacing.Space8)) {
            PreviewPhoneData.lyricsLines.forEachIndexed { index, line ->
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
