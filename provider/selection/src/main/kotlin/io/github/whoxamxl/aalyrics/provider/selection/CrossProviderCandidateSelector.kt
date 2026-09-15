package io.github.whoxamxl.aalyrics.provider.selection

import io.github.whoxamxl.aalyrics.core.lyrics.CandidateSelectionPreferences
import io.github.whoxamxl.aalyrics.core.lyrics.CandidateSelector
import io.github.whoxamxl.aalyrics.core.model.LyricLine
import io.github.whoxamxl.aalyrics.core.model.LyricsSyncType
import io.github.whoxamxl.aalyrics.core.model.TimedLyricLine
import io.github.whoxamxl.aalyrics.core.model.Track
import io.github.whoxamxl.aalyrics.provider.api.LyricsCandidate
import java.util.Locale
import kotlin.math.abs

/**
 * Production cross-provider selection policy adapted from the mature resolver in
 * the working Auto Lyrics fork.
 *
 * Provider-specific source preferences live here rather than in `:core:lyrics`.
 * The core coordinator continues to know only the [CandidateSelector] port.
 */
class CrossProviderCandidateSelector : CandidateSelector {
    override fun select(
        track: Track,
        candidates: List<LyricsCandidate>,
        preferences: CandidateSelectionPreferences,
    ): LyricsCandidate? {
        val scored = scoreCandidates(track, candidates)
        val synced = scored.filter { it.candidate.lyrics.syncType != LyricsSyncType.PLAIN }
        val standardBest = best(scored = synced)

        if (standardBest != null) {
            if (preferences.preferredSyncType == LyricsSyncType.WORD) {
                val karaokeBest = best(
                    scored = synced.filter { score ->
                        hasUsableWordTiming(score.candidate) &&
                            score.metadataScore >= standardBest.metadataScore - KARAOKE_METADATA_TOLERANCE &&
                            score.qualityScore >= standardBest.qualityScore - KARAOKE_QUALITY_TOLERANCE
                    },
                )
                if (karaokeBest != null) return karaokeBest.candidate
            }
            return standardBest.candidate
        }

        return best(
            scored = scored.filter { it.candidate.lyrics.syncType == LyricsSyncType.PLAIN },
        )?.candidate
    }

    internal fun scoreCandidates(
        track: Track,
        candidates: Collection<LyricsCandidate>,
    ): List<CandidateScore> {
        return candidates.mapNotNull { candidate ->
            if (!hasUsableLyricContent(candidate)) return@mapNotNull null

            val metadata = metadataScore(track, candidate) ?: return@mapNotNull null
            if (metadata < MIN_METADATA_SCORE) return@mapNotNull null

            val quality = lyricsQualityScore(candidate, track.durationMs)
            val confidence = sourceConfidence(track, candidate)
            val finalScore = (
                metadata * METADATA_WEIGHT +
                    quality * QUALITY_WEIGHT +
                    confidence * SOURCE_WEIGHT
                ).coerceIn(0.0, 1.0)

            CandidateScore(
                candidate = candidate,
                metadataScore = metadata,
                qualityScore = quality,
                sourceConfidence = confidence,
                finalScore = finalScore,
            )
        }
    }

    internal fun metadataScore(track: Track, candidate: LyricsCandidate): Double? {
        val candidateTrack = candidate.matchedTrack
        val requestedAlbum = track.album.orEmpty()
        val candidateAlbum = candidateTrack.album.orEmpty()

        if (
            !MetadataMatching.versionsCompatible(
                requestedTitle = track.title,
                candidateTitle = candidateTrack.title,
                requestedAlbum = requestedAlbum,
                candidateAlbum = candidateAlbum,
            )
        ) {
            return null
        }

        val titleScore = MetadataMatching.stringSimilarity(track.title, candidateTrack.title)
        if (titleScore < MIN_TITLE_SCORE) return null

        val durationScore = MetadataMatching.durationSimilarity(
            requestedMs = track.durationMs,
            candidateMs = candidateTrack.durationMs,
        )
        if (durationScore != null && durationScore < 0.0) return null

        val albumScore = if (
            requestedAlbum.isNotBlank() &&
            candidateAlbum.isNotBlank() &&
            candidateAlbum != "-"
        ) {
            val raw = MetadataMatching.stringSimilarity(requestedAlbum, candidateAlbum)
            if (raw < 0.20 && scriptsClearlyDifferent(requestedAlbum, candidateAlbum)) null else raw
        } else {
            null
        }

        val requestedArtist = track.artists.joinToString(", ")
        val candidateArtist = candidateTrack.artists.joinToString(", ")
        val rawArtistScore = if (requestedArtist.isNotBlank() && candidateArtist.isNotBlank()) {
            MetadataMatching.artistSimilarity(
                left = requestedArtist,
                right = candidateArtist,
                allowContributorComponents = titleScore >= 0.95,
            )
        } else {
            null
        }

        val crossScriptArtist = rawArtistScore != null &&
            rawArtistScore < MIN_ARTIST_SCORE &&
            titleScore >= 0.95 &&
            scriptsClearlyDifferent(requestedArtist, candidateArtist)
        val secondaryEvidence =
            candidate.evidence.artistQueryCorroborated ||
                (albumScore != null && albumScore >= CROSS_SCRIPT_ALBUM_EVIDENCE) ||
                (durationScore != null && durationScore >= 0.85)

        val artistScore = if (crossScriptArtist && secondaryEvidence) null else rawArtistScore

        if (artistScore != null && artistScore < MIN_ARTIST_SCORE) {
            val strongTitleAndDuration = titleScore >= 0.95 && (durationScore ?: 0.0) >= 0.85
            if (!strongTitleAndDuration) return null
        }

        var weighted = titleScore * 0.55
        var totalWeight = 0.55

        if (artistScore != null) {
            weighted += artistScore * 0.30
            totalWeight += 0.30
        }
        if (durationScore != null) {
            weighted += durationScore * 0.12
            totalWeight += 0.12
        }
        if (albumScore != null) {
            weighted += albumScore * 0.03
            totalWeight += 0.03
        }

        return (weighted / totalWeight).coerceIn(0.0, 1.0)
    }

