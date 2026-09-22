package io.github.whoxamxl.aalyrics.ui.phone.shell

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.whoxamxl.aalyrics.ui.designsystem.icon.AALyricsBrandMark
import io.github.whoxamxl.aalyrics.ui.designsystem.icon.AALyricsIcons
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsColors
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsRadius
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsSpacing
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsStroke
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsTypography
import io.github.whoxamxl.aalyrics.ui.phone.R
import io.github.whoxamxl.aalyrics.ui.phone.component.PhonePopupMenu
import io.github.whoxamxl.aalyrics.ui.phone.state.PlaybackSourceConnectionUiState
import io.github.whoxamxl.aalyrics.ui.phone.state.PlaybackSourceErrorUiReason

/** Compact persistent app identity and media-session runtime status presentation. */
@Composable
fun PhoneTopBar(
    mediaSourceLabel: String?,
    mediaSourceConnectionState: PlaybackSourceConnectionUiState =
        PlaybackSourceConnectionUiState.CONNECTING,
    mediaSourceErrorReason: PlaybackSourceErrorUiReason? = null,
    mediaSourceCanOpenApp: Boolean = false,
    mediaSourceIconPainter: Painter? = null,
    onOpenPlaybackApp: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = AALyricsColors.BackgroundChrome,
    ) {
        Row(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(
                    horizontal = AALyricsSpacing.Space16,
                    vertical = AALyricsSpacing.Space8,
                ),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AALyricsBrandMark(contentDescription = "AALyrics")
            Spacer(Modifier.width(AALyricsSpacing.Space8))
            Text(
                text = "AALyrics",
                style = AALyricsTypography.AppTitle,
                color = AALyricsColors.TextPrimary,
                maxLines = 1,
            )
            Spacer(Modifier.weight(1f))

            PlaybackSourceStatusPill(
                mediaSourceLabel = mediaSourceLabel,
                connectionState = mediaSourceConnectionState,
                errorReason = mediaSourceErrorReason,
                mediaSourceCanOpenApp = mediaSourceCanOpenApp,
                mediaSourceIconPainter = mediaSourceIconPainter,
                onOpenPlaybackApp = onOpenPlaybackApp,
            )
        }
    }
}

@Composable
private fun PlaybackSourceStatusPill(
    mediaSourceLabel: String?,
    connectionState: PlaybackSourceConnectionUiState,
    errorReason: PlaybackSourceErrorUiReason?,
    mediaSourceCanOpenApp: Boolean,
    mediaSourceIconPainter: Painter?,
    onOpenPlaybackApp: () -> Unit,
) {
    val palette = playbackSourceStatusPalette(connectionState)
    val canOpenConnectedSource =
        connectionState == PlaybackSourceConnectionUiState.CONNECTED &&
            mediaSourceCanOpenApp

    Surface(
        modifier = Modifier
            .widthIn(max = 220.dp)
            .clickable(
                enabled = canOpenConnectedSource,
                role = Role.Button,
                onClick = onOpenPlaybackApp,
            ),
        shape = RoundedCornerShape(AALyricsRadius.Full),
        color = palette.background,
        border = BorderStroke(
            width = AALyricsStroke.Thin,
            color = palette.border,
        ),
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = AALyricsSpacing.Space8,
                vertical = AALyricsSpacing.Space4,
            ),
            horizontalArrangement = Arrangement.spacedBy(AALyricsSpacing.Space4),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            when (connectionState) {
                PlaybackSourceConnectionUiState.CONNECTING -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        color = palette.foreground,
                        strokeWidth = AALyricsStroke.Strong,
                    )
                    StatusText(
                        text = stringResource(R.string.playback_source_connecting),
                        color = palette.foreground,
                    )
                }

                PlaybackSourceConnectionUiState.CONNECTED -> {
                    SourceIdentity(
                        label = mediaSourceLabel,
                        iconPainter = mediaSourceIconPainter,
                    )
                    StatusSeparator()
                    StatusText(
                        text = stringResource(R.string.playback_source_connected),
                        color = palette.foreground,
                    )
                    if (canOpenConnectedSource) {
                        Icon(
                            imageVector = AALyricsIcons.ExternalLink,
                            contentDescription = stringResource(
                                R.string.playback_source_open_app_description,
                            ),
                            tint = palette.foreground,
                            modifier = Modifier.size(AALyricsSpacing.Space16),
                        )
                    }
                }

                PlaybackSourceConnectionUiState.DISCONNECTED -> {
                    Icon(
                        imageVector = AALyricsIcons.Disconnected,
                        contentDescription = null,
                        tint = palette.foreground,
                        modifier = Modifier.size(AALyricsSpacing.Space16),
                    )
                    StatusText(
                        text = stringResource(R.string.playback_source_disconnected),
                        color = palette.foreground,
                    )
                }

                PlaybackSourceConnectionUiState.UNAVAILABLE -> {
                    if (!mediaSourceLabel.isNullOrBlank()) {
                        SourceIdentity(
                            label = mediaSourceLabel,
                            iconPainter = mediaSourceIconPainter,
                        )
                        StatusSeparator()
                    } else {
                        Icon(
                            imageVector = AALyricsIcons.Unavailable,
                            contentDescription = null,
                            tint = palette.foreground,
                            modifier = Modifier.size(AALyricsSpacing.Space16),
                        )
                    }
                    StatusText(
                        text = stringResource(R.string.playback_source_unavailable),
                        color = palette.foreground,
                    )
                }

                PlaybackSourceConnectionUiState.ERROR -> {
                    Icon(
                        imageVector = AALyricsIcons.Error,
                        contentDescription = null,
                        tint = palette.foreground,
                        modifier = Modifier.size(AALyricsSpacing.Space16),
                    )
                    StatusText(
                        text = stringResource(R.string.playback_source_error),
                        color = palette.foreground,
                    )
                    PlaybackSourceErrorTooltip(
                        reason = errorReason,
                        tint = palette.foreground,
                    )
                }
            }
        }
    }
}

