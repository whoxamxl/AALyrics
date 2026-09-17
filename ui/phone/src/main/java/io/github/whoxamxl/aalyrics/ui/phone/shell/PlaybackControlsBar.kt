package io.github.whoxamxl.aalyrics.ui.phone.shell

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsColors
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsSpacing
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsStroke
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsTypography
import io.github.whoxamxl.aalyrics.ui.phone.state.PlaybackControlsUiState

/** Presentation-only Previous / Play-Pause / Next controls. */
@Composable
fun PlaybackControlsBar(
    state: PlaybackControlsUiState,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = AALyricsColors.BackgroundSurfaceStrong,
    ) {
        androidx.compose.foundation.layout.Column {
            HorizontalDivider(
                thickness = AALyricsStroke.Thin,
                color = AALyricsColors.BorderSoft,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = AALyricsSpacing.Space16,
                        vertical = AALyricsSpacing.Space4,
                    ),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    onClick = onPrevious,
                    enabled = state.previousEnabled,
                ) {
                    Text("Previous", style = AALyricsTypography.Label)
                }
                FilledTonalButton(
                    onClick = onPlayPause,
                    enabled = state.playPauseEnabled,
                ) {
                    Text(
                        text = if (state.isPlaying) "Pause" else "Play",
                        style = AALyricsTypography.Label,
                    )
                }
                TextButton(
                    onClick = onNext,
                    enabled = state.nextEnabled,
                ) {
                    Text("Next", style = AALyricsTypography.Label)
                }
            }
        }
    }
}
