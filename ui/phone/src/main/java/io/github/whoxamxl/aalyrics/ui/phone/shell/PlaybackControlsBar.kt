package io.github.whoxamxl.aalyrics.ui.phone.shell

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.whoxamxl.aalyrics.ui.designsystem.icon.AALyricsIcons
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsColors
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsRadius
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
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = AALyricsSpacing.Space16,
                vertical = AALyricsSpacing.Space4,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.width(184.dp),
            shape = RoundedCornerShape(AALyricsRadius.Radius16),
            color = AALyricsColors.BackgroundSurfaceStrong,
            border = BorderStroke(
                width = AALyricsStroke.Thin,
                color = AALyricsColors.BorderSoft,
            ),
        ) {
            Row(
                modifier = Modifier.padding(
                    horizontal = AALyricsSpacing.Space8,
                    vertical = AALyricsSpacing.Space4,
                ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = onPrevious,
                    enabled = state.previousEnabled,
                    modifier = Modifier.size(AALyricsSpacing.Space40),
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

                IconButton(
                    onClick = onPlayPause,
                    enabled = state.playPauseEnabled,
                    modifier = Modifier
                        .size(width = AALyricsSpacing.Space48, height = AALyricsSpacing.Space40)
                        .background(
                            color = if (state.playPauseEnabled) {
                                AALyricsColors.AccentCyan
                            } else {
                                AALyricsColors.OverlaySoft
                            },
                            shape = RoundedCornerShape(AALyricsRadius.Radius12),
                        ),
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = if (state.playPauseEnabled) {
                            AALyricsColors.BackgroundBase
                        } else {
                            AALyricsColors.TextTertiary
                        },
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
                    modifier = Modifier.size(AALyricsSpacing.Space40),
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
