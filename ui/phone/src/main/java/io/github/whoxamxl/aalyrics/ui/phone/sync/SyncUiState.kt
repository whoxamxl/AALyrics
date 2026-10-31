package io.github.whoxamxl.aalyrics.ui.phone.sync

import androidx.compose.runtime.Immutable
import io.github.whoxamxl.aalyrics.ui.phone.lyrics.LyricsViewportLineUiState

enum class SyncAvailability { NO_MEDIA, LOADING, UNAVAILABLE, PLAIN, TIMED }

/** Presentation facts only; application ownership supplies the calibrated current line. */
@Immutable
data class SyncScreenUiState(
    val offsetMs: Long = 0L,
    val availability: SyncAvailability = SyncAvailability.NO_MEDIA,
    val trackTitle: String? = null,
    val canAdvance: Boolean = false,
    val canDelay: Boolean = false,
    val previousLine: LyricsViewportLineUiState? = null,
    val currentLine: LyricsViewportLineUiState? = null,
    val nextLine: LyricsViewportLineUiState? = null,
    val tapSync: TapSyncUiState = TapSyncUiState(),
)

enum class SyncAdjustment { ADVANCE_FINE, DELAY_FINE, ADVANCE_COARSE, DELAY_COARSE }

enum class TapSyncAvailability { READY, NEEDS_PLAYBACK, NEEDS_TIMED_LYRICS, NOT_ENOUGH_LINES }
enum class TapSyncMessage { SOURCE_CHANGED, OUT_OF_RANGE }

@Immutable
data class TapSyncUiState(
    val availability: TapSyncAvailability = TapSyncAvailability.NEEDS_PLAYBACK,
    val active: Boolean = false,
    val lines: List<String> = emptyList(),
    val completedTaps: Int = 0,
    val savedOffsetMs: Long? = null,
    val message: TapSyncMessage? = null,
)
