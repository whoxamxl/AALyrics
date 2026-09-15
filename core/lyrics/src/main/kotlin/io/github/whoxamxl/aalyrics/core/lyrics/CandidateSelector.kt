package io.github.whoxamxl.aalyrics.core.lyrics

import io.github.whoxamxl.aalyrics.core.model.LyricsSyncType
import io.github.whoxamxl.aalyrics.core.model.Track
import io.github.whoxamxl.aalyrics.provider.api.LyricsCandidate

/**
 * Provider-independent preferences supplied to candidate selection.
 *
 * A preference is input to the selection policy, not a hard eligibility rule.
 * The production interpretation of these preferences is intentionally deferred
 * to the mature resolver behavior that will be adapted after the Core Readiness
 * Gate.
 */
data class CandidateSelectionPreferences(
    val preferredSyncType: LyricsSyncType? = null,
)

/**
 * Boundary between lyrics orchestration and cross-provider winner selection.
 *
 * Implementations receive only normalized AALyrics models. The coordinator does
 * not know scoring weights, provider confidence rules, metadata matching details,
 * or recording-version heuristics.
 *
 * Before the Core Readiness Gate, core tests use fakes. The production
 * implementation is reserved for later adaptation of the proven resolver in the
 * working Auto Lyrics fork.
 */
interface CandidateSelector {
    /**
     * Returns the selected candidate, or null when none of [candidates] is usable.
     *
     * Implementations must not rely on provider execution order as an implicit
     * winner-selection rule.
     */
    fun select(
        track: Track,
        candidates: List<LyricsCandidate>,
        preferences: CandidateSelectionPreferences = CandidateSelectionPreferences(),
    ): LyricsCandidate?
}
