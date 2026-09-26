package io.github.whoxamxl.aalyrics.ui.automotive.state

import android.graphics.Bitmap
import io.github.whoxamxl.aalyrics.core.lyrics.LyricsState
import io.github.whoxamxl.aalyrics.core.model.LyricsSyncType
import io.github.whoxamxl.aalyrics.core.model.PlaybackSnapshot
import io.github.whoxamxl.aalyrics.core.model.PlaybackStatus
import io.github.whoxamxl.aalyrics.core.model.PlaybackTrackIdentity
import io.github.whoxamxl.aalyrics.core.model.TimedLyricLine
import io.github.whoxamxl.aalyrics.core.timing.LyricsTimingOffset
import io.github.whoxamxl.aalyrics.core.timing.effectiveLyricsPosition
import io.github.whoxamxl.aalyrics.core.timing.projectLyricsTiming
import io.github.whoxamxl.aalyrics.core.timing.projectedPlaybackPosition
import io.github.whoxamxl.aalyrics.ui.automotive.AutomotiveTransportCapabilities
import io.github.whoxamxl.aalyrics.ui.automotive.AutomotiveArtworkState
import io.github.whoxamxl.aalyrics.translation.api.TranslationLanguages
import io.github.whoxamxl.aalyrics.translation.api.TranslationSettings
import io.github.whoxamxl.aalyrics.translation.core.CanonicalLyricsIdentity
import io.github.whoxamxl.aalyrics.translation.core.TranslationState

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

internal fun shouldRenderProjectionTick(
    playback: PlaybackSnapshot,
    isAnimatedLoading: Boolean,
): Boolean = playback.isPlaying || isAnimatedLoading

internal fun <T> artworkForTrack(
    playbackIdentity: PlaybackTrackIdentity?,
    artworkIdentity: PlaybackTrackIdentity?,
    artwork: T?,
): T? = artwork.takeIf { playbackIdentity != null && playbackIdentity == artworkIdentity }

internal object AutomotiveLyricsUiStateMapper {
    fun project(
        playback: PlaybackSnapshot,
        lyricsState: LyricsState,
        currentMonotonicTimeMs: Long,
        artwork: AutomotiveArtworkState = AutomotiveArtworkState(),
        capabilities: AutomotiveTransportCapabilities = AutomotiveTransportCapabilities(),
        translationSettings: TranslationSettings = TranslationSettings(enabled = false),
        translationState: TranslationState = TranslationState.Idle,
        canonicalLyricsIdentity: CanonicalLyricsIdentity? = null,
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

        val lyricsPresentation = when (matchingState) {
            null,
            is LyricsState.Loading,
            -> AutomotiveLyricPresentation(
                primaryText = "Loading lyrics${loadingDots(currentMonotonicTimeMs)}",
                isAnimatedLoading = true,
            )
            LyricsState.Idle -> AutomotiveLyricPresentation("Waiting for lyrics…")
            is LyricsState.NotFound -> AutomotiveLyricPresentation("No synced lyrics found")
            is LyricsState.Failed -> AutomotiveLyricPresentation("Unable to load lyrics")
            is LyricsState.Ready,
            is LyricsState.Degraded,
            -> AutomotiveLyricPresentation(when {
                document?.syncType == LyricsSyncType.PLAIN -> "Synced lyrics unavailable"
                currentLine != null -> currentLine.text.ifBlank { "♪" }
                else -> "♪"
            })
        }
        val presentation = if (
            currentLine != null && currentLine.text.isNotBlank() &&
            document?.syncType != LyricsSyncType.PLAIN
        ) {
            lyricsPresentation.withTranslation(
                lineIndex = requireNotNull(activeLineIndex),
                lineCount = document.lines.size,
                settings = translationSettings,
                state = translationState,
                canonicalIdentity = canonicalLyricsIdentity,
                currentMonotonicTimeMs = currentMonotonicTimeMs,
            )
        } else lyricsPresentation

        return AutomotiveLyricsUiState(
            trackTitle = track.title,
            artist = track.primaryArtist,
            album = track.album,
            durationMs = track.durationMs,
            positionMs = positionMs,
            playbackStatus = playback.status,
            playbackRate = playback.playbackRate,
            artwork = artworkForTrack(
                playback.trackIdentity,
                artwork.trackIdentity,
                artwork.bitmap,
            ),
            capabilities = capabilities,
            lyrics = presentation,
        )
    }

    internal fun loadingDots(currentMonotonicTimeMs: Long): String =
        ".".repeat(((currentMonotonicTimeMs.coerceAtLeast(0L) / 250L) % 3L).toInt() + 1)

    private fun AutomotiveLyricPresentation.withTranslation(
        lineIndex: Int,
        lineCount: Int,
        settings: TranslationSettings,
        state: TranslationState,
        canonicalIdentity: CanonicalLyricsIdentity?,
        currentMonotonicTimeMs: Long,
    ): AutomotiveLyricPresentation {
        if (!settings.enabled || canonicalIdentity == null) return this
        val target = TranslationLanguages.normalizeTargetLanguage(settings.targetLanguage)
        return when (state) {
            is TranslationState.Translating -> if (
                state.request.canonicalLyrics == canonicalIdentity &&
                state.request.targetLanguage == target
            ) {
                copy(
                    secondaryText = "Translating${loadingDots(currentMonotonicTimeMs)}",
                    isAnimatedLoading = true,
                )
            } else this
            is TranslationState.Ready -> {
                val artifact = state.artifact
                val translated = artifact.takeIf {
                    it.request.canonicalLyrics == canonicalIdentity &&
                        it.request.targetLanguage == target &&
                        it.lines.size == lineCount
                }?.lines?.getOrNull(lineIndex)
                    ?.takeIf { it.translated && it.text.isNotBlank() }
                if (translated == null) this else copy(secondaryText = translated.text)
            }
            TranslationState.Disabled,
            TranslationState.Idle,
            is TranslationState.NotRequired,
            is TranslationState.Failed,
            -> this
        }
    }
}