    internal fun lyricsQualityScore(
        candidate: LyricsCandidate,
        trackDurationMs: Long?,
    ): Double {
        val useful = candidate.lyrics.lines
            .filter { it.text.isNotBlank() && it.text.trim() != "♪" }
        if (useful.isEmpty()) return 0.0

        var quality = 1.0
        val classes = useful.map { line ->
            val text = line.text.trim()
            val japanese = JAPANESE_SCRIPT.containsMatchIn(text)
            val latin = LATIN_SCRIPT.containsMatchIn(text)
            when {
                japanese -> ScriptClass.JAPANESE
                latin -> ScriptClass.LATIN_ONLY
                else -> ScriptClass.OTHER
            }
        }

        val japaneseCount = classes.count { it == ScriptClass.JAPANESE }
        val latinOnlyCount = classes.count { it == ScriptClass.LATIN_ONLY }
        val linguisticCount = japaneseCount + latinOnlyCount

        if (japaneseCount >= 3 && latinOnlyCount >= 2 && linguisticCount >= 6) {
            val latinRatio = latinOnlyCount.toDouble() / linguisticCount.toDouble()
            var alternatingPairs = 0
            var classifiedPairs = 0
            var nearDuplicateTimestampPairs = 0

            for (index in 1 until classes.size) {
                val previous = classes[index - 1]
                val current = classes[index]
                if (previous == ScriptClass.OTHER || current == ScriptClass.OTHER) continue
                classifiedPairs++
                if (previous != current) {
                    alternatingPairs++
                    val previousTime = useful[index - 1].startTimeMsOrNull()
                    val currentTime = useful[index].startTimeMsOrNull()
                    if (
                        previousTime != null &&
                        currentTime != null &&
                        abs(currentTime - previousTime) <= 750L
                    ) {
                        nearDuplicateTimestampPairs++
                    }
                }
            }

            val alternatingRatio = if (classifiedPairs > 0) {
                alternatingPairs.toDouble() / classifiedPairs.toDouble()
            } else {
                0.0
            }
            val duplicateTimestampRatio = if (alternatingPairs > 0) {
                nearDuplicateTimestampPairs.toDouble() / alternatingPairs.toDouble()
            } else {
                0.0
            }

            if (
                latinRatio >= 0.18 &&
                (alternatingRatio >= 0.25 || nearDuplicateTimestampPairs >= 2)
            ) {
                val ratioStrength = ((latinRatio - 0.18) / 0.32).coerceIn(0.0, 1.0)
                val alternatingStrength = ((alternatingRatio - 0.25) / 0.55).coerceIn(0.0, 1.0)
                val duplicateStrength = duplicateTimestampRatio.coerceIn(0.0, 1.0)
                val contamination =
                    ratioStrength * 0.50 +
                        alternatingStrength * 0.30 +
                        duplicateStrength * 0.20
                quality -= 0.52 * contamination
            }
        }

        if (
            candidate.lyrics.syncType != LyricsSyncType.PLAIN &&
            trackDurationMs != null &&
            trackDurationMs > 0L
        ) {
            val timed = useful.filterIsInstance<TimedLyricLine>().sortedBy { it.startMs }
            val first = timed.firstOrNull()?.startMs
            val last = timed.lastOrNull()?.startMs

            if (last != null && last > trackDurationMs + 20_000L) quality -= 0.25
            if (last != null && timed.size >= 10 && last < trackDurationMs * 0.50) quality -= 0.15
            if (first != null && first > 75_000L) quality -= 0.08
        }

        return quality.coerceIn(0.0, 1.0)
    }

