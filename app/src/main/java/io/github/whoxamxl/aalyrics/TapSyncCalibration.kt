package io.github.whoxamxl.aalyrics

import io.github.whoxamxl.aalyrics.core.lyrics.LyricsLookupId
import io.github.whoxamxl.aalyrics.core.lyrics.LyricsState
import io.github.whoxamxl.aalyrics.core.model.LyricsDocument
import io.github.whoxamxl.aalyrics.core.model.PlaybackSnapshot
import io.github.whoxamxl.aalyrics.core.model.TimedLyricLine
import io.github.whoxamxl.aalyrics.core.timing.LyricsTimingOffset
import io.github.whoxamxl.aalyrics.core.timing.effectiveLyricsPosition
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs
import kotlin.math.roundToLong

internal data class TapSyncTarget(val lineIndex: Int, val startMs: Long, val text: String)

internal enum class TapSyncStopReason { SOURCE_CHANGED, OUT_OF_RANGE }

internal sealed interface TapSyncState {
    data object Idle : TapSyncState
    data class Listening(val targets: List<TapSyncTarget>, val completedTaps: Int) : TapSyncState
    data class Completed(val offsetMs: Long) : TapSyncState
    data class Stopped(val reason: TapSyncStopReason) : TapSyncState
}

/** One transient three-tap session. All calls are serialized on the application's main thread. */
internal class TapSyncCalibration(
    private val settings: LyricsTimingSettings,
    private val readPlayback: () -> PlaybackSnapshot,
    private val readLyrics: () -> LyricsState,
    private val monotonicTimeMs: () -> Long,
) {
    private val mutableState = MutableStateFlow<TapSyncState>(TapSyncState.Idle)
    val state: StateFlow<TapSyncState> = mutableState.asStateFlow()
    private var session: Session? = null

    fun start() {
        cancel()
        val playback = readPlayback()
        val lyrics = readLyrics()
        val resolved = currentTapSyncLyrics(playback, lyrics) ?: return
        val targets = nextTapSyncTargets(playback, lyrics, settings.offset.value, monotonicTimeMs())
        if (targets.size != TAP_COUNT) return
        session = Session(resolved, targets, settings.offset.value, playback)
        mutableState.value = TapSyncState.Listening(targets, completedTaps = 0)
    }

    fun tap() {
        // Read the actual click time and latest source snapshot, never the Phone UI's 250 ms tick.
        val now = monotonicTimeMs()
        val playback = readPlayback()
        val active = validatedSession(playback, readLyrics(), now) ?: return
        if (active.lastTapAtMs?.let { now - it < MIN_TAP_INTERVAL_MS } == true) return

        val target = active.targets[active.samples.size]
        val rawPosition = projectedPlaybackPosition(playback, now)
        // Absolute replacement: effectivePosition = rawPosition + offset = lyric start.
        val offset = target.startMs - rawPosition
        if (offset !in -LyricsTimingSettings.MAX_OFFSET_MS..LyricsTimingSettings.MAX_OFFSET_MS) {
            stop(TapSyncStopReason.OUT_OF_RANGE)
            return
        }
        active.samples.add(offset)
        active.lastTapAtMs = now
        if (active.samples.size == TAP_COUNT) {
            val average = active.samples.average().roundToLong()
            session = null
            settings.setOffset(average)
            mutableState.value = TapSyncState.Completed(average)
        } else {
            mutableState.value = TapSyncState.Listening(active.targets, active.samples.size)
        }
    }

    fun onSourceChanged() {
        if (session != null) validatedSession(readPlayback(), readLyrics(), monotonicTimeMs())
    }

    fun cancel() {
        session = null
        mutableState.value = TapSyncState.Idle
    }

    private fun validatedSession(
        playback: PlaybackSnapshot,
        lyrics: LyricsState,
        now: Long,
    ): Session? {
        val active = session ?: return null
        val previous = active.lastPlayback
        val sourceChanged = !playback.isPlaying || playback.playbackRate <= 0f ||
            playback.trackIdentity != previous.trackIdentity ||
            playback.playbackRate != previous.playbackRate ||
            currentTapSyncLyrics(playback, lyrics) != active.lyrics ||
            settings.offset.value != active.initialOffset
        // Ordinary source clock corrections are tolerated; a seek must not mix two timelines.
        val jumped = abs(
            projectedPlaybackPosition(playback, now) - projectedPlaybackPosition(previous, now),
        ) > TIMELINE_JUMP_TOLERANCE_MS
        if (sourceChanged || jumped) {
            stop(TapSyncStopReason.SOURCE_CHANGED)
            return null
        }
        active.lastPlayback = playback
        return active
    }

    private fun stop(reason: TapSyncStopReason) {
        session = null
        mutableState.value = TapSyncState.Stopped(reason)
    }

    private data class Session(
        val lyrics: TapSyncLyrics,
        val targets: List<TapSyncTarget>,
        val initialOffset: LyricsTimingOffset,
        var lastPlayback: PlaybackSnapshot,
        val samples: MutableList<Long> = mutableListOf(),
        var lastTapAtMs: Long? = null,
    )

    companion object {
        const val TAP_COUNT = 3
        private const val MIN_TAP_INTERVAL_MS = 250L
        private const val TIMELINE_JUMP_TOLERANCE_MS = 1_000L
    }
}

/** Freeze distinct, nonblank upcoming lines at Start, so clock ticks cannot move the tap target. */
internal fun nextTapSyncTargets(
    playback: PlaybackSnapshot,
    lyrics: LyricsState,
    offset: LyricsTimingOffset,
    now: Long,
): List<TapSyncTarget> {
    if (!playback.isPlaying || playback.playbackRate <= 0f) return emptyList()
    val document = currentTapSyncLyrics(playback, lyrics)?.document ?: return emptyList()
    val position = effectiveLyricsPosition(projectedPlaybackPosition(playback, now), offset).milliseconds
    return document.lines.mapIndexedNotNull { index, line ->
        (line as? TimedLyricLine)?.takeIf { it.text.isNotBlank() && it.startMs > position }
            ?.let { TapSyncTarget(index, it.startMs, it.text) }
    }.sortedBy { it.startMs }.distinctBy { it.startMs }.take(TapSyncCalibration.TAP_COUNT)
}

private data class TapSyncLyrics(val lookupId: LyricsLookupId, val document: LyricsDocument)

private fun currentTapSyncLyrics(playback: PlaybackSnapshot, state: LyricsState): TapSyncLyrics? {
    if (playback.trackIdentity == null || (state as? LyricsState.ForLookup)?.lookup?.playbackIdentity != playback.trackIdentity) {
        return null
    }
    return when (state) {
        is LyricsState.Ready -> TapSyncLyrics(state.lookup.id, state.lyrics)
        is LyricsState.Degraded -> TapSyncLyrics(state.lookup.id, state.lyrics)
        else -> null
    }
}
