package io.github.whoxamxl.aalyrics.ui.phone.shell

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.github.whoxamxl.aalyrics.ui.designsystem.icon.AALyricsIcons
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsColors
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsSpacing
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsStroke
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
                IconButton(
                    onClick = onPrevious,
                    enabled = state.previousEnabled,
                    modifier = Modifier.size(AALyricsSpacing.Space48),
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = AALyricsColors.TextPrimary,
                        disabledContentColor = AALyricsColors.TextTertiary,
                    ),
                ) {
                    Icon(
                        imageVector = AALyricsIcons.Previous,
                        contentDescription = "Previous",
                    )
                }

                FilledIconButton(
                    onClick = onPlayPause,
                    enabled = state.playPauseEnabled,
                    modifier = Modifier.size(AALyricsSpacing.Space48),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = AALyricsColors.AccentCyan,
                        contentColor = AALyricsColors.BackgroundBase,
                        disabledContainerColor = AALyricsColors.OverlaySoft,
                        disabledContentColor = AALyricsColors.TextTertiary,
                    ),
                ) {
                    Icon(
                        imageVector = if (state.isPlaying) {
                            AALyricsIcons.Pause
                        } else {
                            AALyricsIcons.Play
                        },
                        contentDescription = if (state.isPlaying) "Pause" else "Play",
                    )
                }

                IconButton(
                    onClick = onNext,
                    enabled = state.nextEnabled,
                    modifier = Modifier.size(AALyricsSpacing.Space48),
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = AALyricsColors.TextPrimary,
                        disabledContentColor = AALyricsColors.TextTertiary,
                    ),
                ) {
                    Icon(
                        imageVector = AALyricsIcons.Next,
                        contentDescription = "Next",
                    )
                }
            }
        }
    }
}
