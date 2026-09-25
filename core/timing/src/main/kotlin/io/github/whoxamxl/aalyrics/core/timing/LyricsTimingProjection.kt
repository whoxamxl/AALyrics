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

    return LyricsTimingProjection(
        activeLineIndex = activeLineIndex,
        activeWordIndex = null,
        wordProgress = null,
        wordBoundary = WordTimingBoundary.UNAVAILABLE,
    )
}
