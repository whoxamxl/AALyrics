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

/**
 * Stable-enough identity for deciding whether playback represents a new item.
 *
 * Position, playback status/rate, and duration are deliberately excluded so a
 * timeline update or late duration metadata cannot restart a lyrics lookup.
 * Identity prefers explicit catalog references, then source media identity,
 * then a metadata fallback when the playback source exposes no stable id.
 */
sealed interface PlaybackTrackIdentity {
    data class Referenced(
        val sourceId: String?,
        val references: Set<TrackReference>,
    ) : PlaybackTrackIdentity {
        init {
            require(references.isNotEmpty()) { "Referenced playback identity requires a track reference" }
        }
    }

    data class SourceMedia(
        val sourceId: String,
        val mediaId: String? = null,
        val mediaUri: String? = null,
    ) : PlaybackTrackIdentity {
        init {
            require(sourceId.isNotBlank()) { "Source media identity requires a source id" }
            require(mediaId != null || mediaUri != null) {
                "Source media identity requires a media id or URI"
            }
        }
    }

    data class Metadata(
        val sourceId: String?,
        val title: String,
        val artists: List<String>,
        val album: String?,
    ) : PlaybackTrackIdentity
}

/**
 * Immutable snapshot consumed by the lyrics domain and presentation layers.
 *
 * [positionUpdatedAtMonotonicMs] is optional because not every playback source
 * supplies a sample timestamp. When present, it is on the source platform's
 * monotonic clock and lets a downstream adapter advance [positionMs] without
 * pretending the snapshot was sampled when AALyrics happened to receive it.
 */
data class PlaybackSnapshot(
    val track: Track? = null,
    val status: PlaybackStatus = PlaybackStatus.IDLE,
    val positionMs: Long = 0L,
    val playbackRate: Float = 1.0f,
    val source: PlaybackSource? = null,
    val positionUpdatedAtMonotonicMs: Long? = null,
) {
    init {
        require(positionMs >= 0L) { "Playback position must not be negative" }
        require(playbackRate.isFinite() && playbackRate >= 0f) {
            "Playback rate must be finite and non-negative"
        }
        require(positionUpdatedAtMonotonicMs == null || positionUpdatedAtMonotonicMs >= 0L) {
            "Playback position update time must be null or non-negative"
        }
    }

    val isPlaying: Boolean
        get() = status == PlaybackStatus.PLAYING

    val trackIdentity: PlaybackTrackIdentity?
        get() {
            val currentTrack = track ?: return null
            val references = currentTrack.references
            if (references.isNotEmpty()) {
                return PlaybackTrackIdentity.Referenced(
                    sourceId = source?.id,
                    references = references.toSet(),
                )
            }

            val currentSource = source
            if (currentSource != null &&
                (currentSource.mediaId != null || currentSource.mediaUri != null)
            ) {
                return PlaybackTrackIdentity.SourceMedia(
                    sourceId = currentSource.id,
                    mediaId = currentSource.mediaId,
                    mediaUri = currentSource.mediaUri,
                )
            }

            return PlaybackTrackIdentity.Metadata(
                sourceId = currentSource?.id,
                title = currentTrack.title,
                artists = currentTrack.artists.toList(),
                album = currentTrack.album,
            )
        }
}
