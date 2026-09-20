package io.github.whoxamxl.aalyrics.ui.phone.state

import androidx.compose.runtime.Immutable
import io.github.whoxamxl.aalyrics.ui.phone.navigation.PhoneDestination

/** Presentation-only state for persistent Phone shell chrome. */
@Immutable
data class PhoneShellUiState(
    val selectedDestination: PhoneDestination = PhoneDestination.Home,
    val mediaSourceLabel: String? = null,
    val playbackSurface: PlaybackSurfaceUiState? = null,
)

/** Presentation-ready queue item. Platform queue objects stay outside the Phone UI. */
@Immutable
data class PlaybackQueueItemUiState(
    val id: Long,
    val title: String,
    val subtitle: String? = null,
)

/**
 * Presentation-only state for the collapsed Playback Bar and Expanded Player.
 *
 * Artwork stays caller-owned so this model remains independent from bitmap/image-loader types.
 */
@Immutable
data class PlaybackSurfaceUiState(
    val isPlaying: Boolean,
    val title: String,
    val artist: String? = null,
    val playbackIdentityKey: String = title + "\u0000" + artist.orEmpty(),
    val positionMs: Long = 0L,
    val durationMs: Long? = null,
    val playbackRate: Float = 1.0f,
    val positionUpdatedAtMonotonicMs: Long? = null,
    val canPlay: Boolean = true,
    val canPause: Boolean = true,
    val canSkipPrevious: Boolean = true,
    val canSkipNext: Boolean = true,
    val canSkipToQueueItem: Boolean = false,
    val canSeek: Boolean = false,
    val queue: List<PlaybackQueueItemUiState> = emptyList(),
    val canOpenPlaybackApp: Boolean = false,
    val translationEnabled: Boolean = false,
) {
    init {
        require(title.isNotBlank()) { "Playback title must not be blank" }
        require(artist == null || artist.isNotBlank()) { "Playback artist must be null or non-blank" }
        require(playbackIdentityKey.isNotBlank()) { "Playback identity key must not be blank" }
        require(positionMs >= 0L) { "Playback position must not be negative" }
        require(durationMs == null || durationMs > 0L) {
            "Playback duration must be null or positive"
        }
        require(playbackRate.isFinite() && playbackRate >= 0f) {
            "Playback rate must be finite and non-negative"
        }
        require(positionUpdatedAtMonotonicMs == null || positionUpdatedAtMonotonicMs >= 0L) {
            "Playback position update time must be null or non-negative"
        }
    }

    val playPauseEnabled: Boolean
        get() = if (isPlaying) canPause else canPlay

    val queueAvailable: Boolean
        get() = canSkipToQueueItem && queue.isNotEmpty()

    val seekEnabled: Boolean
        get() = canSeek && durationMs != null

    val progressFraction: Float?
        get() = durationMs?.let { duration ->
            (positionMs.toDouble() / duration.toDouble())
                .coerceIn(0.0, 1.0)
                .toFloat()
        }
}
