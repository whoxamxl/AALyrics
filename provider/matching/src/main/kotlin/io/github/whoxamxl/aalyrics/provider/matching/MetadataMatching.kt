package io.github.whoxamxl.aalyrics.provider.matching

import java.text.Normalizer
import java.util.Locale
import kotlin.math.abs
import kotlin.math.min

object MetadataMatching {
    private const val MAX_DURATION_DELTA_MS = 15_000L
    private const val CONTRIBUTOR_COMPONENT_SCORE = 0.95

    private val MULTI_SPACE = Regex("""\s+""")
    private val NON_WORD = Regex("""[^\p{L}\p{N}]+""")
    private val CONTRIBUTOR_SEPARATOR = Regex("""\s*[,;]\s*""")
    private val FEAT_SUFFIX = Regex(
        """\s*[\(\[]?\s*(?:feat(?:uring)?|ft)\.?\s+.+?[\)\]]?\s*$""",
        RegexOption.IGNORE_CASE,
    )

    private data class VersionQualifier(
        val name: String,
        val englishPattern: Regex,
        val japaneseMarkers: List<String> = emptyList(),
    )

    private val VERSION_QUALIFIERS = listOf(
        VersionQualifier("live", Regex("""\blive\b""", RegexOption.IGNORE_CASE), listOf("ライブ")),
        VersionQualifier("acoustic", Regex("""\bacoustic\b""", RegexOption.IGNORE_CASE), listOf("アコースティック")),
        VersionQualifier("remix", Regex("""\bremix(?:ed)?\b""", RegexOption.IGNORE_CASE), listOf("リミックス")),
        VersionQualifier("remaster", Regex("""\bremaster(?:ed)?\b""", RegexOption.IGNORE_CASE), listOf("リマスター")),
        VersionQualifier("instrumental", Regex("""\binstrumental\b""", RegexOption.IGNORE_CASE), listOf("インストゥルメンタル", "インスト")),
        VersionQualifier("edit", Regex("""\bedit(?:ed)?\b""", RegexOption.IGNORE_CASE), listOf("エディット")),
        VersionQualifier("extended", Regex("""\bextended\b""", RegexOption.IGNORE_CASE), listOf("エクステンデッド")),
        VersionQualifier("demo", Regex("""\bdemo\b""", RegexOption.IGNORE_CASE), listOf("デモ")),
    )

    fun versionsCompatible(
        requestedTitle: String,
        candidateTitle: String,
        requestedAlbum: String = "",
        candidateAlbum: String = "",
        candidateInstrumental: Boolean = false,
    ): Boolean {
        val requestedTitleVersions = RecordingVersionContext.extractTitleVersionQualifiers(requestedTitle)
        val candidateTitleVersions = RecordingVersionContext.extractTitleVersionQualifiers(candidateTitle) +
            if (candidateInstrumental) setOf("instrumental") else emptySet()
        if (requestedTitleVersions == candidateTitleVersions) return true

        if (requestedTitleVersions.isNotEmpty() && candidateTitleVersions.isNotEmpty()) {
            return false
        }

        val requestedContext = requestedTitleVersions +
            RecordingVersionContext.extractAlbumVersionQualifiers(requestedAlbum)
        val candidateContext = candidateTitleVersions +
            RecordingVersionContext.extractAlbumVersionQualifiers(candidateAlbum)
        return requestedContext.isNotEmpty() && requestedContext == candidateContext
    }

    fun extractVersionQualifiers(value: String): Set<String> {
        val normalized = Normalizer.normalize(value, Normalizer.Form.NFKC)
            .lowercase(Locale.ROOT)
        val result = linkedSetOf<String>()

        for (qualifier in VERSION_QUALIFIERS) {
            if (
                qualifier.englishPattern.containsMatchIn(normalized) ||
                qualifier.japaneseMarkers.any { normalized.contains(it) }
            ) {
                result += qualifier.name
            }
        }
        return result
    }

    fun durationSimilarity(requestedMs: Long?, candidateMs: Long?): Double? {
        if (requestedMs == null || requestedMs <= 0L || candidateMs == null || candidateMs <= 0L) {
            return null
        }

        val delta = abs(candidateMs - requestedMs)
        return durationDeltaScore(delta.toDouble())
    }

    /** Provider-native seconds, retaining fractional precision at threshold boundaries. */
    fun durationSimilaritySeconds(requestedSec: Int, candidateSec: Double?): Double? {
        if (requestedSec <= 0 || candidateSec == null || !candidateSec.isFinite() || candidateSec <= 0) {
            return null
        }
        return durationDeltaScore(abs(candidateSec - requestedSec) * 1_000)
    }

