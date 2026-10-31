package io.github.whoxamxl.aalyrics.core.lyrics

import io.github.whoxamxl.aalyrics.core.model.LyricsDocument
import io.github.whoxamxl.aalyrics.core.model.Track

/** All retrieval/selection inputs that can change a cached winner. No playback timeline state. */
data class LyricsCacheKey(
    val track: Track,
    val preferences: CandidateSelectionPreferences,
    val providerIds: Set<String>,
)

/**
 * Replaceable storage boundary for selected, canonical lyrics only.
 *
 * Implementations own format, identity encoding, invalidation and capacity policy, and must move
 * blocking I/O off the caller's dispatcher. Failures are advisory; cancellation must propagate.
 * A hit must preserve the full document, without Translation, calibration or presentation state.
 */
interface LyricsCache {
    suspend fun get(key: LyricsCacheKey): LyricsDocument?
    suspend fun put(key: LyricsCacheKey, lyrics: LyricsDocument)

    data object None : LyricsCache {
        override suspend fun get(key: LyricsCacheKey): LyricsDocument? = null
        override suspend fun put(key: LyricsCacheKey, lyrics: LyricsDocument) = Unit
    }
}
