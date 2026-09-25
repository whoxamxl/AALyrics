package io.github.whoxamxl.aalyrics.core.timing

import io.github.whoxamxl.aalyrics.core.model.LyricsDocument
import io.github.whoxamxl.aalyrics.core.model.TimedLyricLine

/** Timing facts over canonical lyrics; indices refer to the original document and active line. */
data class LyricsTimingProjection(
    val activeLineIndex: Int?,
    val activeWordIndex: Int?,
    val wordProgress: Float?,
    val wordBoundary: WordTimingBoundary,
)

enum class WordTimingBoundary {
    UNAVAILABLE,
    BEFORE_FIRST,
    ACTIVE,
    GAP,
    AFTER_LAST,
}

/** Stateless semantic projection from the lyrics-only clock and unchanged source timing. */
fun projectLyricsTiming(
    document: LyricsDocument,
    position: EffectiveLyricsPosition,
): LyricsTimingProjection {
    val activeLineIndex = document.lines.indices.lastOrNull { index ->
        val line = document.lines[index]
        line is TimedLyricLine && line.startMs <= position.milliseconds
    }
    val activeLine = activeLineIndex?.let { document.lines[it] as TimedLyricLine }
    val words = activeLine?.words.orEmpty()
    if (words.isEmpty()) {
        return LyricsTimingProjection(
            activeLineIndex = activeLineIndex,
            activeWordIndex = null,
            wordProgress = null,
            wordBoundary = WordTimingBoundary.UNAVAILABLE,
        )
    }

    val latestStartedWordIndex = words.indices.lastOrNull { index ->
        words[index].startMs <= position.milliseconds
    }
    if (latestStartedWordIndex == null) {
        return LyricsTimingProjection(
            activeLineIndex = activeLineIndex,
            activeWordIndex = null,
            wordProgress = null,
            wordBoundary = WordTimingBoundary.BEFORE_FIRST,
        )
    }

    val latestStartedWord = words[latestStartedWordIndex]
    if (latestStartedWord.endMs?.let { position.milliseconds >= it } == true) {
        return LyricsTimingProjection(
            activeLineIndex = activeLineIndex,
            activeWordIndex = null,
            wordProgress = null,
            wordBoundary = if (latestStartedWordIndex == words.lastIndex) {
                WordTimingBoundary.AFTER_LAST
            } else {
                WordTimingBoundary.GAP
            },
        )
    }

    return LyricsTimingProjection(
        activeLineIndex = activeLineIndex,
        activeWordIndex = latestStartedWordIndex,
        wordProgress = null,
        wordBoundary = WordTimingBoundary.ACTIVE,
    )
}
