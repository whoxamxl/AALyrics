package io.github.whoxamxl.aalyrics.ui.phone.shell

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.whoxamxl.aalyrics.ui.designsystem.icon.AALyricsIcons
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsColors
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsRadius
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsSpacing
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsStroke
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsTypography
import io.github.whoxamxl.aalyrics.ui.phone.component.TrackIdentityMarquee
import io.github.whoxamxl.aalyrics.ui.phone.state.PlaybackSurfaceUiState

/** Compact persistent playback surface shown above Phone bottom navigation. */
@Composable
internal fun PlaybackBar(
    state: PlaybackSurfaceUiState,
    progressFraction: Float?,
    onExpand: () -> Unit,
    onPlayPause: () -> Unit,
    modifier: Modifier = Modifier,
    artwork: (@Composable BoxScope.() -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = AALyricsSpacing.Space4,
                vertical = AALyricsSpacing.Space4,
            ),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(AALyricsRadius.Radius16),
            color = AALyricsColors.BackgroundSurfaceStrong.copy(alpha = 0.92f),
            border = BorderStroke(
                width = AALyricsStroke.Thin,
                color = AALyricsColors.BorderSoft.copy(alpha = 0.72f),
            ),
        ) {
            Box(Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 56.dp)
                            .clickable(onClick = onExpand)
                            .padding(
                                start = AALyricsSpacing.Space8,
                                end = AALyricsSpacing.Space8,
                                top = AALyricsSpacing.Space4,
                                bottom = AALyricsSpacing.Space4,
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        PlaybackArtwork(
                            artwork = artwork,
                            modifier = Modifier.size(40.dp),
                        )

                        Spacer(Modifier.width(AALyricsSpacing.Space8))

                        TrackIdentityMarquee(
                            title = state.title,
                            artist = state.artist,
                            titleStyle = AALyricsTypography.AppTitle,
                            artistStyle = AALyricsTypography.Label,
                            modifier = Modifier.weight(1f),
                        )
                    }

                    CompactPlayPauseButton(
                        isPlaying = state.isPlaying,
                        enabled = state.playPauseEnabled,
                        onClick = onPlayPause,
                        modifier = Modifier.padding(end = AALyricsSpacing.Space4),
                    )
                }

                progressFraction?.let { progress ->
                    PlaybackProgressIndicator(
                        progressFraction = progress,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(
                                start = AALyricsSpacing.Space12,
                                end = AALyricsSpacing.Space12,
                                bottom = 2.dp,
                            ),
                    )
                }
            }
        }
    }
}

@Composable
internal fun PlaybackArtwork(
    artwork: (@Composable BoxScope.() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(AALyricsRadius.Radius8))
            .background(AALyricsColors.OverlaySoft),
        contentAlignment = Alignment.Center,
    ) {
        artwork?.invoke(this)
    }
}

@Composable
private fun CompactPlayPauseButton(
    isPlaying: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.size(AALyricsSpacing.Space48),
        colors = IconButtonDefaults.iconButtonColors(
            contentColor = Color.Unspecified,
            disabledContentColor = Color.Unspecified,
        ),
    ) {
        Surface(
            modifier = Modifier.size(36.dp),
            shape = RoundedCornerShape(AALyricsRadius.Full),
            color = if (enabled) {
                AALyricsColors.AccentCyan
            } else {
                AALyricsColors.OverlaySoft
            },
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (isPlaying) AALyricsIcons.Pause else AALyricsIcons.Play,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = if (enabled) {
                        AALyricsColors.BackgroundBase
                    } else {
                        AALyricsColors.TextTertiary
                    },
                    modifier = Modifier.size(AALyricsSpacing.Space24),
                )
            }
        }
    }
}

@Composable
internal fun PlaybackProgressIndicator(
    progressFraction: Float,
    modifier: Modifier = Modifier,
) {
    val progress = progressFraction.coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(AALyricsStroke.Thin)
            .clip(RoundedCornerShape(AALyricsRadius.Full))
            .background(AALyricsColors.BorderSoft.copy(alpha = 0.48f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress)
                .height(AALyricsStroke.Thin)
                .background(AALyricsColors.AccentCyan.copy(alpha = 0.9f)),
        )
    }
}

internal val PlaybackSurfaceOverlayInset = 64.dp
