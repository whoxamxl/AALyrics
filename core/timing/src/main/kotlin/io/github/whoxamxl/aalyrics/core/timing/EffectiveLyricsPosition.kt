package io.github.whoxamxl.aalyrics.core.timing

/** A signed shift of the lyrics clock: positive advances lyrics, negative delays them. */
@JvmInline
value class LyricsTimingOffset(val milliseconds: Long) {
    companion object {
        val ZERO = LyricsTimingOffset(0L)
    }
}

/** The lyrics-only position used to compare against unchanged source timestamps. */
@JvmInline
value class EffectiveLyricsPosition(val milliseconds: Long)

/** Derives the lyrics clock without changing playback position or source timestamps. */
fun effectiveLyricsPosition(
    projectedPlaybackPositionMs: Long,
    offset: LyricsTimingOffset,
): EffectiveLyricsPosition = EffectiveLyricsPosition(projectedPlaybackPositionMs + offset.milliseconds)
