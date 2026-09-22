package io.github.whoxamxl.aalyrics.ui.phone.shell

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.whoxamxl.aalyrics.ui.designsystem.icon.AALyricsIcons
import io.github.whoxamxl.aalyrics.ui.phone.component.PhonePopupMenu
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsColors
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsRadius
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsSpacing
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsStroke
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
    transformationDragModifier: Modifier = Modifier,
    modifier: Modifier = Modifier,
    artwork: (@Composable BoxScope.() -> Unit)? = null,
    queueArtwork: (@Composable BoxScope.(PlaybackQueueItemUiState) -> Unit)? = null,
) {
    var queueVisible by remember { mutableStateOf(false) }

    LaunchedEffect(state.queueAvailable) {
        if (!state.queueAvailable) queueVisible = false
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = AALyricsSpacing.Space8)
            // Keep this surface in the pointer hit-test chain so taps on empty panel
            // chrome do not fall through to the backdrop. Do not consume here:
            // child Slider/buttons own gesture consumption and cancellation semantics.
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent()
                    }
                }
            },
        shape = RoundedCornerShape(AALyricsRadius.Radius24),
        color = AALyricsColors.BackgroundSurfaceStrong.copy(alpha = 0.98f),
        shadowElevation = AALyricsSpacing.Space12,
    ) {
        Column(
            modifier = Modifier.padding(
                start = AALyricsSpacing.Space16,
                top = AALyricsSpacing.Space8,
                end = AALyricsSpacing.Space16,
                bottom = AALyricsSpacing.Space12,
            ),
        ) {
            ExpandedPlayerHeader(
                state = state,
                onCollapse = onCollapse,
                transformationDragModifier = transformationDragModifier,
                artwork = artwork,
            )

            Spacer(Modifier.height(AALyricsSpacing.Space8))

            key(state.playbackIdentityKey) {
                ExpandedSeekArea(
                    enabled = state.seekEnabled,
                    displayedPositionMs = displayedPositionMs,
                    durationMs = state.durationMs,
                    onSeekPreview = onSeekPreview,
                    onSeekCommit = onSeekCommit,
                    onSeekCancel = onSeekCancel,
                )
            }

            Spacer(Modifier.height(AALyricsSpacing.Space4))

            ExpandedTransportRow(
                state = state,
                displayedPositionMs = displayedPositionMs,
                onPrevious = onPrevious,
                onPlayPause = onPlayPause,
                onNext = onNext,
                onSeekPreview = onSeekPreview,
                onSeekCommit = onSeekCommit,
                onSeekCancel = onSeekCancel,
                onQueue = { queueVisible = true },
                onOpenPlaybackApp = onOpenPlaybackApp,
                onTranslationEnabledChanged = onTranslationEnabledChanged,
            )
        }
    }

    if (queueVisible && state.queueAvailable) {
        PlaybackQueueSheet(
            queue = state.queue,
            canOpenPlaybackApp = state.canOpenPlaybackApp,
            onOpenPlaybackApp = onOpenPlaybackApp,
            onQueueItemSelected = {
                queueVisible = false
                onQueueItemSelected(it)
            },
            queueArtwork = queueArtwork,
            onDismissRequest = { queueVisible = false },
        )
    }
}

@Composable
private fun PlaybackDragHandle() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = AALyricsSpacing.Space4),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .width(AALyricsSpacing.Space32)
                .height(3.dp)
                .clip(RoundedCornerShape(AALyricsRadius.Full))
                .background(AALyricsColors.TextTertiary.copy(alpha = 0.48f)),
        )
    }
}

