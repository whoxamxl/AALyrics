package io.github.whoxamxl.aalyrics.core.model

/** Domain-level playback state independent from Android framework classes. */
enum class PlaybackStatus {
    IDLE,
    PLAYING,
    PAUSED,
    BUFFERING,
    STOPPED,
}

/**
 * Identifies the application or service currently supplying playback.
 *
 * Android adapters may use a package name as [id], but the core model does not
 * depend on Android terminology.
 */
data class PlaybackSource(
    val id: String,
    val mediaId: String? = null,
    val mediaUri: String? = null,
) {
    init {
        require(id.isNotBlank()) { "Playback source id must not be blank" }
        require(mediaId == null || mediaId.isNotBlank()) { "Media id must be null or non-blank" }
        require(mediaUri == null || mediaUri.isNotBlank()) { "Media URI must be null or non-blank" }
    }
}

/** Immutable snapshot consumed by the lyrics domain and presentation layers. */
data class PlaybackSnapshot(
    val track: Track? = null,
    val status: PlaybackStatus = PlaybackStatus.IDLE,
    val positionMs: Long = 0L,
    val playbackRate: Float = 1.0f,
    val source: PlaybackSource? = null,
) {
    init {
        require(positionMs >= 0L) { "Playback position must not be negative" }
        require(playbackRate.isFinite() && playbackRate >= 0f) {
            "Playback rate must be finite and non-negative"
        }
    }

    val isPlaying: Boolean
        get() = status == PlaybackStatus.PLAYING
}