    internal fun sourceConfidence(track: Track, candidate: LyricsCandidate): Double {
        val japaneseTrack = containsJapanese(track.title) ||
            track.artists.any(::containsJapanese) ||
            track.album?.let(::containsJapanese) == true
        val provider = candidate.providerId.value
        val syncType = candidate.lyrics.syncType

        return if (japaneseTrack) {
            when {
                provider.equals(PETITLYRICS_PROVIDER_ID, ignoreCase = true) && syncType == LyricsSyncType.WORD -> 1.00
                provider.equals(PETITLYRICS_PROVIDER_ID, ignoreCase = true) && syncType == LyricsSyncType.LINE -> 0.96
                provider.equals(LRCLIB_PROVIDER_ID, ignoreCase = true) && syncType != LyricsSyncType.PLAIN -> 0.78
                syncType == LyricsSyncType.PLAIN -> 0.55
                else -> 0.75
            }
        } else {
            when {
                provider.equals(LRCLIB_PROVIDER_ID, ignoreCase = true) && syncType != LyricsSyncType.PLAIN -> 1.00
                provider.equals(PETITLYRICS_PROVIDER_ID, ignoreCase = true) && syncType == LyricsSyncType.WORD -> 0.92
                provider.equals(PETITLYRICS_PROVIDER_ID, ignoreCase = true) && syncType == LyricsSyncType.LINE -> 0.88
                syncType == LyricsSyncType.PLAIN -> 0.55
                else -> 0.80
            }
        }
    }

    private fun hasUsableLyricContent(candidate: LyricsCandidate): Boolean {
        return candidate.lyrics.lines.any { line ->
            line.text.isNotBlank() && line.text.trim() != "♪"
        }
    }

    private fun hasUsableWordTiming(candidate: LyricsCandidate): Boolean {
        return candidate.lyrics.syncType == LyricsSyncType.WORD &&
            candidate.lyrics.lines.any { it is TimedLyricLine && it.words.isNotEmpty() }
    }

    private fun best(scored: Collection<CandidateScore>): CandidateScore? {
        return scored.maxWithOrNull(
            compareBy<CandidateScore> { it.finalScore }
                .thenBy { it.metadataScore }
                .thenBy { it.qualityScore }
                .thenBy { it.sourceConfidence }
                .thenBy { stableCandidateKey(it.candidate) },
        )
    }

    private fun stableCandidateKey(candidate: LyricsCandidate): String {
        return buildString {
            append(candidate.providerId.value.lowercase(Locale.ROOT))
            append('|')
            append(candidate.matchedTrack.title.lowercase(Locale.ROOT))
            append('|')
            append(candidate.matchedTrack.artists.joinToString(",").lowercase(Locale.ROOT))
            append('|')
            append(candidate.matchedTrack.album.orEmpty().lowercase(Locale.ROOT))
            append('|')
            append(candidate.matchedTrack.durationMs ?: -1L)
            append('|')
            append(candidate.lyrics.attribution?.sourceId.orEmpty().lowercase(Locale.ROOT))
        }
    }

    private fun scriptsClearlyDifferent(left: String, right: String): Boolean {
        if (left.isBlank() || right.isBlank()) return false
        val leftJapanese = containsJapanese(left)
        val rightJapanese = containsJapanese(right)
        val leftLatin = LATIN_SCRIPT.containsMatchIn(left)
        val rightLatin = LATIN_SCRIPT.containsMatchIn(right)

        return (leftJapanese && !leftLatin && rightLatin && !rightJapanese) ||
            (rightJapanese && !rightLatin && leftLatin && !leftJapanese)
    }

    private fun containsJapanese(value: String): Boolean = JAPANESE_SCRIPT.containsMatchIn(value)

    internal data class CandidateScore(
        val candidate: LyricsCandidate,
        val metadataScore: Double,
        val qualityScore: Double,
        val sourceConfidence: Double,
        val finalScore: Double,
    )

    private enum class ScriptClass {
        JAPANESE,
        LATIN_ONLY,
        OTHER,
    }

    private companion object {
        const val MIN_METADATA_SCORE = 0.70
        const val MIN_TITLE_SCORE = 0.60
        const val MIN_ARTIST_SCORE = 0.40
        const val CROSS_SCRIPT_ALBUM_EVIDENCE = 0.65

        const val METADATA_WEIGHT = 0.82
        const val QUALITY_WEIGHT = 0.10
        const val SOURCE_WEIGHT = 0.08

        const val KARAOKE_METADATA_TOLERANCE = 0.03
        const val KARAOKE_QUALITY_TOLERANCE = 0.05

        const val LRCLIB_PROVIDER_ID = "lrclib"
        const val PETITLYRICS_PROVIDER_ID = "petitlyrics"

        val JAPANESE_SCRIPT = Regex("[\\u3040-\\u30ff\\u3400-\\u4dbf\\u4e00-\\u9fff]")
        val LATIN_SCRIPT = Regex("[A-Za-z]")
    }
}

private fun LyricLine.startTimeMsOrNull(): Long? = (this as? TimedLyricLine)?.startMs
