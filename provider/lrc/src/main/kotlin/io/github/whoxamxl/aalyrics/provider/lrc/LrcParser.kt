package io.github.whoxamxl.aalyrics.provider.lrc

import io.github.whoxamxl.aalyrics.core.model.TimedLyricLine
import io.github.whoxamxl.aalyrics.core.model.TimedWord

object LrcParser {

    private val LINE_TIMESTAMP = Regex("""\[(\d{1,3}):(\d{2})[.:](\d{2,3})]""")
    private val WORD_TIMESTAMP = Regex("""<(\d{1,3}):(\d{2})[.:](\d{2,3})>""")

    fun parse(lrc: String): List<TimedLyricLine> {
        return lrc.lines()
            .flatMap { line -> parseLine(line) }
            .sortedBy { it.startMs }
    }

    fun parseKaraoke(lrc: String): List<TimedLyricLine> {
        return lrc.lines()
            .flatMap { line -> parseKaraokeLine(line) }
            .sortedBy { it.startMs }
    }

    private fun parseLine(line: String): List<TimedLyricLine> {
        val timestamps = mutableListOf<Long>()
        var remaining = line.trim()

        while (remaining.startsWith("[")) {
            val match = LINE_TIMESTAMP.find(remaining) ?: break
            if (match.range.first != 0) break
            timestamps.add(parseTimestamp(match))
            remaining = remaining.substring(match.range.last + 1)
        }

        if (timestamps.isEmpty()) return emptyList()

        val text = remaining.trim()
        return timestamps.map { ts ->
            TimedLyricLine(startMs = ts, text = text.ifBlank { "♪" })
        }
    }

    private fun parseKaraokeLine(line: String): List<TimedLyricLine> {
        val timestamps = mutableListOf<Long>()
        var remaining = line.trim()

        while (remaining.startsWith("[")) {
            val match = LINE_TIMESTAMP.find(remaining) ?: break
            if (match.range.first != 0) break
            timestamps.add(parseTimestamp(match))
            remaining = remaining.substring(match.range.last + 1)
        }

        if (timestamps.isEmpty()) return emptyList()

        // Some Enhanced-LRC producers insert formatting whitespace between the
        // line timestamp and the first timed-token tag. That whitespace is not
        // part of the sung lyric text, so remove it only when the prefix before
        // the first word tag contains no visible characters.
        val firstWordMatch = WORD_TIMESTAMP.find(remaining)
        if (firstWordMatch != null &&
            remaining.substring(0, firstWordMatch.range.first).isBlank()
        ) {
            remaining = remaining.substring(firstWordMatch.range.first)
        }

        val wordMatches = WORD_TIMESTAMP.findAll(remaining).toList()
        val wordStarts = wordMatches.map(::parseTimestamp)
        if (wordStarts.zipWithNext().any { (left, right) -> left > right }) return emptyList()

        val fullText = WORD_TIMESTAMP.replace(remaining, "")
        val words = wordMatches.mapIndexedNotNull { index, match ->
            val textStart = match.range.last + 1
            val textEnd = wordMatches.getOrNull(index + 1)?.range?.first ?: remaining.length
            if (textStart > textEnd) return@mapIndexedNotNull null

            // Enhanced LRC timestamps delimit the preceding visible token as well
            // as starting the next one. Preserve a trailing textless timestamp as
            // the previous token's explicit end instead of inventing an invisible
            // word. Non-final tokens therefore retain the same natural next-start
            // boundary that the timing engine already inferred at presentation time.
            val tokenText = remaining.substring(textStart, textEnd)
            if (tokenText.isEmpty()) return@mapIndexedNotNull null
            val startMs = wordStarts[index]
            TimedWord(
                startMs = startMs,
                text = tokenText,
                endMs = wordStarts.getOrNull(index + 1)?.takeIf { it >= startMs },
            )
        }

        return timestamps.map { ts ->
            if (words.isNotEmpty() && fullText.isNotBlank()) {
                TimedLyricLine(startMs = ts, text = fullText, words = words)
            } else {
                TimedLyricLine(startMs = ts, text = fullText.ifBlank { "♪" })
            }
        }
    }

    private fun parseTimestamp(match: MatchResult): Long {
        val min = match.groupValues[1].toLong()
        val sec = match.groupValues[2].toLong()
        val msRaw = match.groupValues[3]
        val ms = if (msRaw.length == 2) msRaw.toLong() * 10 else msRaw.toLong()
        return min * 60_000 + sec * 1_000 + ms
    }
}
