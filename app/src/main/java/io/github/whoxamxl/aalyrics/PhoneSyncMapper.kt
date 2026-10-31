package io.github.whoxamxl.aalyrics

import io.github.whoxamxl.aalyrics.core.model.LyricsSyncType
import io.github.whoxamxl.aalyrics.core.model.PlaybackSnapshot
import io.github.whoxamxl.aalyrics.core.lyrics.LyricsState
import io.github.whoxamxl.aalyrics.core.timing.LyricsTimingOffset
import io.github.whoxamxl.aalyrics.ui.phone.lyrics.LyricsScreenUiState
import io.github.whoxamxl.aalyrics.ui.phone.lyrics.TrackCardLyricsStatus
import io.github.whoxamxl.aalyrics.ui.phone.sync.SyncAdjustment
import io.github.whoxamxl.aalyrics.ui.phone.sync.SyncAvailability
import io.github.whoxamxl.aalyrics.ui.phone.sync.SyncScreenUiState
import io.github.whoxamxl.aalyrics.ui.phone.sync.TapSyncAvailability
import io.github.whoxamxl.aalyrics.ui.phone.sync.TapSyncMessage
import io.github.whoxamxl.aalyrics.ui.phone.sync.TapSyncUiState

internal fun mapPhoneSyncState(
    lyrics: LyricsScreenUiState,
    offset: LyricsTimingOffset,
    tapSync: TapSyncUiState = TapSyncUiState(),
): SyncScreenUiState {
    val viewport = lyrics.viewport
    val availability = when (lyrics.trackCard.lyricsStatus) {
        TrackCardLyricsStatus.IDLE -> SyncAvailability.NO_MEDIA
        TrackCardLyricsStatus.LOADING -> SyncAvailability.LOADING
        TrackCardLyricsStatus.NOT_FOUND, TrackCardLyricsStatus.FAILED -> SyncAvailability.UNAVAILABLE
        TrackCardLyricsStatus.READY -> when {
            viewport.lines.isEmpty() -> SyncAvailability.UNAVAILABLE
            viewport.syncType == LyricsSyncType.PLAIN -> SyncAvailability.PLAIN
            else -> SyncAvailability.TIMED
        }
    }
    val timed = availability == SyncAvailability.TIMED
    val index = viewport.currentLineIndex
    return SyncScreenUiState(
        offsetMs = offset.milliseconds,
        availability = availability,
        trackTitle = lyrics.trackCard.title.takeUnless { availability == SyncAvailability.NO_MEDIA },
        canAdvance = timed && offset.milliseconds < LyricsTimingSettings.MAX_OFFSET_MS,
        canDelay = timed && offset.milliseconds > -LyricsTimingSettings.MAX_OFFSET_MS,
        previousLine = if (timed && index != null) viewport.lines.getOrNull(index - 1) else null,
        currentLine = if (timed && index != null) viewport.lines.getOrNull(index) else null,
        nextLine = if (timed) viewport.lines.getOrNull(index?.plus(1) ?: 0) else null,
        tapSync = tapSync,
    )
}

internal fun mapPhoneTapSyncState(
    state: TapSyncState,
    playback: PlaybackSnapshot,
    lyrics: LyricsState,
    lyricsUi: LyricsScreenUiState,
    offset: LyricsTimingOffset,
    now: Long,
): TapSyncUiState {
    val listening = state as? TapSyncState.Listening
    val availability = when {
        !playback.isPlaying || playback.playbackRate <= 0f -> TapSyncAvailability.NEEDS_PLAYBACK
        lyricsUi.trackCard.lyricsStatus != TrackCardLyricsStatus.READY ||
            lyricsUi.viewport.syncType == LyricsSyncType.PLAIN -> TapSyncAvailability.NEEDS_TIMED_LYRICS
        listening != null -> TapSyncAvailability.READY
        nextTapSyncTargets(playback, lyrics, offset, now).size < TapSyncCalibration.TAP_COUNT ->
            TapSyncAvailability.NOT_ENOUGH_LINES
        else -> TapSyncAvailability.READY
    }
    return TapSyncUiState(
        availability = availability,
        active = listening != null,
        lines = listening?.targets?.map { it.text }.orEmpty(),
        completedTaps = listening?.completedTaps ?: 0,
        savedOffsetMs = (state as? TapSyncState.Completed)?.offsetMs,
        message = (state as? TapSyncState.Stopped)?.reason?.let {
            when (it) {
                TapSyncStopReason.SOURCE_CHANGED -> TapSyncMessage.SOURCE_CHANGED
                TapSyncStopReason.OUT_OF_RANGE -> TapSyncMessage.OUT_OF_RANGE
            }
        },
    )
}

internal fun SyncAdjustment.deltaMs(): Long = when (this) {
    SyncAdjustment.ADVANCE_FINE -> 50L
    SyncAdjustment.DELAY_FINE -> -50L
    SyncAdjustment.ADVANCE_COARSE -> 500L
    SyncAdjustment.DELAY_COARSE -> -500L
}
