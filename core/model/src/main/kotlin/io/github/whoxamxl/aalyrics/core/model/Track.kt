package io.github.whoxamxl.aalyrics.core.model

/**
 * Provider-independent metadata for one playable track.
 *
 * All durations in the domain model are expressed in milliseconds. Unknown
 * values are represented with null rather than sentinel values such as 0.
 */
data class Track(
    val title: String,
    val artists: List<String> = emptyList(),
    val album: String? = null,
    val durationMs: Long? = null,
    val references: Set<TrackReference> = emptySet(),
) {
    init {
        require(title.isNotBlank()) { "Track title must not be blank" }
        require(artists.all { it.isNotBlank() }) { "Artist names must not be blank" }
        require(album == null || album.isNotBlank()) { "Album must be null or non-blank" }
        require(durationMs == null || durationMs >= 0L) { "Track duration must not be negative" }
    }

    val primaryArtist: String?
        get() = artists.firstOrNull()
}

/**
 * Stable identity supplied by a playback source or external catalog.
 *
 * The namespace deliberately remains platform-neutral. Examples may later be
 * "spotify", "musicbrainz", or another catalog understood by an adapter.
 */
data class TrackReference(
    val namespace: String,
    val value: String,
) {
    init {
        require(namespace.isNotBlank()) { "Track reference namespace must not be blank" }
        require(value.isNotBlank()) { "Track reference value must not be blank" }
    }
}
