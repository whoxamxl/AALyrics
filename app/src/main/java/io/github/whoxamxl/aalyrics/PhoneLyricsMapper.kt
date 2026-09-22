package io.github.whoxamxl.aalyrics

import io.github.whoxamxl.aalyrics.core.lyrics.LyricsState
import io.github.whoxamxl.aalyrics.core.model.LyricsDocument
import io.github.whoxamxl.aalyrics.core.model.LyricsSyncType
import io.github.whoxamxl.aalyrics.core.model.PlaybackSnapshot
import io.github.whoxamxl.aalyrics.core.model.TimedLyricLine
import io.github.whoxamxl.aalyrics.ui.phone.lyrics.LyricsScreenUiState
import io.github.whoxamxl.aalyrics.ui.phone.lyrics.LyricsViewportInteractionMode
import io.github.whoxamxl.aalyrics.ui.phone.lyrics.LyricsViewportLineUiState
import io.github.whoxamxl.aalyrics.ui.phone.lyrics.LyricsViewportUiState
import io.github.whoxamxl.aalyrics.ui.phone.lyrics.TrackCardLyricsStatus
import io.github.whoxamxl.aalyrics.ui.phone.lyrics.TrackCardUiState
import kotlin.math.roundToLong

internal fun mapPhoneLyricsState(
    playback: PlaybackSnapshot,
    lyricsState: LyricsState,
    plainLyricsAutoScrollEnabled: Boolean,
    interactionMode: LyricsViewportInteractionMode,
    currentMonotonicTimeMs: Long,
): LyricsScreenUiState {
    val track = playback.track
    val matchingLyricsState = lyricsState
        .takeIf { state ->
            state !is LyricsState.ForLookup ||
                state.lookup.playbackIdentity == playback.trackIdentity
        }
    val document = when (matchingLyricsState) {
        is LyricsState.Ready -> matchingLyricsState.lyrics
        is LyricsState.Degraded -> matchingLyricsState.lyrics
        else -> null
    }
    val positionMs = projectedPlaybackPosition(playback, currentMonotonicTimeMs)
    val sourceSyncType = document?.syncType ?: LyricsSyncType.PLAIN
    val displaySyncType = if (sourceSyncType == LyricsSyncType.WORD) {
        LyricsSyncType.LINE
    } else {
        sourceSyncType
    }

    return LyricsScreenUiState(
        trackCard = TrackCardUiState(
            title = track?.title ?: "No active track",
            artist = track
                ?.artists
                ?.takeIf { it.isNotEmpty() }
                ?.joinToString(separator = ", "),
            providerLabel = document?.attribution?.displayName,
            syncLabel = document?.let { sourceSyncType.label() },
            lyricsStatus = when (matchingLyricsState) {
                is LyricsState.Loading -> TrackCardLyricsStatus.LOADING
                is LyricsState.Ready,
                is LyricsState.Degraded -> TrackCardLyricsStatus.READY
                is LyricsState.NotFound -> TrackCardLyricsStatus.NOT_FOUND
                is LyricsState.Failed -> TrackCardLyricsStatus.FAILED
                else -> TrackCardLyricsStatus.IDLE
            },
        ),
        viewport = LyricsViewportUiState(
            lines = document
                ?.lines
                .orEmpty()
                .map { line -> LyricsViewportLineUiState(text = line.text) },
            syncType = displaySyncType,
            currentLineIndex = document?.currentTimedLineIndex(positionMs),
            playbackProgress = track
                ?.durationMs
                ?.takeIf { it > 0L }
                ?.let { duration ->
                    (positionMs.toDouble() / duration.toDouble())
                        .coerceIn(0.0, 1.0)
                        .toFloat()
                },
            plainAutoScrollEnabled = plainLyricsAutoScrollEnabled,
            interactionMode = interactionMode,
        ),
    )
}

internal fun projectedPlaybackPosition(
    playback: PlaybackSnapshot,
    currentMonotonicTimeMs: Long,
): Long {
    val base = playback.positionMs
    if (!playback.isPlaying || playback.playbackRate <= 0f) {
        return playback.track?.durationMs?.let { base.coerceIn(0L, it) } ?: base
    }

    val elapsedMs = playback.positionUpdatedAtMonotonicMs
        ?.let { updatedAt -> (currentMonotonicTimeMs - updatedAt).coerceAtLeast(0L) }
        ?: 0L
    val projected = base + (elapsedMs * playback.playbackRate)
        .toDouble()
        .roundToLong()

    return playback.track?.durationMs
        ?.let { projected.coerceIn(0L, it) }
        ?: projected.coerceAtLeast(0L)
}

internal fun LyricsDocument.currentTimedLineIndex(positionMs: Long): Int? =
    lines.indices
        .filter { index ->
            val line = lines[index]
            line is TimedLyricLine && line.startMs <= positionMs
        }
        .lastOrNull()

private fun LyricsSyncType.label(): String = when (this) {
    LyricsSyncType.PLAIN -> "Plain"
    LyricsSyncType.LINE -> "Line synced"
    LyricsSyncType.WORD -> "Word synced"
}
