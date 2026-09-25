package io.github.whoxamxl.aalyrics.ui.phone.shell

import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsSpacing
import io.github.whoxamxl.aalyrics.ui.phone.R
import io.github.whoxamxl.aalyrics.ui.phone.state.PlaybackQueueItemUiState
import io.github.whoxamxl.aalyrics.ui.phone.state.PlaybackSurfaceUiState
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Shell-owned collapsed Playback Bar + on-demand Expanded Player. */
@Composable
fun PlaybackSurface(
    state: PlaybackSurfaceUiState,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onQueueItemSelected: (Long) -> Unit,
    onOpenPlaybackApp: () -> Unit,
    onTranslationEnabledChanged: (Boolean) -> Unit,
    karaokeFeatureEnabled: Boolean = false,
    karaokeModeEnabled: Boolean = false,
    onKaraokeModeEnabledChanged: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
    artwork: (@Composable BoxScope.() -> Unit)? = null,
    queueArtwork: (@Composable BoxScope.(PlaybackQueueItemUiState) -> Unit)? = null,
) {
    val collapseLabel = stringResource(R.string.playback_collapse)
    val density = LocalDensity.current
    val transformTravelPx = with(density) { PLAYBACK_SURFACE_TRANSFORM_TRAVEL.toPx() }
    val flingThresholdPxPerSecond = with(density) {
        PLAYBACK_SURFACE_FLING_THRESHOLD.toPx()
    }
    val coroutineScope = rememberCoroutineScope()
    val backdropInteractionSource = remember { MutableInteractionSource() }

    var expanded by rememberSaveable { mutableStateOf(false) }
    var transformOffsetPx by remember {
        mutableFloatStateOf(if (expanded) -transformTravelPx else 0f)
    }
    val settleJob = remember { mutableStateOf<Job?>(null) }
    var previewPositionMs by remember { mutableStateOf<Long?>(null) }
    var pendingCommittedPositionMs by remember { mutableStateOf<Long?>(null) }
    val livePositionMs = rememberLivePlaybackPositionMs(state)

    val displayedPositionMs = previewPositionMs
        ?: pendingCommittedPositionMs
        ?: livePositionMs

    val progressFraction = state.durationMs?.let { duration ->
        (displayedPositionMs.toDouble() / duration.toDouble())
            .coerceIn(0.0, 1.0)
            .toFloat()
    }
    val expansionProgress = playbackSurfaceExpansionProgress(
        transformOffsetPx = transformOffsetPx,
        transformTravelPx = transformTravelPx,
    )

    fun cancelPreview() {
        previewPositionMs = null
    }

    fun stopSettleAnimation() {
        settleJob.value?.cancel()
        settleJob.value = null
    }

    fun settleTo(targetExpanded: Boolean) {
        if (!targetExpanded) cancelPreview()
        expanded = targetExpanded
        stopSettleAnimation()

        val targetOffsetPx = if (targetExpanded) -transformTravelPx else 0f
        val startOffsetPx = transformOffsetPx
        val remainingFraction = (
            abs(targetOffsetPx - startOffsetPx) / transformTravelPx
        ).coerceIn(0f, 1f)

        if (remainingFraction <= PLAYBACK_SURFACE_SETTLE_EPSILON) {
            transformOffsetPx = targetOffsetPx
            return
        }

        val durationMs = (
            PLAYBACK_SURFACE_SETTLE_DURATION_MS *
                remainingFraction.coerceAtLeast(PLAYBACK_SURFACE_MIN_SETTLE_FRACTION)
        ).roundToInt()

        settleJob.value = coroutineScope.launch {
            animate(
                initialValue = startOffsetPx,
                targetValue = targetOffsetPx,
                animationSpec = tween(
                    durationMillis = durationMs,
                    easing = FastOutSlowInEasing,
                ),
            ) { value, _ ->
                transformOffsetPx = value
            }
        }
    }

    fun expand() {
        settleTo(targetExpanded = true)
    }

    fun collapse() {
        settleTo(targetExpanded = false)
    }

    fun settleFromDrag(velocityPxPerSecond: Float) {
        val targetExpanded = playbackSurfaceSettlesExpanded(
            expansionProgress = playbackSurfaceExpansionProgress(
                transformOffsetPx = transformOffsetPx,
                transformTravelPx = transformTravelPx,
            ),
            velocityPxPerSecond = velocityPxPerSecond,
            flingThresholdPxPerSecond = flingThresholdPxPerSecond,
        )
        settleTo(targetExpanded)
    }

    fun updateTransformDrag(deltaPx: Float) {
        stopSettleAnimation()
        transformOffsetPx = (transformOffsetPx + deltaPx)
            .coerceIn(-transformTravelPx, 0f)
    }

    val collapsedDragState = rememberDraggableState(::updateTransformDrag)
    val expandedDragState = rememberDraggableState(::updateTransformDrag)
    val collapsedTransformDragModifier = Modifier.draggable(
        state = collapsedDragState,
        orientation = Orientation.Vertical,
        onDragStarted = {
            stopSettleAnimation()
        },
        onDragStopped = { velocity -> settleFromDrag(velocity) },
    )
    val expandedTransformDragModifier = Modifier.draggable(
        state = expandedDragState,
        orientation = Orientation.Vertical,
        onDragStarted = {
            stopSettleAnimation()
        },
        onDragStopped = { velocity -> settleFromDrag(velocity) },
    )

    fun commitSeek(positionMs: Long) {
        val duration = state.durationMs ?: return
        val target = positionMs.coerceIn(0L, duration)
        previewPositionMs = null
        pendingCommittedPositionMs = target
        onSeekTo(target)
    }

    BackHandler(enabled = expanded || expansionProgress > 0f) {
        collapse()
    }

    LaunchedEffect(state.playbackIdentityKey, state.durationMs, state.seekEnabled) {
        previewPositionMs = null
        pendingCommittedPositionMs = null
    }

    LaunchedEffect(
        state.positionMs,
        state.positionUpdatedAtMonotonicMs,
        pendingCommittedPositionMs,
    ) {
        val pending = pendingCommittedPositionMs ?: return@LaunchedEffect
        if (abs(livePositionMs - pending) <= SEEK_RECONCILE_TOLERANCE_MS) {
            pendingCommittedPositionMs = null
        }
    }

    LaunchedEffect(pendingCommittedPositionMs) {
        if (pendingCommittedPositionMs == null) return@LaunchedEffect
        delay(SEEK_RECONCILE_TIMEOUT_MS)
        pendingCommittedPositionMs = null
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (expansionProgress > 0f || expanded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = expansionProgress
                    }
                    .background(Color.Black.copy(alpha = 0.24f))
                    .clickable(
                        interactionSource = backdropInteractionSource,
                        indication = null,
                        onClickLabel = collapseLabel,
                        onClick = ::collapse,
                    ),
            )
        }

        if (expansionProgress < 1f || !expanded) {
            PlaybackBar(
                state = state,
                progressFraction = progressFraction,
                onExpand = ::expand,
                onPlayPause = onPlayPause,
                transformationDragModifier = collapsedTransformDragModifier,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .graphicsLayer {
                        alpha = 1f - expansionProgress
                    },
                artwork = artwork,
            )
        }

        if (expansionProgress > 0f || expanded) {
            ExpandedPlayer(
                state = state,
                displayedPositionMs = displayedPositionMs,
                onCollapse = ::collapse,
                onPrevious = onPrevious,
                onPlayPause = onPlayPause,
                onNext = onNext,
                onSeekPreview = { previewPositionMs = it },
                onSeekCommit = ::commitSeek,
                onSeekCancel = ::cancelPreview,
                onQueueItemSelected = onQueueItemSelected,
                onOpenPlaybackApp = onOpenPlaybackApp,
                onTranslationEnabledChanged = onTranslationEnabledChanged,
                karaokeFeatureEnabled = karaokeFeatureEnabled,
                karaokeModeEnabled = karaokeModeEnabled,
                onKaraokeModeEnabledChanged = onKaraokeModeEnabledChanged,
                transformationDragModifier = expandedTransformDragModifier,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = AALyricsSpacing.Space4)
                    .graphicsLayer {
                        translationY = transformTravelPx + transformOffsetPx
                        alpha = expansionProgress
                    },
                artwork = artwork,
                queueArtwork = queueArtwork,
            )
        }
    }
}

