package io.github.whoxamxl.aalyrics.ui.phone.shell

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.github.whoxamxl.aalyrics.ui.designsystem.icon.AALyricsIcons
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsColors
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsRadius
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsSpacing
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsTypography
import io.github.whoxamxl.aalyrics.ui.phone.R
import io.github.whoxamxl.aalyrics.ui.phone.component.TrackIdentityMarquee
import io.github.whoxamxl.aalyrics.ui.phone.state.PlaybackQueueItemUiState
import io.github.whoxamxl.aalyrics.ui.phone.state.PlaybackSurfaceUiState
import kotlinx.coroutines.flow.collect

/** On-demand compact player expanded upward from the persistent Playback Bar. */
@Composable
internal fun ExpandedPlayer(
    state: PlaybackSurfaceUiState,
    displayedPositionMs: Long,
    onCollapse: () -> Unit,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onSeekPreview: (Long) -> Unit,
    onSeekCommit: (Long) -> Unit,
    onSeekCancel: () -> Unit,
    onQueueItemSelected: (Long) -> Unit,
    onOpenPlaybackApp: () -> Unit,
    onTranslationEnabledChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    artwork: (@Composable BoxScope.() -> Unit)? = null,
) {
    var queueVisible by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = AALyricsSpacing.Space4),
        shape = RoundedCornerShape(AALyricsRadius.Radius16),
        color = AALyricsColors.BackgroundSurfaceStrong,
        border = BorderStroke(
            width = io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsStroke.Thin,
            color = AALyricsColors.BorderSoft,
        ),
        shadowElevation = AALyricsSpacing.Space8,
    ) {
        Column(
            modifier = Modifier.padding(
                start = AALyricsSpacing.Space12,
                top = AALyricsSpacing.Space8,
                end = AALyricsSpacing.Space12,
                bottom = AALyricsSpacing.Space8,
            ),
        ) {
            ExpandedPlayerHeader(
                state = state,
                onCollapse = onCollapse,
                artwork = artwork,
            )

            Spacer(Modifier.size(AALyricsSpacing.Space8))

            ExpandedSeekRow(
                enabled = state.seekEnabled,
                displayedPositionMs = displayedPositionMs,
                durationMs = state.durationMs,
                onSeekPreview = onSeekPreview,
                onSeekCommit = onSeekCommit,
                onSeekCancel = onSeekCancel,
            )

            Spacer(Modifier.size(AALyricsSpacing.Space4))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                QuickControlsButton(
                    translationEnabled = state.translationEnabled,
                    onTranslationEnabledChanged = onTranslationEnabledChanged,
                )

                RelativeSeekTransportButton(
                    imageVector = AALyricsIcons.Previous,
                    contentDescription = stringResource(R.string.playback_previous),
                    longClickDescription = stringResource(R.string.playback_seek_backward),
                    skipEnabled = state.canSkipPrevious,
                    relativeSeekEnabled = state.seekEnabled,
                    currentPositionMs = displayedPositionMs,
                    durationMs = state.durationMs,
                    direction = RelativeSeekDirection.BACKWARD,
                    onSkip = onPrevious,
                    onSeekPreview = onSeekPreview,
                    onSeekCommit = onSeekCommit,
                    onSeekCancel = onSeekCancel,
                )

                ExpandedPlayPauseButton(
                    isPlaying = state.isPlaying,
                    enabled = state.playPauseEnabled,
                    onClick = onPlayPause,
                )

                RelativeSeekTransportButton(
                    imageVector = AALyricsIcons.Next,
                    contentDescription = stringResource(R.string.playback_next),
                    longClickDescription = stringResource(R.string.playback_seek_forward),
                    skipEnabled = state.canSkipNext,
                    relativeSeekEnabled = state.seekEnabled,
                    currentPositionMs = displayedPositionMs,
                    durationMs = state.durationMs,
                    direction = RelativeSeekDirection.FORWARD,
                    onSkip = onNext,
                    onSeekPreview = onSeekPreview,
                    onSeekCommit = onSeekCommit,
                    onSeekCancel = onSeekCancel,
                )

                when {
                    state.queueAvailable -> {
                        IconButton(
                            onClick = { queueVisible = true },
                            modifier = Modifier.size(AALyricsSpacing.Space48),
                        ) {
                            Icon(
                                imageVector = AALyricsIcons.Queue,
                                contentDescription = stringResource(R.string.playback_queue),
                                tint = AALyricsColors.TextPrimary,
                            )
                        }
                    }

                    state.canOpenPlaybackApp -> {
                        IconButton(
                            onClick = onOpenPlaybackApp,
                            modifier = Modifier.size(AALyricsSpacing.Space48),
                        ) {
                            Icon(
                                imageVector = AALyricsIcons.OpenPlaybackApp,
                                contentDescription = stringResource(R.string.playback_open_app),
                                tint = AALyricsColors.TextPrimary,
                            )
                        }
                    }

                    else -> Spacer(Modifier.size(AALyricsSpacing.Space48))
                }
            }
        }
    }

    if (queueVisible) {
        PlaybackQueueDialog(
            queue = state.queue,
            onQueueItemSelected = {
                queueVisible = false
                onQueueItemSelected(it)
            },
            onDismissRequest = { queueVisible = false },
        )
    }
}

