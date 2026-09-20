package io.github.whoxamxl.aalyrics.ui.phone.shell

import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsSpacing
import io.github.whoxamxl.aalyrics.ui.phone.state.PlaybackSurfaceUiState
import kotlin.math.abs
import kotlinx.coroutines.delay

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
    modifier: Modifier = Modifier,
    artwork: (@Composable BoxScope.() -> Unit)? = null,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
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

    fun cancelPreview() {
        previewPositionMs = null
    }

    fun collapse() {
        cancelPreview()
        expanded = false
    }

    fun commitSeek(positionMs: Long) {
        val duration = state.durationMs ?: return
        val target = positionMs.coerceIn(0L, duration)
        previewPositionMs = null
        pendingCommittedPositionMs = target
        onSeekTo(target)
    }

    BackHandler(enabled = expanded) {
        collapse()
    }

    LaunchedEffect(state.title, state.artist, state.durationMs, state.seekEnabled) {
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
        AnimatedVisibility(
            visible = expanded,
            modifier = Modifier.fillMaxSize(),
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.24f))
                    .clickable(
                        onClickLabel = "Collapse player",
                        onClick = ::collapse,
                    ),
            )
        }

        AnimatedVisibility(
            visible = !expanded,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            PlaybackBar(
                state = state,
                progressFraction = progressFraction,
                onExpand = { expanded = true },
                onPlayPause = onPlayPause,
                artwork = artwork,
            )
        }

        AnimatedVisibility(
            visible = expanded,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = AALyricsSpacing.Space4),
            enter = expandVertically(expandFrom = Alignment.Bottom) + fadeIn(),
            exit = shrinkVertically(shrinkTowards = Alignment.Bottom) + fadeOut(),
        ) {
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
                artwork = artwork,
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
        state.positionMs,
        state.playbackRate,
        state.isPlaying,
        state.durationMs,
        state.positionUpdatedAtMonotonicMs,
        state.title,
        state.artist,
    ) {
        val callbackAgeMs = if (
            state.isPlaying &&
            state.positionUpdatedAtMonotonicMs != null
        ) {
            (SystemClock.elapsedRealtime() - state.positionUpdatedAtMonotonicMs)
                .coerceAtLeast(0L)
        } else {
            0L
        }

        val initialAdvanceMs = (callbackAgeMs * state.playbackRate).toLong()
        positionMs = clampPlaybackPosition(
            positionMs = state.positionMs + initialAdvanceMs,
            durationMs = state.durationMs,
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

private fun clampPlaybackPosition(
    positionMs: Long,
    durationMs: Long?,
): Long = if (durationMs != null) {
    positionMs.coerceIn(0L, durationMs)
} else {
    positionMs.coerceAtLeast(0L)
}

private const val SEEK_RECONCILE_TOLERANCE_MS = 2_000L
private const val SEEK_RECONCILE_TIMEOUT_MS = 1_500L
