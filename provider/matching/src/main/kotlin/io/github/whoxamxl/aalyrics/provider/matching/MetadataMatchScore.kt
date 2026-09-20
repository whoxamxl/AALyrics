package io.github.whoxamxl.aalyrics.provider.matching

/** Shared metadata plausibility only; payload quality, source confidence and winner selection stay outside. */
object MetadataMatchScore {
    fun score(
        requestedTitle: String,
        candidateTitle: String,
        requestedArtist: String = "",
        candidateArtist: String = "",
        requestedAlbum: String = "",
        candidateAlbum: String = "",
        requestedDurationMs: Long? = null,
        candidateDurationMs: Long? = null,
        artistQueryCorroborated: Boolean = false,
    ): Double? {
        if (
            !MetadataMatching.versionsCompatible(
                requestedTitle = requestedTitle,
                candidateTitle = candidateTitle,
                requestedAlbum = requestedAlbum,
                candidateAlbum = candidateAlbum,
            )
        ) {
            return null
        }

        val titleScore = MetadataMatching.stringSimilarity(requestedTitle, candidateTitle)
        if (titleScore < MIN_TITLE_SCORE) return null

        val durationScore = MetadataMatching.durationSimilarity(
            requestedMs = requestedDurationMs,
            candidateMs = candidateDurationMs,
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
            artistQueryCorroborated ||
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

    private const val MIN_TITLE_SCORE = 0.60
    private const val MIN_ARTIST_SCORE = 0.40
    private const val CROSS_SCRIPT_ALBUM_EVIDENCE = 0.65
    private val JAPANESE_SCRIPT = Regex("[\\u3040-\\u30ff\\u3400-\\u4dbf\\u4e00-\\u9fff]")
    private val LATIN_SCRIPT = Regex("[A-Za-z]")
}