@Composable
private fun rememberLivePlaybackPositionMs(
    state: PlaybackSurfaceUiState,
): Long {
    var positionMs by remember { mutableLongStateOf(state.positionMs) }
    LaunchedEffect(
        state.playbackIdentityKey,
        state.positionMs,
        state.playbackRate,
        state.isPlaying,
        state.durationMs,
        state.positionUpdatedAtMonotonicMs,
        state.positionSampledAtMonotonicMs,
    ) {
        positionMs = projectedLivePlaybackPositionMs(
            positionMs = state.positionMs,
            playbackRate = state.playbackRate,
            isPlaying = state.isPlaying,
            durationMs = state.durationMs,
            currentMonotonicTimeMs = SystemClock.elapsedRealtime(),
            sourceUpdatedAtMonotonicMs = state.positionUpdatedAtMonotonicMs,
            sampledAtMonotonicMs = state.positionSampledAtMonotonicMs,
        )

        if (!state.isPlaying || state.playbackRate <= 0f) return@LaunchedEffect

        var previousFrameNanos = withFrameNanos { it }
        while (true) {
            val frameNanos = withFrameNanos { it }
            val elapsedMs = (frameNanos - previousFrameNanos) / 1_000_000.0
            previousFrameNanos = frameNanos

            positionMs = clampPlaybackPosition(
                positionMs = positionMs + (elapsedMs * state.playbackRate).toLong(),
                durationMs = state.durationMs,
            )
        }
    }

    return positionMs
}

