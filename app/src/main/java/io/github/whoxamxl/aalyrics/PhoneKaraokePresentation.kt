package io.github.whoxamxl.aalyrics

import io.github.whoxamxl.aalyrics.core.model.TimedLyricLine
import io.github.whoxamxl.aalyrics.core.timing.LyricsTimingProjection
import io.github.whoxamxl.aalyrics.core.timing.WordTimingBoundary
import io.github.whoxamxl.aalyrics.ui.phone.lyrics.KaraokeLineUiState
import io.github.whoxamxl.aalyrics.ui.phone.lyrics.KaraokeSweepUiState

/** Phone-only display grouping adapted from auto-lyrics 8484bed2 PhoneKaraokeSweep. */
internal fun mapPhoneKaraokeLine(
    line: TimedLyricLine,
    timing: LyricsTimingProjection,
    effectivePositionMs: Long,
    nextTimedLineStartMs: Long? = null,
): KaraokeLineUiState? {
    if (!LyricWordLayout.hasRenderableWordGranularity(line)) return null
    val ranges = LyricWordLayout.displayRangesForLine(line) ?: return null

    return when (timing.wordBoundary) {
        WordTimingBoundary.UNAVAILABLE -> null
        WordTimingBoundary.BEFORE_FIRST -> KaraokeLineUiState(completedEnd = 0)
        WordTimingBoundary.ACTIVE -> {
            val activeIndex = timing.activeWordIndex ?: return null
            val group = displayGroup(line, ranges, activeIndex) ?: return null
            val groupEndMs = resolveGroupEndMs(
                line = line,
                group = group,
                nextTimedLineStartMs = nextTimedLineStartMs,
            )
            if (groupEndMs <= group.startMs) return null
            if (effectivePositionMs >= groupEndMs) {
                KaraokeLineUiState(completedEnd = group.range.end)
            } else {
                val semanticProgress = timing.wordProgress
                val progress = if (group.firstIndex == group.lastIndex && semanticProgress != null) {
                    semanticProgress
                } else {
                    progressBetween(
                        positionMs = effectivePositionMs,
                        startMs = group.startMs,
                        endMs = groupEndMs,
                    )
                }
                KaraokeLineUiState(
                    completedEnd = group.range.start,
                    sweep = KaraokeSweepUiState(
                        start = group.range.start,
                        end = group.range.end,
                        progress = progress,
                    ),
                )
            }
        }
        WordTimingBoundary.GAP -> {
            val completedIndex = line.words.indices.lastOrNull { index ->
                val word = line.words[index]
                word.startMs <= effectivePositionMs &&
                    word.endMs?.let { endMs -> effectivePositionMs >= endMs } == true
            } ?: return null
            val completedRange = ranges.getOrNull(completedIndex) ?: return null
            val nextIndex = completedIndex + 1
            val nextRange = ranges.getOrNull(nextIndex)

            if (nextRange != completedRange) {
                KaraokeLineUiState(completedEnd = completedRange.end)
            } else {
                val group = displayGroup(line, ranges, completedIndex) ?: return null
                val groupEndMs = resolveGroupEndMs(
                    line = line,
                    group = group,
                    nextTimedLineStartMs = nextTimedLineStartMs,
                )
                val holdPositionMs = line.words[completedIndex].endMs ?: return null
                if (groupEndMs <= group.startMs) return null
                KaraokeLineUiState(
                    completedEnd = group.range.start,
                    sweep = KaraokeSweepUiState(
                        start = group.range.start,
                        end = group.range.end,
                        progress = progressBetween(
                            positionMs = holdPositionMs,
                            startMs = group.startMs,
                            endMs = groupEndMs,
                        ),
                    ),
                )
            }
        }
        WordTimingBoundary.AFTER_LAST -> KaraokeLineUiState(completedEnd = line.text.length)
    }
}

private data class DisplayGroup(
    val firstIndex: Int,
    val lastIndex: Int,
    val range: LyricWordLayout.DisplayRange,
    val startMs: Long,
)

private fun displayGroup(
    line: TimedLyricLine,
    ranges: List<LyricWordLayout.DisplayRange>,
    tokenIndex: Int,
): DisplayGroup? {
    val activeRange = ranges.getOrNull(tokenIndex) ?: return null
    var first = tokenIndex
    while (first > 0 && ranges[first - 1] == activeRange) first--
    var last = tokenIndex
    while (last < ranges.lastIndex && ranges[last + 1] == activeRange) last++
    return DisplayGroup(
        firstIndex = first,
        lastIndex = last,
        range = activeRange,
        startMs = line.words[first].startMs,
    )
}

private fun resolveGroupEndMs(
    line: TimedLyricLine,
    group: DisplayGroup,
    nextTimedLineStartMs: Long?,
): Long = line.words[group.lastIndex].endMs
    ?: line.words.getOrNull(group.lastIndex + 1)?.startMs
    ?: line.endMs
    ?: nextTimedLineStartMs
    ?: (group.startMs + FINAL_GROUP_VISUAL_DURATION_MS)

private fun progressBetween(
    positionMs: Long,
    startMs: Long,
    endMs: Long,
): Float = ((positionMs - startMs).toDouble() / (endMs - startMs).toDouble())
    .coerceIn(0.0, 1.0)
    .toFloat()

private const val FINAL_GROUP_VISUAL_DURATION_MS = 650L
