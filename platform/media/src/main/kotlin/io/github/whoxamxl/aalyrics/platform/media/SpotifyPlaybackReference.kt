package io.github.whoxamxl.aalyrics.platform.media

import io.github.whoxamxl.aalyrics.core.model.TrackReference

/**
 * Converts explicit Spotify track resources from Spotify playback metadata into
 * a provider-independent track reference.
 *
 * A bare 22-character media id is intentionally not treated as a Spotify track
 * id because its resource type is ambiguous.
 */
internal object SpotifyPlaybackReference {
    const val SOURCE_ID = "com.spotify.music"
    const val NAMESPACE = "spotify"

    private val trackUri = Regex(
        pattern = "^spotify:track:([A-Za-z0-9]{22})$",
        option = RegexOption.IGNORE_CASE,
    )
    private val trackUrl = Regex(
        pattern = "^https?://open\\.spotify\\.com/track/([A-Za-z0-9]{22})(?:[/?#].*)?$",
        option = RegexOption.IGNORE_CASE,
    )

    fun resolve(
        sourceId: String,
        mediaId: String?,
        mediaUri: String?,
    ): TrackReference? {
        if (sourceId != SOURCE_ID) return null

        val id = parseExplicitTrackResource(mediaUri)
            ?: parseExplicitTrackResource(mediaId)
            ?: return null
        return TrackReference(namespace = NAMESPACE, value = id)
    }

    private fun parseExplicitTrackResource(value: String?): String? {
        val candidate = value?.trim().orEmpty()
        if (candidate.isEmpty()) return null

        return trackUri.matchEntire(candidate)?.groupValues?.get(1)
            ?: trackUrl.matchEntire(candidate)?.groupValues?.get(1)
    }
}