private data class PlaybackSourceStatusPalette(
    val foreground: Color,
    val background: Color,
    val border: Color,
)

private fun playbackSourceStatusPalette(
    state: PlaybackSourceConnectionUiState,
): PlaybackSourceStatusPalette {
    val foreground = when (state) {
        PlaybackSourceConnectionUiState.CONNECTING -> AALyricsColors.TextSecondary
        PlaybackSourceConnectionUiState.CONNECTED -> AALyricsColors.Success
        PlaybackSourceConnectionUiState.DISCONNECTED -> AALyricsColors.TextTertiary
        PlaybackSourceConnectionUiState.UNAVAILABLE -> AALyricsColors.Warning
        PlaybackSourceConnectionUiState.ERROR -> AALyricsColors.Error
    }
    val backgroundBlend = when (state) {
        PlaybackSourceConnectionUiState.CONNECTING -> 0f
        PlaybackSourceConnectionUiState.DISCONNECTED -> 0.07f
        else -> 0.10f
    }
    val borderBlend = when (state) {
        PlaybackSourceConnectionUiState.CONNECTING -> 0f
        PlaybackSourceConnectionUiState.DISCONNECTED -> 0.20f
        else -> 0.42f
    }

    return PlaybackSourceStatusPalette(
        foreground = foreground,
        background = lerp(
            AALyricsColors.OverlaySoft,
            foreground,
            backgroundBlend,
        ),
        border = lerp(
            AALyricsColors.BorderSoft,
            foreground,
            borderBlend,
        ),
    )
}

@Composable
private fun RowScope.SourceIdentity(
    label: String?,
    iconPainter: Painter?,
) {
    if (iconPainter != null) {
        Image(
            painter = iconPainter,
            contentDescription = null,
            modifier = Modifier
                .size(AALyricsSpacing.Space16)
                .clip(RoundedCornerShape(AALyricsRadius.Radius4)),
        )
    } else {
        Box(
            modifier = Modifier
                .size(AALyricsSpacing.Space4)
                .background(
                    color = AALyricsColors.AccentCyan,
                    shape = CircleShape,
                ),
        )
    }

    Text(
        text = label.orEmpty(),
        style = AALyricsTypography.Label,
        color = AALyricsColors.TextPrimary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.weight(1f, fill = false),
    )
}

@Composable
private fun StatusSeparator() {
    Text(
        text = "·",
        style = AALyricsTypography.Label,
        color = AALyricsColors.TextSecondary,
        maxLines = 1,
    )
}

@Composable
private fun StatusText(
    text: String,
    color: Color,
) {
    Text(
        text = text,
        style = AALyricsTypography.Label,
        color = color,
        maxLines = 1,
    )
}

@Composable
private fun PlaybackSourceErrorTooltip(
    reason: PlaybackSourceErrorUiReason?,
    tint: Color,
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier.size(28.dp),
        ) {
            Icon(
                imageVector = AALyricsIcons.Info,
                contentDescription = stringResource(
                    R.string.playback_source_error_details_description,
                ),
                tint = tint,
                modifier = Modifier.size(AALyricsSpacing.Space16),
            )
        }

        PhonePopupMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.widthIn(max = 280.dp),
        ) {
            Text(
                text = playbackSourceErrorMessage(reason),
                style = AALyricsTypography.TrackArtist,
                color = AALyricsColors.TextPrimary,
                modifier = Modifier.padding(AALyricsSpacing.Space16),
            )
        }
    }
}

@Composable
private fun playbackSourceErrorMessage(
    reason: PlaybackSourceErrorUiReason?,
): String =
    when (reason) {
        PlaybackSourceErrorUiReason.NOTIFICATION_ACCESS_LOST ->
            stringResource(R.string.playback_source_error_notification_access_lost)
        PlaybackSourceErrorUiReason.SESSION_QUERY_FAILED ->
            stringResource(R.string.playback_source_error_session_query_failed)
        PlaybackSourceErrorUiReason.SESSION_ATTACH_FAILED ->
            stringResource(R.string.playback_source_error_session_attach_failed)
        PlaybackSourceErrorUiReason.UNKNOWN,
        null -> stringResource(R.string.playback_source_error_unknown)
    }
