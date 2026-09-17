package io.github.whoxamxl.aalyrics.provider.synclrc

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import io.github.whoxamxl.aalyrics.core.model.TimedLyricLine
import io.github.whoxamxl.aalyrics.core.model.Track
import io.github.whoxamxl.aalyrics.provider.lrc.LrcParser

internal class SyncLrcClient(
    private val transport: SyncLrcTransport,
) {
    private val gson = Gson()

    suspend fun getKaraokeLyrics(track: Track, artist: String): SyncLrcResult? =
        parseApiResponse(transport.get(track, artist), track, artist)

    internal fun parseApiResponse(
        json: String,
        requestedTrack: Track? = null,
        requestedArtist: String = requestedTrack?.artists?.joinToString(", ").orEmpty(),
    ): SyncLrcResult? {
        if (json.isBlank()) return null
        val response = try {
            gson.fromJson(json, ApiResponse::class.java)
        } catch (_: Exception) {
            return null
        } ?: return null

        if (response.instrumental) return null
        val karaokeLyrics = response.karaoke
            ?.takeIf(String::isNotBlank)
            ?: response.lyrics?.takeIf {
                it.isNotBlank() && response.type.equals("karaoke", ignoreCase = true)
            }
            ?: return null

        val lines = LrcParser.parseKaraoke(karaokeLyrics)
        if (!hasUsableKaraoke(lines)) return null

        return SyncLrcResult(
            lines = lines,
            matchedTitle = response.track.orEmpty().ifBlank { requestedTrack?.title.orEmpty() },
            matchedArtist = response.artist.orEmpty().ifBlank { requestedArtist },
            matchedAlbum = response.album.orEmpty().ifBlank { requestedTrack?.album.orEmpty() },
            matchedDurationSec = response.duration,
            sourceId = response.id?.takeIf(String::isNotBlank),
            artistQueryCorroborated = requestedArtist.isNotBlank(),
        )
    }

    private fun hasUsableKaraoke(lines: List<TimedLyricLine>): Boolean {
        fun String.hasRealText(): Boolean = isNotBlank() && trim() != "♪"
        return lines.any { it.text.hasRealText() } &&
            lines.any { line -> line.words.any { it.text.hasRealText() } }
    }

    internal data class SyncLrcResult(
        val lines: List<TimedLyricLine>,
        val matchedTitle: String,
        val matchedArtist: String,
        val matchedAlbum: String,
        val matchedDurationSec: Double?,
        val sourceId: String?,
        val artistQueryCorroborated: Boolean,
    )

    internal data class ApiResponse(
        @SerializedName("karaoke") val karaoke: String? = null,
        @SerializedName("synced") val synced: String? = null,
        @SerializedName("plain") val plain: String? = null,
        @SerializedName("lyrics") val lyrics: String? = null,
        @SerializedName("type") val type: String? = null,
        @SerializedName("id") val id: String? = null,
        @SerializedName("track") val track: String? = null,
        @SerializedName("artist") val artist: String? = null,
        @SerializedName("album") val album: String? = null,
        @SerializedName("duration") val duration: Double? = null,
        @SerializedName("instrumental") val instrumental: Boolean = false,
    )
}
