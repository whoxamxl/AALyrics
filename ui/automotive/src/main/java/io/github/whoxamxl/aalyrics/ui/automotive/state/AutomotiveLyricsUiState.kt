package io.github.whoxamxl.aalyrics.ui.automotive.state

import io.github.whoxamxl.aalyrics.core.lyrics.LyricsState
import io.github.whoxamxl.aalyrics.core.model.LyricLine
import io.github.whoxamxl.aalyrics.core.model.PlaybackSnapshot
import io.github.whoxamxl.aalyrics.core.model.PlaybackStatus
import io.github.whoxamxl.aalyrics.core.model.TimedLyricLine
import io.github.whoxamxl.aalyrics.core.model.Track
import kotlin.math.roundToLong

data class AutomotiveLyricsUiState(
    val trackTitle: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val durationMs: Long? = null,
    val positionMs: Long = 0L,
    val playbackStatus: PlaybackStatus = PlaybackStatus.IDLE,
    val playbackRate: Float = 1.0f,
    val subtitle: String = NO_MEDIA_MESSAGE,
) {
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

internal object AutomotiveLyricsUiStateMapper {
    fun project(
        playback: PlaybackSnapshot,
        lyricsState: LyricsState,
        elapsedSincePlaybackSnapshotMs: Long = 0L,
    ): AutomotiveLyricsUiState {
        val track = playback.track
        if (track == null) {
            return AutomotiveLyricsUiState(
                playbackStatus = playback.status,
                playbackRate = playback.playbackRate,
            )
        }

        val positionMs = projectedPosition(playback, elapsedSincePlaybackSnapshotMs)
        val matchingState = lyricsState.takeIf { it.belongsTo(track) }
        val document = when (matchingState) {
            is LyricsState.Ready -> matchingState.lyrics
            is LyricsState.Degraded -> matchingState.lyrics
            else -> null
        }
        val currentLine = document?.lines?.currentTimedLine(positionMs)

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
            subtitle = subtitle,
        )
    }

    private fun projectedPosition(playback: PlaybackSnapshot, elapsedMs: Long): Long {
        val base = playback.positionMs
        if (!playback.isPlaying || elapsedMs <= 0L || playback.playbackRate <= 0f) {
            return clampToDuration(base, playback.track)
        }
        val advanced = base + (elapsedMs.coerceAtLeast(0L) * playback.playbackRate)
            .toDouble()
            .roundToLong()
        return clampToDuration(advanced.coerceAtLeast(base), playback.track)
    }

    private fun clampToDuration(positionMs: Long, track: Track?): Long =
        track?.durationMs?.let { positionMs.coerceIn(0L, it) } ?: positionMs.coerceAtLeast(0L)

    private fun LyricsState.belongsTo(track: Track): Boolean = when (this) {
        LyricsState.Idle -> true
        is LyricsState.ForLookup -> lookup.track.samePresentationIdentity(track)
    }

    private fun Track.samePresentationIdentity(other: Track): Boolean =
        title == other.title && artists == other.artists && album == other.album

    private fun List<LyricLine>.currentTimedLine(positionMs: Long): TimedLyricLine? =
        asSequence()
            .filterIsInstance<TimedLyricLine>()
            .takeWhile { it.startMs <= positionMs }
            .lastOrNull()
}
