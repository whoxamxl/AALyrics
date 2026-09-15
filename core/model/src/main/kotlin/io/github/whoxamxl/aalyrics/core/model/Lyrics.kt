package io.github.whoxamxl.aalyrics.core.model

/** Describes how precise the timing information in a lyric document is. */
enum class LyricsSyncType {
    PLAIN,
    LINE,
    WORD,
}

/** One logical lyric row. */
sealed interface LyricLine {
    val text: String
}

/** A lyric row without timing information. */
data class PlainLyricLine(
    override val text: String,
) : LyricLine

/** A lyric row anchored to the playback timeline. */
data class TimedLyricLine(
    override val text: String,
    val startMs: Long,
    val endMs: Long? = null,
    val words: List<TimedWord> = emptyList(),
) : LyricLine {
    init {
        require(startMs >= 0L) { "Lyric line start time must not be negative" }
        require(endMs == null || endMs >= startMs) {
            "Lyric line end time must not precede its start time"
        }
        require(words.zipWithNext().all { (left, right) -> left.startMs <= right.startMs }) {
            "Timed words must be ordered by start time"
        }
    }
}

/** Word-level timing for karaoke-style rendering. */
data class TimedWord(
    val text: String,
    val startMs: Long,
    val endMs: Long? = null,
) {
    init {
        require(startMs >= 0L) { "Word start time must not be negative" }
        require(endMs == null || endMs >= startMs) {
            "Word end time must not precede its start time"
        }
    }
}

/** Attribution retained with a resolved lyric document. */
data class LyricsAttribution(
    val providerId: String,
    val displayName: String = providerId,
    val sourceId: String? = null,
) {
    init {
        require(providerId.isNotBlank()) { "Provider id must not be blank" }
        require(displayName.isNotBlank()) { "Provider display name must not be blank" }
        require(sourceId == null || sourceId.isNotBlank()) { "Source id must be null or non-blank" }
    }
}

/**
 * Provider-independent lyrics ready for application use.
 *
 * A document may contain a mixture of plain and timed rows. Its sync type is
 * derived from the strongest timing data available instead of being stored as
 * a second, potentially inconsistent source of truth.
 */
data class LyricsDocument(
    val lines: List<LyricLine>,
    val languageTag: String? = null,
    val attribution: LyricsAttribution? = null,
) {
    init {
        require(languageTag == null || languageTag.isNotBlank()) {
            "Language tag must be null or non-blank"
        }
    }

    val syncType: LyricsSyncType
        get() = when {
            lines.any { it is TimedLyricLine && it.words.isNotEmpty() } -> LyricsSyncType.WORD
            lines.any { it is TimedLyricLine } -> LyricsSyncType.LINE
            else -> LyricsSyncType.PLAIN
        }

    val isEmpty: Boolean
        get() = lines.isEmpty()
}