@Composable
private fun ExpandedPlayerHeader(
    state: PlaybackSurfaceUiState,
    onCollapse: () -> Unit,
    artwork: (@Composable BoxScope.() -> Unit)?,
) {
    val density = LocalDensity.current
    val collapseLabel = stringResource(R.string.playback_collapse)
    var downwardDragPx by remember { mutableFloatStateOf(0f) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .pointerInput(onCollapse) {
                detectVerticalDragGestures(
                    onDragStart = { downwardDragPx = 0f },
                    onVerticalDrag = { change, dragAmount ->
                        if (dragAmount > 0f) {
                            downwardDragPx += dragAmount
                            change.consume()
                        }
                    },
                    onDragCancel = { downwardDragPx = 0f },
                    onDragEnd = {
                        val threshold = with(density) { 48.dp.toPx() }
                        if (downwardDragPx >= threshold) onCollapse()
                        downwardDragPx = 0f
                    },
                )
            }
            .combinedClickable(
                role = Role.Button,
                onClickLabel = collapseLabel,
                onClick = onCollapse,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlaybackArtwork(
            artwork = artwork,
            modifier = Modifier.size(AALyricsSpacing.Space48),
        )
        Spacer(Modifier.width(AALyricsSpacing.Space12))
        TrackIdentityMarquee(
            title = state.title,
            artist = state.artist,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ExpandedSeekRow(
    enabled: Boolean,
    displayedPositionMs: Long,
    durationMs: Long?,
    onSeekPreview: (Long) -> Unit,
    onSeekCommit: (Long) -> Unit,
    onSeekCancel: () -> Unit,
) {
    val duration = durationMs ?: 1L
    val interactionSource = remember { MutableInteractionSource() }
    var cancelled by remember { mutableStateOf(false) }
    val currentPosition by rememberUpdatedState(displayedPositionMs)

    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is DragInteraction.Start -> cancelled = false
                is DragInteraction.Cancel -> {
                    cancelled = true
                    onSeekCancel()
                }
            }
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = formatPlaybackTime(displayedPositionMs),
            style = AALyricsTypography.Label,
            color = AALyricsColors.TextSecondary,
            modifier = Modifier.width(44.dp),
        )

        Slider(
            value = displayedPositionMs.coerceIn(0L, duration).toFloat(),
            onValueChange = { value ->
                if (enabled) onSeekPreview(value.toLong())
            },
            onValueChangeFinished = {
                if (enabled && !cancelled) onSeekCommit(currentPosition)
                cancelled = false
            },
            enabled = enabled,
            valueRange = 0f..duration.toFloat(),
            interactionSource = interactionSource,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = AALyricsSpacing.Space4),
        )

        Text(
            text = durationMs?.let(::formatPlaybackTime) ?: "--:--",
            style = AALyricsTypography.Label,
            color = AALyricsColors.TextSecondary,
            modifier = Modifier.width(44.dp),
        )
    }
}

@Composable
private fun QuickControlsButton(
    translationEnabled: Boolean,
    onTranslationEnabledChanged: (Boolean) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier.size(AALyricsSpacing.Space48),
        ) {
            Icon(
                imageVector = AALyricsIcons.QuickControls,
                contentDescription = stringResource(R.string.playback_quick_controls),
                tint = AALyricsColors.TextPrimary,
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.widthIn(min = 220.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = AALyricsSpacing.Space48)
                    .toggleable(
                        value = translationEnabled,
                        role = Role.Switch,
                        onValueChange = onTranslationEnabledChanged,
                    )
                    .padding(horizontal = AALyricsSpacing.Space16),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.playback_translation),
                    style = AALyricsTypography.AppTitle,
                    color = AALyricsColors.TextPrimary,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = translationEnabled,
                    onCheckedChange = null,
                )
            }
        }
    }
}