@Composable
private fun ExpandedPlayerHeader(
    state: PlaybackSurfaceUiState,
    onCollapse: () -> Unit,
    transformationDragModifier: Modifier,
    artwork: (@Composable BoxScope.() -> Unit)?,
) {
    val collapseLabel = stringResource(R.string.playback_collapse)
    val collapseInteractionSource = remember { MutableInteractionSource() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(transformationDragModifier)
            .clickable(
                interactionSource = collapseInteractionSource,
                indication = null,
                role = Role.Button,
                onClickLabel = collapseLabel,
                onClick = onCollapse,
            ),
    ) {
        PlaybackDragHandle()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PlaybackArtwork(
                artwork = artwork,
                modifier = Modifier.size(AALyricsSpacing.Space48),
            )

            Spacer(Modifier.width(AALyricsSpacing.Space8))

            TrackIdentityMarquee(
                title = state.title,
                artist = state.artist,
                titleStyle = AALyricsTypography.AppTitle,
                artistStyle = AALyricsTypography.TrackArtist,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpandedSeekArea(
    enabled: Boolean,
    displayedPositionMs: Long,
    durationMs: Long?,
    onSeekPreview: (Long) -> Unit,
    onSeekCommit: (Long) -> Unit,
    onSeekCancel: () -> Unit,
) {
    val duration = durationMs ?: 1L
    val positionDescription = stringResource(R.string.playback_position)
    val positionStateDescription = durationMs?.let {
        stringResource(
            R.string.playback_position_state,
            formatPlaybackTime(displayedPositionMs),
            formatPlaybackTime(it),
        )
    } ?: formatPlaybackTime(displayedPositionMs)

    val interactionSource = remember { MutableInteractionSource() }
    var cancelled by remember { mutableStateOf(false) }
    var isDragging by remember { mutableStateOf(false) }
    val currentPosition by rememberUpdatedState(displayedPositionMs)
    val trackHeight by animateDpAsState(
        targetValue = if (isDragging) 12.dp else 4.dp,
        animationSpec = tween(durationMillis = 160),
        label = "seek-track-height",
    )

    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is DragInteraction.Start -> {
                    cancelled = false
                    isDragging = true
                }

                is DragInteraction.Stop -> {
                    isDragging = false
                }

                is DragInteraction.Cancel -> {
                    isDragging = false
                    cancelled = true
                    onSeekCancel()
                }
            }
        }
    }

    val seekColors = SliderDefaults.colors(
        thumbColor = Color.Transparent,
        activeTrackColor = AALyricsColors.TextPrimary.copy(alpha = 0.92f),
        inactiveTrackColor = AALyricsColors.TextTertiary.copy(alpha = 0.42f),
        disabledThumbColor = Color.Transparent,
        disabledActiveTrackColor = AALyricsColors.TextTertiary.copy(alpha = 0.58f),
        disabledInactiveTrackColor = AALyricsColors.BorderSoft.copy(alpha = 0.42f),
    )

    Column(Modifier.fillMaxWidth()) {
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
            colors = seekColors,
            thumb = {
                Box(
                    modifier = Modifier.size(18.dp),
                )
            },
            track = { sliderState ->
                SliderDefaults.Track(
                    sliderState = sliderState,
                    modifier = Modifier
                        .height(trackHeight)
                        .clip(RoundedCornerShape(AALyricsRadius.Full)),
                    enabled = enabled,
                    colors = seekColors,
                    drawStopIndicator = null,
                    thumbTrackGapSize = 0.dp,
                    trackInsideCornerSize = 0.dp,
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = positionDescription
                    stateDescription = positionStateDescription
                },
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AALyricsSpacing.Space4),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = formatPlaybackTime(displayedPositionMs),
                style = AALyricsTypography.Label,
                color = AALyricsColors.TextTertiary,
            )
            Text(
                text = durationMs?.let(::formatPlaybackTime) ?: "--:--",
                style = AALyricsTypography.Label,
                color = AALyricsColors.TextTertiary,
            )
        }
    }
}

@Composable
private fun ExpandedTransportRow(
    state: PlaybackSurfaceUiState,
    displayedPositionMs: Long,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onSeekPreview: (Long) -> Unit,
    onSeekCommit: (Long) -> Unit,
    onSeekCancel: () -> Unit,
    onQueue: () -> Unit,
    onOpenPlaybackApp: () -> Unit,
    onTranslationEnabledChanged: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        QuickControlsButton(
            translationEnabled = state.translationEnabled,
            onTranslationEnabledChanged = onTranslationEnabledChanged,
        )

        key(state.playbackIdentityKey) {
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
        }

        ExpandedPlayPauseButton(
            isPlaying = state.isPlaying,
            enabled = state.playPauseEnabled,
            onClick = onPlayPause,
        )

        key(state.playbackIdentityKey) {
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
        }

        when {
            state.queueAvailable -> PlayerIconAction(
                imageVector = AALyricsIcons.Queue,
                contentDescription = stringResource(R.string.playback_queue),
                onClick = onQueue,
            )

            state.canOpenPlaybackApp -> PlayerIconAction(
                imageVector = AALyricsIcons.OpenPlaybackApp,
                contentDescription = stringResource(R.string.playback_open_app),
                onClick = onOpenPlaybackApp,
            )

            else -> Spacer(Modifier.size(AALyricsSpacing.Space48))
        }
    }
}

