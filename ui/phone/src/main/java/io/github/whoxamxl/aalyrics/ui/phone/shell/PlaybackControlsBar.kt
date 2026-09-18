package io.github.whoxamxl.aalyrics.ui.phone.shell

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
                TransportButton(
                    imageVector = AALyricsIcons.Previous,
                    contentDescription = "Previous",
                    enabled = state.previousEnabled,
                    onClick = onPrevious,
                )
                TransportButton(
                    imageVector = if (state.isPlaying) {
                        AALyricsIcons.Pause
                    } else {
                        AALyricsIcons.Play
                    },
                    contentDescription = if (state.isPlaying) "Pause" else "Play",
                    enabled = state.playPauseEnabled,
                    emphasized = true,
                    onClick = onPlayPause,
                )
                TransportButton(
                    imageVector = AALyricsIcons.Next,
                    contentDescription = "Next",
                    enabled = state.nextEnabled,
                    onClick = onNext,
                )
            }
        }
    }
}

@Composable
private fun TransportButton(
    imageVector: ImageVector,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
    emphasized: Boolean = false,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(width = 56.dp, height = AALyricsSpacing.Space48),
        colors = IconButtonDefaults.iconButtonColors(
            contentColor = Color.Unspecified,
            disabledContentColor = Color.Unspecified,
        ),
    ) {
        Surface(
            modifier = Modifier.size(
                width = if (emphasized) 52.dp else AALyricsSpacing.Space48,
                height = 36.dp,
            ),
            shape = RoundedCornerShape(AALyricsRadius.Radius12),
            color = when {
                emphasized && enabled -> AALyricsColors.AccentCyan
                emphasized -> AALyricsColors.OverlaySoft
                else -> AALyricsColors.OverlaySoft
            },
            border = if (emphasized) {
                null
            } else {
                BorderStroke(
                    width = AALyricsStroke.Thin,
                    color = AALyricsColors.BorderSoft,
                )
            },
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = imageVector,
                    contentDescription = contentDescription,
                    tint = when {
                        !enabled -> AALyricsColors.TextTertiary
                        emphasized -> AALyricsColors.BackgroundBase
                        else -> AALyricsColors.TextPrimary
                    },
                )
            }
        }
    }
}
