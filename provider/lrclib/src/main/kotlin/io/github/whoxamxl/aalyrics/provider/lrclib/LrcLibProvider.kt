package io.github.whoxamxl.aalyrics.provider.lrclib

import com.google.gson.annotations.SerializedName
import io.github.whoxamxl.aalyrics.core.model.*
import io.github.whoxamxl.aalyrics.provider.api.*
import io.github.whoxamxl.aalyrics.provider.lrc.LrcParser
import io.github.whoxamxl.aalyrics.provider.matching.MetadataMatching.artistSimilarity
import io.github.whoxamxl.aalyrics.provider.matching.MetadataMatching.durationSimilaritySeconds
import io.github.whoxamxl.aalyrics.provider.matching.MetadataMatching.stringSimilarity
import io.github.whoxamxl.aalyrics.provider.matching.MetadataMatching.stripFeaturing
import io.github.whoxamxl.aalyrics.provider.matching.MetadataMatching.versionsCompatible
import okhttp3.OkHttpClient

/** LRCLIB's mature local discovery policy, adapted from auto-lyrics main 8484bed. */
class LrcLibProvider(
    client: OkHttpClient = LrcLibTransport.defaultClient(),
    baseUrl: String = "https://lrclib.net/api/",
) : LyricsProvider {
    private val transport = LrcLibTransport(client, baseUrl)

    override val descriptor = LyricsProviderDescriptor(
        LyricsProviderId("lrclib"), "LRCLIB", setOf(LyricsSyncType.PLAIN, LyricsSyncType.LINE),
    )

    override suspend fun search(request: LyricsRequest): List<LyricsCandidate> {
        val track = request.track
        // The working fork queries and validates against whole playback seconds.
        val durationSec = ((track.durationMs ?: 0) / 1_000).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        val result = getLyrics(track.title, track.artists.joinToString(", "), track.album.orEmpty(), durationSec)
            ?: return emptyList()
        val lines = syncedLines(result).takeIf { it.any(::hasRealText) }
            ?: result.plainLyrics.orEmpty().lines().filter { it.isNotBlank() }.map(::PlainLyricLine)
        val matchedTrack = Track(
            title = result.trackName!!, // Local title validation excludes missing/blank titles.
            artists = listOfNotNull(result.artistName?.takeIf { it.isNotBlank() }),
            album = result.albumName?.takeIf { it.isNotBlank() && it != "-" },
            durationMs = result.duration?.takeIf { it.isFinite() && it > 0 && it < Long.MAX_VALUE / 1_000.0 }
                ?.let { (it * 1_000).toLong() },
        )
        return listOf(LyricsCandidate(
            providerId = descriptor.id,
            matchedTrack = matchedTrack,
            lyrics = LyricsDocument(lines, attribution = LyricsAttribution(
                providerId = descriptor.id.value, displayName = descriptor.displayName, sourceId = result.id?.toString(),
            )),
        ))
    }

    private fun syncedLines(candidate: LrcLibResponse): List<TimedLyricLine> =
        candidate.syncedLyrics?.let(LrcParser::parse).orEmpty()

    private fun hasRealText(line: LyricLine): Boolean = line.text.isNotBlank() && line.text != "♪"

    private fun hasUsableSyncedLyrics(candidate: LrcLibResponse): Boolean = syncedLines(candidate).any(::hasRealText)

    private fun hasUsablePlainLyrics(candidate: LrcLibResponse): Boolean =
        candidate.plainLyrics.orEmpty().lines().any { it.isNotBlank() }

    private fun titleSimilarity(left: String, right: String): Double = maxOf(
        stringSimilarity(left, right), stringSimilarity(stripFeaturing(left), stripFeaturing(right)),
    )

    private suspend fun getExact(track: String, artist: String, album: String, durationSec: Int): LrcLibResponse? =
        transport.getExact(track, artist, album, durationSec)

    private suspend fun searchAll(trackName: String, artistName: String?, albumName: String?): List<LrcLibResponse> =
        transport.searchAll(trackName, artistName, albumName)

    private suspend fun searchFreeText(query: String): List<LrcLibResponse> = transport.searchFreeText(query)

    private companion object {
        const val MIN_MATCH_SCORE = 0.70
        const val MIN_TITLE_SCORE = 0.60
        const val MIN_ARTIST_SCORE = 0.40
        const val EARLY_EXACT_SCORE = 0.95
    }

    internal data class LrcLibResponse(
        @SerializedName("id") val id: Int?,
        @SerializedName("trackName") val trackName: String?,
        @SerializedName("artistName") val artistName: String?,
        @SerializedName("albumName") val albumName: String?,
        @SerializedName("duration") val duration: Double?,
        @SerializedName("instrumental") val instrumental: Boolean?,
        @SerializedName("plainLyrics") val plainLyrics: String?,
        @SerializedName("syncedLyrics") val syncedLyrics: String?
    )

    private data class SearchQuery(
        val trackName: String,
        val artistName: String?,
        val albumName: String?
    )

    private data class ScoredCandidate(
        val response: LrcLibResponse,
        val score: Double
    )

    private suspend fun getLyrics(
        trackName: String,
        artistName: String,
        albumName: String,
        durationSec: Int
    ): LrcLibResponse? {
        val candidates = LinkedHashMap<String, LrcLibResponse>()

        fun addCandidate(candidate: LrcLibResponse?) {
            if (candidate == null) return
            val key = candidate.id?.let { "id:$it" }
                ?: listOf(
                    candidate.trackName.orEmpty(),
                    candidate.artistName.orEmpty(),
                    candidate.albumName.orEmpty(),
                    candidate.duration?.toString().orEmpty()
                ).joinToString("|")
            candidates.putIfAbsent(key, candidate)
        }

        fun addCandidates(results: List<LrcLibResponse>) {
            results.forEach(::addCandidate)
        }

        // A strong /api/get result is already constrained by all available
        // metadata. Fast-path it when our local validation also considers it an
        // excellent synchronized match, avoiding several unnecessary searches.
        if (durationSec > 0 && albumName.isNotBlank()) {
            val exact = getExact(trackName, artistName, albumName, durationSec)
            addCandidate(exact)
            if (exact != null && hasUsableSyncedLyrics(exact)) {
                val exactScore = scoreCandidate(
                    exact,
                    trackName,
                    artistName,
                    albumName,
                    durationSec
                )
                if (exactScore != null && exactScore >= EARLY_EXACT_SCORE) {
                    return exact
                }
            }
        }

        val structuredQueries = linkedSetOf<SearchQuery>()
        structuredQueries += SearchQuery(
            trackName = trackName,
            artistName = artistName.takeIf { it.isNotBlank() },
            albumName = albumName.takeIf { it.isNotBlank() }
        )
        if (albumName.isNotBlank()) {
            structuredQueries += SearchQuery(
                trackName = trackName,
                artistName = artistName.takeIf { it.isNotBlank() },
                albumName = null
            )
        }

        val strippedTitle = stripFeaturing(trackName)
        val primaryArtist = stripFeaturing(artistName)
        if (strippedTitle != trackName || primaryArtist != artistName) {
            structuredQueries += SearchQuery(
                trackName = strippedTitle,
                artistName = primaryArtist.takeIf { it.isNotBlank() },
                albumName = null
            )
        }

        structuredQueries.forEach { query ->
            addCandidates(searchAll(query.trackName, query.artistName, query.albumName))
        }

        selectBest(
            candidates.values,
            trackName,
            artistName,
            albumName,
            durationSec,
            requireSynced = true
        )?.let { return it }

        // Some media sessions expose a contributor list instead of the performing
        // artist (for example composer, lyricist, vocalist and studio/brand names
        // concatenated with commas). If artist-constrained FTS could not find a
        // usable synchronized result, broaden discovery by title only and let the
        // local matcher validate title, contributor components, duration and album.
        addCandidates(searchAll(strippedTitle, artistName = null, albumName = null))

        selectBest(
            candidates.values,
            trackName,
            artistName,
            albumName,
            durationSec,
            requireSynced = true
        )?.let { return it }

        // A free-text fallback helps when field-specific FTS matching is defeated
        // by collaboration/feature metadata differences. It is only used when no
        // acceptable synchronized result was found by the structured searches.
        val freeText = listOf(trackName, artistName)
            .filter { it.isNotBlank() }
            .joinToString(" ")
        if (freeText.isNotBlank()) {
            addCandidates(searchFreeText(freeText))
        }

        selectBest(
            candidates.values,
            trackName,
            artistName,
            albumName,
            durationSec,
            requireSynced = true
        )?.let { return it }

        // Plain lyrics are a last resort. They use the same metadata validation,
        // so a weak first search result cannot win simply because it was first.
        return selectBest(
            candidates.values,
            trackName,
            artistName,
            albumName,
            durationSec,
            requireSynced = false
        )
    }

    private fun selectBest(
        candidates: Collection<LrcLibResponse>,
        trackName: String,
        artistName: String,
        albumName: String,
        durationSec: Int,
        requireSynced: Boolean
    ): LrcLibResponse? {
        return candidates.asSequence()
            .filter { candidate ->
                if (requireSynced) {
                    hasUsableSyncedLyrics(candidate)
                } else {
                    hasUsablePlainLyrics(candidate)
                }
            }
            .mapNotNull { candidate ->
                scoreCandidate(candidate, trackName, artistName, albumName, durationSec)
                    ?.let { score -> ScoredCandidate(candidate, score) }
            }
            .filter { it.score >= MIN_MATCH_SCORE }
            .maxByOrNull { it.score }
            ?.response
    }

    private fun scoreCandidate(
        candidate: LrcLibResponse,
        trackName: String,
        artistName: String,
        albumName: String,
        durationSec: Int
    ): Double? {
        val candidateTitle = candidate.trackName.orEmpty()
        val candidateArtist = candidate.artistName.orEmpty()
        val candidateAlbum = candidate.albumName.orEmpty()

        if (!versionsCompatible(
            requestedTitle = trackName,
            candidateTitle = candidateTitle,
            candidateInstrumental = candidate.instrumental == true,
            requestedAlbum = albumName,
            candidateAlbum = candidateAlbum
        )) {
            return null
        }

        val titleScore = titleSimilarity(trackName, candidateTitle)
        if (titleScore < MIN_TITLE_SCORE) return null

        val artistScore = if (artistName.isBlank() || candidateArtist.isBlank()) {
            null
        } else {
            artistSimilarity(
                left = artistName,
                right = candidateArtist,
                allowContributorComponents = titleScore >= 0.95
            )
        }

        val durationScore = durationSimilaritySeconds(durationSec, candidate.duration)
        if (durationScore != null && durationScore < 0.0) return null

        // Reject a clearly different artist unless title + duration are both
        // exceptionally strong. Exact contributor-component matches are admitted
        // only for near-exact titles by artistSimilarity().
        if (artistScore != null && artistScore < MIN_ARTIST_SCORE) {
            val strongTitleAndDuration = titleScore >= 0.95 && (durationScore ?: 0.0) >= 0.85
            if (!strongTitleAndDuration) return null
        }

        var weightedScore = titleScore * 0.55
        var totalWeight = 0.55

        if (artistScore != null) {
            weightedScore += artistScore * 0.30
            totalWeight += 0.30
        }

        if (durationScore != null) {
            weightedScore += durationScore * 0.12
            totalWeight += 0.12
        }

        if (albumName.isNotBlank() && candidateAlbum.isNotBlank() && candidateAlbum != "-") {
            weightedScore += stringSimilarity(albumName, candidateAlbum) * 0.03
            totalWeight += 0.03
        }

        return weightedScore / totalWeight
    }

}