@Composable
private fun PlayerIconAction(
    imageVector: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(AALyricsSpacing.Space48),
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            tint = if (enabled) {
                AALyricsColors.TextSecondary
            } else {
                AALyricsColors.TextTertiary
            },
            modifier = Modifier.size(AALyricsSpacing.Space24),
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
        PlayerIconAction(
            imageVector = AALyricsIcons.QuickControls,
            contentDescription = stringResource(R.string.playback_quick_controls),
            onClick = { expanded = true },
        )

        PhonePopupMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.width(208.dp),
        ) {
            QuickControlTranslationRow(
                enabled = translationEnabled,
                onEnabledChanged = onTranslationEnabledChanged,
            )
        }
    }
}

@Composable
internal fun QuickControlTranslationRow(
    enabled: Boolean,
    onEnabledChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .toggleable(
                value = enabled,
                role = Role.Switch,
                onValueChange = onEnabledChanged,
            )
            .padding(
                start = AALyricsSpacing.Space16,
                end = AALyricsSpacing.Space12,
                top = AALyricsSpacing.Space8,
                bottom = AALyricsSpacing.Space8,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.playback_translation),
            style = AALyricsTypography.TrackArtist,
            color = AALyricsColors.TextPrimary,
            modifier = Modifier.weight(1f),
        )

        CompactPlaybackToggle(checked = enabled)
    }
}

@Composable
private fun CompactPlaybackToggle(
    checked: Boolean,
) {
    Box(
        modifier = Modifier
            .size(width = 38.dp, height = 22.dp)
            .clip(RoundedCornerShape(AALyricsRadius.Full))
            .background(
                if (checked) {
                    AALyricsColors.AccentCyan
                } else {
                    AALyricsColors.OverlaySoft
                },
            )
            .then(
                if (checked) {
                    Modifier
                } else {
                    Modifier.border(
                        width = AALyricsStroke.Thin,
                        color = AALyricsColors.BorderSoft,
                        shape = RoundedCornerShape(AALyricsRadius.Full),
                    )
                },
            )
            .padding(3.dp),
    ) {
        Box(
            modifier = Modifier
                .align(if (checked) Alignment.CenterEnd else Alignment.CenterStart)
                .size(16.dp)
                .clip(RoundedCornerShape(AALyricsRadius.Full))
                .background(
                    if (checked) {
                        AALyricsColors.BackgroundBase
                    } else {
                        AALyricsColors.TextSecondary
                    },
                ),
        )
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

    Box(
        modifier = Modifier
            .size(AALyricsSpacing.Space48)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                enabled = skipEnabled,
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
            tint = if (skipEnabled) {
                AALyricsColors.TextPrimary
            } else {
                AALyricsColors.TextTertiary
            },
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
        modifier = Modifier.size(52.dp),
    ) {
        Surface(
            modifier = Modifier.size(44.dp),
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
                    contentDescription = stringResource(
                        if (isPlaying) R.string.playback_pause else R.string.playback_play,
                    ),
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaybackQueueSheet(
    queue: List<PlaybackQueueItemUiState>,
    canOpenPlaybackApp: Boolean,
    onOpenPlaybackApp: () -> Unit,
    onQueueItemSelected: (Long) -> Unit,
    queueArtwork: (@Composable BoxScope.(PlaybackQueueItemUiState) -> Unit)?,
    onDismissRequest: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        dragHandle = null,
        containerColor = AALyricsColors.BackgroundSurfaceStrong,
        contentColor = AALyricsColors.TextPrimary,
        scrimColor = Color.Black.copy(alpha = 0.48f),
        shape = RoundedCornerShape(
            topStart = AALyricsRadius.Radius24,
            topEnd = AALyricsRadius.Radius24,
        ),
    ) {
        PlaybackQueueSheetContent(
            queue = queue,
            canOpenPlaybackApp = canOpenPlaybackApp,
            onOpenPlaybackApp = onOpenPlaybackApp,
            onQueueItemSelected = onQueueItemSelected,
            queueArtwork = queueArtwork,
            modifier = Modifier.fillMaxHeight(QUEUE_SHEET_HEIGHT_FRACTION),
        )
    }
}

@Composable
internal fun PlaybackQueueSheetContent(
    queue: List<PlaybackQueueItemUiState>,
    canOpenPlaybackApp: Boolean,
    onOpenPlaybackApp: () -> Unit,
    onQueueItemSelected: (Long) -> Unit,
    modifier: Modifier = Modifier,
    queueArtwork: (@Composable BoxScope.(PlaybackQueueItemUiState) -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = AALyricsSpacing.Space16),
    ) {
        PlaybackQueueDragHandle()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = AALyricsSpacing.Space48),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.playback_queue),
                style = AALyricsTypography.AppTitle,
                color = AALyricsColors.TextPrimary,
                modifier = Modifier.weight(1f),
            )

            if (canOpenPlaybackApp) {
                PlayerIconAction(
                    imageVector = AALyricsIcons.OpenPlaybackApp,
                    contentDescription = stringResource(R.string.playback_open_app),
                    onClick = onOpenPlaybackApp,
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    bottom = AALyricsSpacing.Space56Compat,
                ),
            ) {
                items(
                    items = queue,
                    key = { it.id },
                ) { item ->
                    QueueTrackRow(
                        item = item,
                        onClick = { onQueueItemSelected(item.id) },
                        queueArtwork = queueArtwork,
                    )
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .fillMaxHeight(QUEUE_BOTTOM_FADE_FRACTION)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                AALyricsColors.BackgroundSurfaceStrong.copy(alpha = 0.92f),
                                AALyricsColors.BackgroundSurfaceStrong,
                            ),
                        ),
                    ),
            )
        }
    }
}

