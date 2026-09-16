package io.github.whoxamxl.aalyrics.provider.petitlyrics

import io.github.whoxamxl.aalyrics.core.model.LyricsAttribution
import io.github.whoxamxl.aalyrics.core.model.LyricsDocument
import io.github.whoxamxl.aalyrics.core.model.LyricsSyncType
import io.github.whoxamxl.aalyrics.core.model.Track
import io.github.whoxamxl.aalyrics.provider.api.*
import okhttp3.OkHttpClient

/** PetitLyrics discovery adapted from auto-lyrics main 8484bed, with normalized output. */
class PetitLyricsProvider(
    private val config: PetitLyricsConfig,
    client: OkHttpClient = PetitLyricsTransport.defaultClient(),
    endpoint: String = "https://on.petitlyrics.com/api/GetPetitLyricsData.php",
    versionCode: Int = 1,
    versionName: String = "0.1.0-dev",
) : LyricsProvider {
    private val transport = PetitLyricsTransport(config, client, endpoint, versionCode, versionName)

    override val descriptor = LyricsProviderDescriptor(
        LyricsProviderId("petitlyrics"), "PetitLyrics", setOf(LyricsSyncType.LINE, LyricsSyncType.WORD),
    )

    val isConfigured: Boolean get() = config.isConfigured

    override suspend fun search(request: LyricsRequest): List<LyricsCandidate> {
        if (!isConfigured) return emptyList()
        // Per-lookup discovery state isolates concurrent requests and their fallback failures.
        val result = PetitLyricsClient(transport).getSyncedLyrics(
            album = request.track.album.orEmpty(),
            artist = request.track.artists.joinToString(", "),
            title = request.track.title,
        ) ?: return emptyList()
        return listOf(LyricsCandidate(
            providerId = descriptor.id,
            matchedTrack = Track(
                title = result.matchedTitle,
                artists = listOfNotNull(result.matchedArtist.takeIf { it.isNotBlank() }),
                album = result.matchedAlbum.takeIf { it.isNotBlank() && it != "-" },
                // PetitLyrics duration units are ambiguous; preserve the fork's exclusion from matching.
                durationMs = null,
            ),
            lyrics = LyricsDocument(result.lines, attribution = LyricsAttribution(
                descriptor.id.value, descriptor.displayName, result.lyricsId,
            )),
            evidence = LyricsCandidateEvidence(artistQueryCorroborated = result.artistQueryCorroborated),
        ))
    }
}
