package io.github.whoxamxl.aalyrics.provider.synclrc

import io.github.whoxamxl.aalyrics.core.model.LyricsAttribution
import io.github.whoxamxl.aalyrics.core.model.LyricsDocument
import io.github.whoxamxl.aalyrics.core.model.LyricsSyncType
import io.github.whoxamxl.aalyrics.core.model.Track
import io.github.whoxamxl.aalyrics.provider.api.LyricsCandidate
import io.github.whoxamxl.aalyrics.provider.api.LyricsCandidateEvidence
import io.github.whoxamxl.aalyrics.provider.api.LyricsProvider
import io.github.whoxamxl.aalyrics.provider.api.LyricsProviderDescriptor
import io.github.whoxamxl.aalyrics.provider.api.LyricsProviderId
import io.github.whoxamxl.aalyrics.provider.api.LyricsRequest
import okhttp3.OkHttpClient
import kotlin.math.roundToLong

/** SyncLRC's karaoke-only behavior adapted from auto-lyrics main 8484bed. */
class SyncLrcProvider(
    client: OkHttpClient = SyncLrcTransport.defaultClient(),
    endpoint: String = SyncLrcTransport.DEFAULT_ENDPOINT,
    versionName: String = "0.1.0-dev",
) : LyricsProvider {
    private val providerClient = SyncLrcClient(SyncLrcTransport(client, endpoint, versionName))

    override val descriptor = LyricsProviderDescriptor(
        id = LyricsProviderId("synclrc"),
        displayName = "SyncLRC",
        supportedSyncTypes = setOf(LyricsSyncType.WORD),
    )

    override suspend fun search(request: LyricsRequest): List<LyricsCandidate> {
        if (request.preferredSyncType != LyricsSyncType.WORD) return emptyList()
        val artist = request.track.artists.joinToString(", ")
        if (artist.isBlank()) return emptyList()

        val result = providerClient.getKaraokeLyrics(request.track, artist) ?: return emptyList()
        if (result.matchedTitle.isBlank()) return emptyList()
        return listOf(LyricsCandidate(
            providerId = descriptor.id,
            matchedTrack = Track(
                title = result.matchedTitle,
                artists = listOfNotNull(result.matchedArtist.takeIf(String::isNotBlank)),
                album = result.matchedAlbum.takeIf(String::isNotBlank),
                durationMs = result.matchedDurationSec
                    ?.takeIf { it.isFinite() && it >= 0.0 && it <= Long.MAX_VALUE / 1_000.0 }
                    ?.times(1_000.0)
                    ?.roundToLong(),
            ),
            lyrics = LyricsDocument(
                lines = result.lines,
                attribution = LyricsAttribution(
                    providerId = descriptor.id.value,
                    displayName = descriptor.displayName,
                    sourceId = result.sourceId,
                ),
            ),
            evidence = LyricsCandidateEvidence(
                artistQueryCorroborated = result.artistQueryCorroborated,
            ),
        ))
    }
}
