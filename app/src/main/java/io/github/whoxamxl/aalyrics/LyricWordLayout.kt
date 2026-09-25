package io.github.whoxamxl.aalyrics

import io.github.whoxamxl.aalyrics.core.model.TimedLyricLine
import java.text.BreakIterator
import java.text.Normalizer
import java.util.Locale

/** Adapted from auto-lyrics 8484bed2 LyricWordLayout; canonical text is never reconstructed. */
internal object LyricWordLayout {
    internal data class DisplayRange(val start: Int, val end: Int)
    private data class TokenSpan(val start: Int, val end: Int)

    private val JAPANESE_SCRIPT = Regex("[\\u3040-\\u30ff\\u3400-\\u4dbf\\u4e00-\\u9fff]")
    private val HAN_SCRIPT = Regex("[\\u3400-\\u4dbf\\u4e00-\\u9fff]")
    private val HIRAGANA_ONLY = Regex("[\\u3040-\\u309fー]+")
    internal fun displayRangeForToken(line: TimedLyricLine, activeTokenIndex: Int): DisplayRange? =
        displayRangesForLine(line)?.getOrNull(activeTokenIndex)

    /**
     * Returns readable ranges for every timing token only when the timing payload
     * has enough evidence that it belongs to [TimedLyricLine.text]. This prevents a
     * small number of incidental substring matches from enabling a visual sweep.
     */
    internal fun displayRangesForLine(line: TimedLyricLine): List<DisplayRange>? {
        if (line.words.isEmpty()) return emptyList()
        val ranges = lexicalRanges(line.text)
        if (ranges.isEmpty()) return null

        val tokenSpans = locateTokensBestEffort(line)
        if (!hasCredibleAlignment(line, tokenSpans)) return null

        val result = ArrayList<DisplayRange>(line.words.size)
        line.words.indices.forEach { tokenIndex ->
            result += mapDisplayRange(line, tokenIndex, ranges, tokenSpans) ?: return null
        }
        return result
    }

    private fun mapDisplayRange(
        line: TimedLyricLine,
        activeTokenIndex: Int,
        ranges: List<DisplayRange>,
        tokenSpans: List<TokenSpan?>
    ): DisplayRange? {
        val token = tokenSpans.getOrNull(activeTokenIndex)
        if (token == null) {
            return fallbackDisplayRange(line, activeTokenIndex, ranges, tokenSpans)
        }

        ranges.firstOrNull { range ->
            token.start < range.end && token.end > range.start
        }?.let { return it }

        // Whitespace and punctuation can themselves be timed by some providers.
        // Keep the visible word stable through those tiny bridge tokens by mapping
        // them to the nearest lexical range, preferring the preceding word on ties.
        return ranges.minWithOrNull(
            compareBy<DisplayRange> { distance(token, it) }
                .thenBy { if (it.end <= token.start) 0 else 1 }
        )
    }

    private fun locateTokensBestEffort(line: TimedLyricLine): List<TokenSpan?> {
        val spans = MutableList<TokenSpan?>(line.words.size) { null }
        var cursor = 0

        line.words.forEachIndexed { index, word ->
            val span = findTokenSpan(line.text, word.text, cursor) ?: return@forEachIndexed
            spans[index] = span
            cursor = span.end
        }

        return spans
    }

    private fun findTokenSpan(text: String, token: String, startIndex: Int): TokenSpan? {
        if (token.isEmpty() || startIndex >= text.length) return null

        val exactIndex = text.indexOf(token, startIndex = startIndex)
        val exactSpan = if (exactIndex >= 0) {
            TokenSpan(exactIndex, exactIndex + token.length)
        } else {
            null
        }

        val caseInsensitiveIndex = text.indexOf(token, startIndex = startIndex, ignoreCase = true)
        val caseInsensitiveSpan = if (caseInsensitiveIndex >= 0) {
            TokenSpan(caseInsensitiveIndex, caseInsensitiveIndex + token.length)
        } else {
            null
        }

        val normalizedToken = normalizeForAlignment(token)
        var normalizedSpan: TokenSpan? = null
        for (candidateStart in startIndex until text.length) {
            val maxEnd = minOf(
                text.length,
                candidateStart + token.length + NORMALIZATION_SLACK_CHARS
            )
            for (candidateEnd in (candidateStart + 1)..maxEnd) {
                if (normalizeForAlignment(text.substring(candidateStart, candidateEnd)) == normalizedToken) {
                    normalizedSpan = TokenSpan(candidateStart, candidateEnd)
                    break
                }
            }
            if (normalizedSpan != null) break
        }

        // Sequential karaoke alignment cares about source order first. A later
        // exact-case occurrence must not beat an earlier case/normalization-
        // equivalent occurrence, otherwise repeated words can shift by one token.
        return listOfNotNull(exactSpan, caseInsensitiveSpan, normalizedSpan)
            .minByOrNull { it.start }
    }