    private fun durationDeltaScore(delta: Double): Double {
        if (delta > MAX_DURATION_DELTA_MS) return -1.0

        return when {
            delta <= 2_000L -> 1.00
            delta <= 4_000L -> 0.92
            delta <= 7_000L -> 0.78
            delta <= 10_000L -> 0.60
            else -> 0.35
        }
    }

    fun artistSimilarity(
        left: String,
        right: String,
        allowContributorComponents: Boolean = false,
    ): Double {
        val direct = maxOf(
            stringSimilarity(left, right),
            stringSimilarity(stripFeaturing(left), stripFeaturing(right)),
        )
        if (!allowContributorComponents || direct >= CONTRIBUTOR_COMPONENT_SCORE) {
            return direct
        }

        val leftVariants = artistVariants(left)
        val rightVariants = artistVariants(right)
        if (leftVariants.size <= 1 && rightVariants.size <= 1) return direct

        val exactComponentMatch = leftVariants.any { leftVariant ->
            rightVariants.any { rightVariant ->
                stringSimilarity(leftVariant, rightVariant) >= 0.999
            }
        }

        return if (exactComponentMatch) {
            maxOf(direct, CONTRIBUTOR_COMPONENT_SCORE)
        } else {
            direct
        }
    }

    fun stringSimilarity(left: String, right: String): Double {
        val a = normalizeForMatch(left)
        val b = normalizeForMatch(right)
        if (a.isBlank() || b.isBlank()) return 0.0
        if (a == b) return 1.0

        val compactA = a.replace(" ", "")
        val compactB = b.replace(" ", "")
        if (compactA == compactB) return 1.0

        val dice = bigramDice(compactA, compactB)
        val token = tokenJaccard(a, b)
        val containment = if (compactA.contains(compactB) || compactB.contains(compactA)) {
            min(compactA.length, compactB.length).toDouble() /
                maxOf(compactA.length, compactB.length).toDouble()
        } else {
            0.0
        }

        return maxOf(
            dice,
            (dice * 0.72) + (token * 0.28),
            containment * 0.92,
        ).coerceIn(0.0, 1.0)
    }

    private fun artistVariants(value: String): List<String> {
        val trimmed = value.trim()
        if (trimmed.isBlank()) return emptyList()

        val variants = linkedSetOf<String>()
        fun addVariant(candidate: String) {
            val normalized = candidate.trim()
            if (normalized.isBlank()) return
            variants += normalized
            val withoutFeaturing = stripFeaturing(normalized)
            if (withoutFeaturing.isNotBlank()) variants += withoutFeaturing
        }

        addVariant(trimmed)
        val components = trimmed.split(CONTRIBUTOR_SEPARATOR)
            .map { it.trim() }
            .filter { it.isNotBlank() }
        if (components.size >= 2) components.forEach(::addVariant)

        return variants.toList()
    }

    private fun tokenJaccard(left: String, right: String): Double {
        val a = left.split(' ').filter { it.isNotBlank() }.toSet()
        val b = right.split(' ').filter { it.isNotBlank() }.toSet()
        if (a.isEmpty() || b.isEmpty()) return 0.0
        val union = a union b
        if (union.isEmpty()) return 0.0
        return (a intersect b).size.toDouble() / union.size.toDouble()
    }

    private fun bigramDice(left: String, right: String): Double {
        if (left == right) return 1.0
        if (left.length < 2 || right.length < 2) return 0.0

        val leftCounts = HashMap<String, Int>()
        for (index in 0 until left.length - 1) {
            val gram = left.substring(index, index + 2)
            leftCounts[gram] = (leftCounts[gram] ?: 0) + 1
        }

        val rightCounts = HashMap<String, Int>()
        for (index in 0 until right.length - 1) {
            val gram = right.substring(index, index + 2)
            rightCounts[gram] = (rightCounts[gram] ?: 0) + 1
        }

        var overlap = 0
        for ((gram, leftCount) in leftCounts) {
            val rightCount = rightCounts[gram] ?: continue
            overlap += min(leftCount, rightCount)
        }

        return (2.0 * overlap) /
            ((left.length - 1) + (right.length - 1)).toDouble()
    }

    private fun normalizeForMatch(value: String): String {
        return Normalizer.normalize(value, Normalizer.Form.NFKC)
            .lowercase(Locale.ROOT)
            .replace('’', '\'')
            .replace('‘', '\'')
            .replace('`', '\'')
            .replace('&', ' ')
            .replace(NON_WORD, " ")
            .replace(MULTI_SPACE, " ")
            .trim()
    }

    fun stripFeaturing(value: String): String {
        return value.replace(FEAT_SUFFIX, "").trim().ifBlank { value.trim() }
    }
}
