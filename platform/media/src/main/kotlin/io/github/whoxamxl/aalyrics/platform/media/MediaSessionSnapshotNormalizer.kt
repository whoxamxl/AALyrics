package io.github.whoxamxl.aalyrics.platform.media

import io.github.whoxamxl.aalyrics.core.model.PlaybackSnapshot
import io.github.whoxamxl.aalyrics.core.model.PlaybackSource
import io.github.whoxamxl.aalyrics.core.model.PlaybackStatus
import io.github.whoxamxl.aalyrics.core.model.Track

/** Raw values extracted from Android media APIs before entering the domain. */
internal data class MediaSessionSnapshotInput(
    val sourceId: String,
    val title: String? = null,
    val displayTitle: String? = null,
    val artist: String? = null,
    val albumArtist: String? = null,
    val album: String? = null,
    val durationMs: Long? = null,
    val mediaId: String? = null,
    val mediaUri: String? = null,
    val status: PlaybackStatus = PlaybackStatus.IDLE,
    val positionMs: Long = 0L,
    val playbackRate: Float = 1.0f,
    val positionUpdatedAtMonotonicMs: Long? = null,
    val positionSampledAtMonotonicMs: Long? = null,
)

/**
 * Pure normalization step shared by the Android adapter and JVM unit tests.
 *
 * This layer performs only structural normalization. Semantic title/artist
 * cleaning remains reserved for the separately inventoried metadata-cleaning
 * migration rather than being reimplemented here.
 */
internal object MediaSessionSnapshotNormalizer {
    fun normalize(input: MediaSessionSnapshotInput): PlaybackSnapshot {
        val sourceId = input.sourceId.trim()
        require(sourceId.isNotEmpty()) { "Media session source id must not be blank" }

        val mediaId = input.mediaId.nonBlankOrNull()
        val mediaUri = input.mediaUri.nonBlankOrNull()
        val source = PlaybackSource(
            id = sourceId,
            mediaId = mediaId,
            mediaUri = mediaUri,
        )

        val title = input.title.nonBlankOrNull()
            ?: input.displayTitle.nonBlankOrNull()
        val albumArtist = input.albumArtist.nonBlankOrNull()
        val artist = input.artist.nonBlankOrNull()
            ?: albumArtist
        val album = input.album.nonBlankOrNull()
        val durationMs = input.durationMs?.takeIf { it > 0L }

        val track = title?.let { normalizedTitle ->
            val spotifyReference = SpotifyPlaybackReference.resolve(
                sourceId = sourceId,
                mediaId = mediaId,
                mediaUri = mediaUri,
            )
            Track(
                title = normalizedTitle,
                artists = artist?.let(::listOf).orEmpty(),
                album = album,
                durationMs = durationMs,
                references = setOfNotNull(spotifyReference),
                albumArtist = albumArtist,
            )
        }

        val playbackRate = input.playbackRate.takeIf { rate ->
            rate.isFinite() && rate >= 0f
        } ?: 1.0f

        return PlaybackSnapshot(
            track = track,
            status = input.status,
            positionMs = input.positionMs.coerceAtLeast(0L),
            playbackRate = playbackRate,
            source = source,
            positionUpdatedAtMonotonicMs = input.positionUpdatedAtMonotonicMs
                ?.takeIf { it >= 0L },
            positionSampledAtMonotonicMs = input.positionSampledAtMonotonicMs
                ?.takeIf { it >= 0L },
        )
    }

    private fun String?.nonBlankOrNull(): String? =
        this?.trim()?.takeIf(String::isNotEmpty)
}