    private fun hasCredibleAlignment(line: TimedLyricLine, tokenSpans: List<TokenSpan?>): Boolean {
        if (line.words.isEmpty()) return false

        val sourceCanonical = canonicalContent(line.text)
        val timingCanonical = canonicalContent(line.words.joinToString(separator = "") { it.text })
        if (sourceCanonical.isNotEmpty() && sourceCanonical == timingCanonical) return true
        if (sourceCanonical.isEmpty() || timingCanonical.isEmpty() || line.words.size == 1) return false

        val alignedIndices = line.words.indices.filter { tokenSpans.getOrNull(it) != null }
        if (alignedIndices.size < MIN_PARTIAL_ANCHORS) return false

        // Count substantive canonical characters rather than token hits. This
        // prevents unrelated fine-grained payloads such as [A, X, G, Z] from
        // being accepted merely because one-letter substrings happen to occur in
        // the source. Sequential matching guarantees these covered spans do not
        // overlap or reorder.
        val alignedCanonicalChars = alignedIndices.sumOf { index ->
            canonicalContent(line.words[index].text).length
        }
        if (alignedCanonicalChars == 0) return false

        return alignedCanonicalChars * MIN_COVERAGE_DENOMINATOR >=
            sourceCanonical.length * MIN_COVERAGE_NUMERATOR &&
            alignedCanonicalChars * MIN_COVERAGE_DENOMINATOR >=
            timingCanonical.length * MIN_COVERAGE_NUMERATOR
    }

    private fun normalizeForAlignment(text: String): String =
        Normalizer.normalize(text, Normalizer.Form.NFC).lowercase(Locale.ROOT)

    private fun canonicalContent(text: String): String =
        normalizeForAlignment(text).filter { it.isLetterOrDigit() }

    private fun fallbackDisplayRange(
        line: TimedLyricLine,
        activeTokenIndex: Int,
        ranges: List<DisplayRange>,
        tokenSpans: List<TokenSpan?>
    ): DisplayRange? {
        if (activeTokenIndex !in line.words.indices || ranges.isEmpty()) return null
        if (line.words.size == 1) {
            return DisplayRange(ranges.first().start, ranges.last().end)
        }

        val previousIndex = (activeTokenIndex - 1 downTo 0)
            .firstOrNull { tokenSpans[it] != null }
        val nextIndex = (activeTokenIndex + 1..line.words.lastIndex)
            .firstOrNull { tokenSpans[it] != null }
        val blockStartIndex = (previousIndex ?: -1) + 1
        val blockEndIndex = (nextIndex ?: line.words.size) - 1
        val intervalStart = tokenSpans.getOrNull(previousIndex ?: -1)?.end
            ?: ranges.first().start
        val intervalEnd = tokenSpans.getOrNull(nextIndex ?: tokenSpans.size)?.start
            ?: ranges.last().end

        // Only the unaligned block is estimated. Successfully aligned neighbors
        // remain anchors, so a single case/punctuation/normalization mismatch does
        // not redistribute the surrounding syllable timing across later words.
        val tokenLengths = (blockStartIndex..blockEndIndex).map { index ->
            line.words[index].text.length.coerceAtLeast(1)
        }
        val totalLength = tokenLengths.sum().coerceAtLeast(1)
        val activeOffset = activeTokenIndex - blockStartIndex
        val consumedBefore = tokenLengths.take(activeOffset).sum()
        val tokenMidpoint = consumedBefore + tokenLengths[activeOffset] / 2f
        val fraction = (tokenMidpoint / totalLength).coerceIn(0f, 1f)
        val start = intervalStart.toFloat()
        val end = intervalEnd.coerceAtLeast(intervalStart).toFloat()
        val target = start + (end - start) * fraction

        return ranges.minWithOrNull(
            compareBy<DisplayRange> { pointDistance(target, it) }
                .thenBy { if (it.end.toFloat() <= target) 0 else 1 }
        )
    }

    private fun lexicalRanges(text: String): List<DisplayRange> {
        if (text.isBlank()) return emptyList()

        val locale = if (JAPANESE_SCRIPT.containsMatchIn(text)) Locale.JAPANESE else Locale.ROOT
        val iterator = BreakIterator.getWordInstance(locale)
        iterator.setText(text)

        val rawRanges = ArrayList<DisplayRange>()
        var start = iterator.first()
        var end = iterator.next()
        while (end != BreakIterator.DONE) {
            val segment = text.substring(start, end)
            if (segment.any { it.isLetterOrDigit() } || JAPANESE_SCRIPT.containsMatchIn(segment)) {
                rawRanges += DisplayRange(start, end)
            }
            start = end
            end = iterator.next()
        }

        if (rawRanges.size < 2 || !JAPANESE_SCRIPT.containsMatchIn(text)) return rawRanges

        // Japanese dictionary boundaries can still expose a stem and its okurigana
        // as separate ranges (e.g. 忘 + れない). Merge an adjacent hiragana suffix
        // into a preceding kanji-containing range. This is intentionally a display
        // heuristic only; the original per-token timestamps remain available.
        val merged = ArrayList<DisplayRange>(rawRanges.size)
        for (range in rawRanges) {
            val currentText = text.substring(range.start, range.end)
            val previous = merged.lastOrNull()
            if (previous != null && previous.end == range.start) {
                val previousText = text.substring(previous.start, previous.end)
                if (HAN_SCRIPT.containsMatchIn(previousText) && HIRAGANA_ONLY.matches(currentText)) {
                    merged[merged.lastIndex] = DisplayRange(previous.start, range.end)
                    continue
                }
            }
            merged += range
        }
        return merged
    }

    private fun distance(token: TokenSpan, range: DisplayRange): Int {
        return when {
            token.end <= range.start -> range.start - token.end
            range.end <= token.start -> token.start - range.end
            else -> 0
        }
    }

    private fun pointDistance(point: Float, range: DisplayRange): Float {
        return when {
            point < range.start -> range.start - point
            point > range.end -> point - range.end
            else -> 0f
        }
    }

    private const val NORMALIZATION_SLACK_CHARS = 4
    private const val MIN_PARTIAL_ANCHORS = 2
    private const val MIN_COVERAGE_NUMERATOR = 1
    private const val MIN_COVERAGE_DENOMINATOR = 2
}
