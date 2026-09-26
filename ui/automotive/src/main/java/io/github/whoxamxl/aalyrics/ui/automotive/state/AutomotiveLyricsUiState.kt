package io.github.whoxamxl.aalyrics.ui.automotive.state

import android.graphics.Bitmap
import io.github.whoxamxl.aalyrics.core.lyrics.LyricsState
import io.github.whoxamxl.aalyrics.core.model.PlaybackSnapshot
import io.github.whoxamxl.aalyrics.core.model.PlaybackStatus
import io.github.whoxamxl.aalyrics.core.model.TimedLyricLine
import io.github.whoxamxl.aalyrics.core.timing.LyricsTimingOffset
import io.github.whoxamxl.aalyrics.core.timing.effectiveLyricsPosition
import io.github.whoxamxl.aalyrics.core.timing.projectLyricsTiming
import io.github.whoxamxl.aalyrics.core.timing.projectedPlaybackPosition
import io.github.whoxamxl.aalyrics.ui.automotive.AutomotiveTransportCapabilities

data class AutomotiveLyricsUiState(
    val trackTitle: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val durationMs: Long? = null,
    val positionMs: Long = 0L,
    val playbackStatus: PlaybackStatus = PlaybackStatus.IDLE,
    val playbackRate: Float = 1.0f,
    val artwork: Bitmap? = null,
    val capabilities: AutomotiveTransportCapabilities = AutomotiveTransportCapabilities(),
    val lyrics: AutomotiveLyricPresentation = AutomotiveLyricPresentation(NO_MEDIA_MESSAGE),
) {
    val subtitle: String
        get() = lyrics.primaryText

    val displayTitle: String
        get() = when {
            trackTitle.isNullOrBlank() -> "AALyrics"
            artist.isNullOrBlank() -> trackTitle
            else -> "$trackTitle — $artist"
        }

    companion object {
        const val NO_MEDIA_MESSAGE = "Play a song to see lyrics"
    }
}

data class AutomotiveLyricPresentation(
    val primaryText: String,
    val secondaryText: String? = null,
    val isAnimatedLoading: Boolean = false,
)

internal object AutomotiveLyricsUiStateMapper {
    fun project(
        playback: PlaybackSnapshot,
        lyricsState: LyricsState,
        currentMonotonicTimeMs: Long,
    ): AutomotiveLyricsUiState {
        val track = playback.track
        if (track == null) {
            return AutomotiveLyricsUiState(
                playbackStatus = playback.status,
                playbackRate = playback.playbackRate,
            )
        }

        val positionMs = projectedPlaybackPosition(playback, currentMonotonicTimeMs)
        val matchingState = lyricsState.takeIf { state ->
            state !is LyricsState.ForLookup ||
                state.lookup.playbackIdentity == playback.trackIdentity
        }
        val document = when (matchingState) {
            is LyricsState.Ready -> matchingState.lyrics
            is LyricsState.Degraded -> matchingState.lyrics
            else -> null
        }
        val activeLineIndex = document?.let { lyrics ->
            projectLyricsTiming(
                lyrics,
                effectiveLyricsPosition(positionMs, LyricsTimingOffset.ZERO),
            ).activeLineIndex
        }
        val currentLine = activeLineIndex?.let { document?.lines?.getOrNull(it) as? TimedLyricLine }

        val subtitle = when (matchingState) {
            null -> "Loading lyrics…"
            LyricsState.Idle -> "Waiting for lyrics…"
            is LyricsState.Loading -> "Loading lyrics…"
            is LyricsState.NotFound -> "No lyrics found"
            is LyricsState.Failed -> "Unable to load lyrics"
            is LyricsState.Ready,
            is LyricsState.Degraded,
            -> when {
                currentLine != null -> currentLine.text.ifBlank { "♪" }
                document?.lines?.any { it is TimedLyricLine } == true -> "♪"
                document?.lines?.isNotEmpty() == true -> "Unsynced lyrics"
                else -> "No lyrics found"
            }
        }

        return AutomotiveLyricsUiState(
            trackTitle = track.title,
            artist = track.primaryArtist,
            album = track.album,
            durationMs = track.durationMs,
            positionMs = positionMs,
            playbackStatus = playback.status,
            playbackRate = playback.playbackRate,
            lyrics = AutomotiveLyricPresentation(subtitle),
        )
    }

}
