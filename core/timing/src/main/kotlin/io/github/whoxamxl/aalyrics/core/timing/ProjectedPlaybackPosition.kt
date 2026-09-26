package io.github.whoxamxl.aalyrics.core.timing

import io.github.whoxamxl.aalyrics.core.model.PlaybackSnapshot
import kotlin.math.roundToLong

/** Projects the normalized sample using its source timestamp or AALyrics sample-time fallback. */
fun projectedPlaybackPosition(
    playback: PlaybackSnapshot,
    currentMonotonicTimeMs: Long,
): Long {
    val base = playback.positionMs
    val duration = playback.track?.durationMs
    if (!playback.isPlaying || playback.playbackRate <= 0f) {
        return duration?.let { base.coerceIn(0L, it) } ?: base
    }

    val anchor = playback.positionUpdatedAtMonotonicMs
        ?: playback.positionSampledAtMonotonicMs
    val elapsedMs = anchor
        ?.let { (currentMonotonicTimeMs - it).coerceAtLeast(0L) }
        ?: 0L
    val projected = base + (elapsedMs * playback.playbackRate).toDouble().roundToLong()
    return duration?.let { projected.coerceIn(0L, it) }
        ?: projected.coerceAtLeast(0L)
}
