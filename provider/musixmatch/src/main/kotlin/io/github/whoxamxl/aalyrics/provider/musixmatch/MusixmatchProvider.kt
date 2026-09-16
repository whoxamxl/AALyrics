package io.github.whoxamxl.aalyrics.provider.musixmatch

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

class MusixmatchProvider(
    client: OkHttpClient = MusixmatchTransport.defaultClient(),
    baseUrl: String = MusixmatchTransport.DEFAULT_BASE_URL,
    nowMs: () -> Long = System::currentTimeMillis,
) : LyricsProvider {
    private val providerClient = MusixmatchClient(
        transport = MusixmatchTransport(client, baseUrl, nowMs),
        nowMs = nowMs,
    )

    override val descriptor = LyricsProviderDescriptor(
        id = LyricsProviderId("musixmatch"),
        displayName = "Musixmatch",
        supportedSyncTypes = setOf(LyricsSyncType.LINE, LyricsSyncType.WORD),
    )

    override suspend fun search(request: LyricsRequest): List<LyricsCandidate> {
        val result = providerClient.getLyrics(request.track) ?: return emptyList()
        if (result.lines.none { it.text.isNotBlank() && it.text != "♪" }) return emptyList()
        val candidate = result.candidate
        return listOf(
            LyricsCandidate(
                providerId = descriptor.id,
                matchedTrack = Track(
                    title = candidate.title,
                    artists = listOfNotNull(candidate.artist.takeIf(String::isNotBlank)),
                    album = candidate.album.takeIf { it.isNotBlank() && it != "-" },
                    durationMs = candidate.durationSec
                        ?.times(1000.0)
                        ?.roundToLong()
                        ?.takeIf { it >= 0L },
                ),
                lyrics = LyricsDocument(
                    lines = result.lines,
                    attribution = LyricsAttribution(
                        providerId = descriptor.id.value,
                        displayName = descriptor.displayName,
                        sourceId = (candidate.trackId ?: candidate.commonTrackId)?.toString(),
                    ),
                ),
                evidence = LyricsCandidateEvidence(
                    artistQueryCorroborated = result.artistQueryCorroborated,
                ),
            ),
        )
    }
}