@Composable
private fun PlaybackQueueDragHandle() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                top = AALyricsSpacing.Space8,
                bottom = AALyricsSpacing.Space4,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .width(AALyricsSpacing.Space32)
                .height(3.dp)
                .clip(RoundedCornerShape(AALyricsRadius.Full))
                .background(AALyricsColors.TextTertiary.copy(alpha = 0.48f)),
        )
    }
}

@Composable
private fun QueueTrackRow(
    item: PlaybackQueueItemUiState,
    onClick: () -> Unit,
    queueArtwork: (@Composable BoxScope.(PlaybackQueueItemUiState) -> Unit)?,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                role = Role.Button,
                onClick = onClick,
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = AALyricsSpacing.Space56Compat)
                .padding(
                    horizontal = AALyricsSpacing.Space4,
                    vertical = AALyricsSpacing.Space4,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PlaybackArtwork(
                artwork = if (
                    queueArtwork != null &&
                    (item.hasEmbeddedArtwork || item.artworkUri != null)
                ) {
                    { queueArtwork(item) }
                } else {
                    {
                        Icon(
                            imageVector = AALyricsIcons.MusicNote,
                            contentDescription = null,
                            tint = AALyricsColors.TextTertiary,
                            modifier = Modifier.size(AALyricsSpacing.Space20),
                        )
                    }
                },
                modifier = Modifier.size(36.dp),
            )

            Spacer(Modifier.width(AALyricsSpacing.Space12))

            Column(Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = AALyricsTypography.AppTitle,
                    color = AALyricsColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                item.subtitle?.let {
                    Text(
                        text = it,
                        style = AALyricsTypography.Label,
                        color = AALyricsColors.TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 48.dp)
                .height(AALyricsStroke.Thin)
                .background(AALyricsColors.BorderSoft.copy(alpha = 0.56f)),
        )
    }
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

private const val QUEUE_SHEET_HEIGHT_FRACTION = 0.72f
private const val QUEUE_BOTTOM_FADE_FRACTION = 0.15f

private val AALyricsSpacing.Space56Compat get() = 56.dp