@Composable
private fun RelativeSeekTransportButton(
    imageVector: ImageVector,
    contentDescription: String,
    longClickDescription: String,
    skipEnabled: Boolean,
    relativeSeekEnabled: Boolean,
    currentPositionMs: Long,
    durationMs: Long?,
    direction: RelativeSeekDirection,
    onSkip: () -> Unit,
    onSeekPreview: (Long) -> Unit,
    onSeekCommit: (Long) -> Unit,
    onSeekCancel: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val haptic = LocalHapticFeedback.current
    var scrubbing by remember { mutableStateOf(false) }
    var scrubStartPositionMs by remember { mutableLongStateOf(0L) }
    var previewPositionMs by remember { mutableLongStateOf(currentPositionMs) }

    LaunchedEffect(scrubbing, scrubStartPositionMs, durationMs, direction) {
        val duration = durationMs ?: return@LaunchedEffect
        if (!scrubbing) return@LaunchedEffect

        val startNanos = withFrameNanos { it }
        while (scrubbing) {
            val nowNanos = withFrameNanos { it }
            val heldMs = ((nowNanos - startNanos) / 1_000_000L).coerceAtLeast(0L)
            previewPositionMs = relativeSeekPreviewPositionMs(
                startPositionMs = scrubStartPositionMs,
                durationMs = duration,
                heldAfterLongPressMs = heldMs,
                direction = direction,
            )
            onSeekPreview(previewPositionMs)
        }
    }

    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Release -> {
                    if (scrubbing) {
                        scrubbing = false
                        onSeekCommit(previewPositionMs)
                    }
                }

                is PressInteraction.Cancel -> {
                    if (scrubbing) {
                        scrubbing = false
                        onSeekCancel()
                    }
                }
            }
        }
    }

    val enabled = skipEnabled
    Box(
        modifier = Modifier
            .size(AALyricsSpacing.Space48)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                enabled = enabled,
                role = Role.Button,
                onClickLabel = contentDescription,
                onLongClickLabel = longClickDescription,
                hapticFeedbackEnabled = false,
                onLongClick = if (relativeSeekEnabled && durationMs != null) {
                    {
                        scrubStartPositionMs = currentPositionMs.coerceIn(0L, durationMs)
                        previewPositionMs = scrubStartPositionMs
                        scrubbing = true
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSeekPreview(previewPositionMs)
                    }
                } else {
                    null
                },
                onClick = onSkip,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            tint = if (enabled) AALyricsColors.TextPrimary else AALyricsColors.TextTertiary,
            modifier = Modifier.size(28.dp),
        )
    }
}

@Composable
private fun ExpandedPlayPauseButton(
    isPlaying: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(AALyricsSpacing.Space48),
    ) {
        Surface(
            modifier = Modifier.size(AALyricsSpacing.Space40),
            shape = RoundedCornerShape(AALyricsRadius.Full),
            color = if (enabled) AALyricsColors.AccentCyan else AALyricsColors.OverlaySoft,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (isPlaying) AALyricsIcons.Pause else AALyricsIcons.Play,
                    contentDescription = stringResource(
                        if (isPlaying) R.string.playback_pause else R.string.playback_play,
                    ),
                    tint = if (enabled) AALyricsColors.BackgroundBase else AALyricsColors.TextTertiary,
                    modifier = Modifier.size(AALyricsSpacing.Space24),
                )
            }
        }
    }
}

@Composable
private fun PlaybackQueueDialog(
    queue: List<PlaybackQueueItemUiState>,
    onQueueItemSelected: (Long) -> Unit,
    onDismissRequest: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(
                text = stringResource(R.string.playback_queue),
                style = AALyricsTypography.TrackTitle,
                color = AALyricsColors.TextPrimary,
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                queue.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = AALyricsSpacing.Space48)
                            .combinedClickable(
                                role = Role.Button,
                                onClick = { onQueueItemSelected(item.id) },
                            )
                            .padding(
                                horizontal = AALyricsSpacing.Space8,
                                vertical = AALyricsSpacing.Space4,
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = item.title,
                                style = AALyricsTypography.AppTitle,
                                color = AALyricsColors.TextPrimary,
                                maxLines = 1,
                            )
                            item.subtitle?.let {
                                Text(
                                    text = it,
                                    style = AALyricsTypography.TrackArtist,
                                    color = AALyricsColors.TextSecondary,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.playback_close))
            }
        },
        containerColor = AALyricsColors.BackgroundSurfaceStrong,
        shape = RoundedCornerShape(AALyricsRadius.Radius16),
    )
}

internal fun formatPlaybackTime(positionMs: Long): String {
    val totalSeconds = positionMs.coerceAtLeast(0L) / 1_000L
    val hours = totalSeconds / 3_600L
    val minutes = (totalSeconds % 3_600L) / 60L
    val seconds = totalSeconds % 60L
    val secondText = seconds.toString().padStart(2, '0')
    return if (hours > 0L) {
        hours.toString() + ":" + minutes.toString().padStart(2, '0') + ":" + secondText
    } else {
        minutes.toString() + ":" + secondText
    }
}
