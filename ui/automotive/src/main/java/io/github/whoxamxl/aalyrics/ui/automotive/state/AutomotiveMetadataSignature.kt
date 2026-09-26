package io.github.whoxamxl.aalyrics.ui.automotive.state

import android.graphics.Bitmap

/** Only facts serialized into MediaSession metadata belong in this invalidation key. */
internal data class AutomotiveMetadataSignature<T>(
    val trackTitle: String?,
    val artist: String?,
    val album: String?,
    val durationMs: Long?,
    val displayTitle: String,
    val lyricPrimary: String,
    val lyricSecondary: String?,
    val artwork: T?,
)

internal fun AutomotiveLyricsUiState.metadataSignature(): AutomotiveMetadataSignature<Bitmap> =
    AutomotiveMetadataSignature(
        trackTitle = trackTitle,
        artist = artist,
        album = album,
        durationMs = durationMs,
        displayTitle = displayTitle,
        lyricPrimary = lyrics.primaryText,
        lyricSecondary = lyrics.secondaryText,
        artwork = artwork,
    )
