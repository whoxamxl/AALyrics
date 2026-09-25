package io.github.whoxamxl.aalyrics

import io.github.whoxamxl.aalyrics.core.model.TimedLyricLine
import io.github.whoxamxl.aalyrics.core.timing.LyricsTimingProjection
import io.github.whoxamxl.aalyrics.core.timing.WordTimingBoundary
import io.github.whoxamxl.aalyrics.ui.phone.lyrics.KaraokeSweepUiState

/** Phone-only display grouping adapted from auto-lyrics 8484bed2 PhoneKaraokeSweep. */
internal fun mapPhoneKaraokeSweep(
    line: TimedLyricLine,
    timing: LyricsTimingProjection,
    effectivePositionMs: Long,
): KaraokeSweepUiState? {
    if (timing.wordBoundary != WordTimingBoundary.ACTIVE) return null
    if (!LyricWordLayout.hasRenderableWordGranularity(line)) return null
    val activeIndex = timing.activeWordIndex ?: return null
    val ranges = LyricWordLayout.displayRangesForLine(line) ?: return null
    val activeRange = ranges.getOrNull(activeIndex) ?: return null
    var first = activeIndex
    while (first > 0 && ranges[first - 1] == activeRange) first--
    var last = activeIndex
    while (last < ranges.lastIndex && ranges[last + 1] == activeRange) last++

    val groupStartMs = line.words[first].startMs
    val groupEndMs = line.words[last].endMs
        ?: line.words.getOrNull(last + 1)?.startMs
        ?: (groupStartMs + FINAL_GROUP_VISUAL_DURATION_MS)
    if (groupEndMs <= groupStartMs || effectivePositionMs >= groupEndMs) return null

    val semanticProgress = timing.wordProgress
    val progress = if (first == last && semanticProgress != null) {
        semanticProgress
    } else {
        ((effectivePositionMs - groupStartMs).toDouble() /
            (groupEndMs - groupStartMs).toDouble()).coerceIn(0.0, 1.0).toFloat()
    }
    return KaraokeSweepUiState(activeRange.start, activeRange.end, progress)
}

private const val FINAL_GROUP_VISUAL_DURATION_MS = 650L