internal fun projectedLivePlaybackPositionMs(
    positionMs: Long,
    playbackRate: Float,
    isPlaying: Boolean,
    durationMs: Long?,
    currentMonotonicTimeMs: Long,
    sourceUpdatedAtMonotonicMs: Long?,
    sampledAtMonotonicMs: Long?,
): Long {
    val updatedAtMonotonicMs = sourceUpdatedAtMonotonicMs
        ?: sampledAtMonotonicMs
    val callbackAgeMs = if (isPlaying && updatedAtMonotonicMs != null) {
        (currentMonotonicTimeMs - updatedAtMonotonicMs).coerceAtLeast(0L)
    } else {
        0L
    }
    val initialAdvanceMs = (callbackAgeMs * playbackRate).toLong()
    return clampPlaybackPosition(
        positionMs = positionMs + initialAdvanceMs,
        durationMs = durationMs,
    )
}

private fun clampPlaybackPosition(
    positionMs: Long,
    durationMs: Long?,
): Long = if (durationMs != null) {
    positionMs.coerceIn(0L, durationMs)
} else {
    positionMs.coerceAtLeast(0L)
}

internal fun playbackSurfaceExpansionProgress(
    transformOffsetPx: Float,
    transformTravelPx: Float,
): Float {
    require(transformTravelPx > 0f) { "Transform travel must be positive" }
    return (-transformOffsetPx / transformTravelPx).coerceIn(0f, 1f)
}

internal fun playbackSurfaceSettlesExpanded(
    expansionProgress: Float,
    velocityPxPerSecond: Float,
    flingThresholdPxPerSecond: Float,
): Boolean {
    require(flingThresholdPxPerSecond > 0f) { "Fling threshold must be positive" }
    return when {
        velocityPxPerSecond <= -flingThresholdPxPerSecond -> true
        velocityPxPerSecond >= flingThresholdPxPerSecond -> false
        else -> expansionProgress.coerceIn(0f, 1f) >= 0.5f
    }
}

private val PLAYBACK_SURFACE_TRANSFORM_TRAVEL = 156.dp
private val PLAYBACK_SURFACE_FLING_THRESHOLD = 600.dp
private const val PLAYBACK_SURFACE_SETTLE_DURATION_MS = 240
private const val PLAYBACK_SURFACE_MIN_SETTLE_FRACTION = 0.30f
private const val PLAYBACK_SURFACE_SETTLE_EPSILON = 0.001f
private const val SEEK_RECONCILE_TOLERANCE_MS = 2_000L
private const val SEEK_RECONCILE_TIMEOUT_MS = 1_500L
