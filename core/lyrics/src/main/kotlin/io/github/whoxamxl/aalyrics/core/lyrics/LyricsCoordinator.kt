package io.github.whoxamxl.aalyrics.core.lyrics

import io.github.whoxamxl.aalyrics.core.model.PlaybackTrackIdentity
import io.github.whoxamxl.aalyrics.core.model.Track
import io.github.whoxamxl.aalyrics.provider.api.LyricsCandidate
import io.github.whoxamxl.aalyrics.provider.api.LyricsProvider
import io.github.whoxamxl.aalyrics.provider.api.LyricsRequest
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

/**
 * Provider-independent orchestration for one active lyrics lookup lifecycle.
 *
 * The coordinator owns request identity, provider fan-out, failure isolation,
 * candidate handoff, and observable [LyricsState]. It deliberately does not own
 * provider-specific networking, cache policy, matching/scoring heuristics,
 * translation, Android media integration, or presentation behavior.
 *
 * [scope] is supplied by the composition root so process/feature lifecycle
 * ownership remains outside the pure lyrics core.
 */
class LyricsCoordinator(
    providers: List<LyricsProvider>,
    private val selector: CandidateSelector,
    private val scope: CoroutineScope,
) : LyricsLookupLifecycle {
    private val providers = providers.toList()
    private val lookupIds = AtomicLong(0L)

    private val _state = MutableStateFlow<LyricsState>(LyricsState.Idle)
    val state: StateFlow<LyricsState> = _state.asStateFlow()

    private var activeJob: Job? = null

    init {
        require(this.providers.isNotEmpty()) { "LyricsCoordinator requires at least one provider" }
    }

    /**
     * Starts a fresh lookup and supersedes any previous active lookup.
     *
     * Starting the same track again still receives a fresh [LyricsLookupId], so
     * refreshes and late results cannot be confused by track equality alone.
     */
    override fun startLookup(
        track: Track,
        preferences: CandidateSelectionPreferences,
        playbackIdentity: PlaybackTrackIdentity?,
    ): LyricsLookup {
        val lookup = LyricsLookup(
            id = LyricsLookupId(lookupIds.getAndIncrement()),
            track = track,
            playbackIdentity = playbackIdentity ?: PlaybackTrackIdentity.Metadata(
                sourceId = null,
                title = track.title,
                artists = track.artists,
                album = track.album,
            ),
        )

        activeJob?.cancel()
        _state.update { current -> current.reduce(LyricsStateEvent.Started(lookup)) }

        activeJob = scope.launch {
            val attempts = searchProviders(
                LyricsRequest(
                    track = track,
                    preferredSyncType = preferences.preferredSyncType,
                ),
            )

            val failedAttempts = attempts.count { it is ProviderAttempt.Failed }
            val candidates = attempts
                .filterIsInstance<ProviderAttempt.Succeeded>()
                .flatMap { it.candidates }

            val selected = selector.select(
                track = track,
                candidates = candidates,
                preferences = preferences,
            )
            require(selected == null || selected in candidates) {
                "CandidateSelector must return one of the supplied candidates"
            }

            val completion = when {
                selected != null && failedAttempts == 0 -> LyricsStateEvent.Resolved(
                    lookupId = lookup.id,
                    lyrics = selected.lyrics,
                )

                selected != null -> LyricsStateEvent.ResolvedDegraded(
                    lookupId = lookup.id,
                    lyrics = selected.lyrics,
                    failedAttempts = failedAttempts,
                )

                failedAttempts == 0 -> LyricsStateEvent.NoLyrics(lookup.id)

                else -> LyricsStateEvent.Failed(
                    lookupId = lookup.id,
                    failedAttempts = failedAttempts,
                )
            }

            // StateFlow.update makes the stale-result guard atomic with respect to
            // a concurrent Started/Cleared event. If ownership changes while the
            // reducer is running, the transform is retried against the new state
            // and the obsolete lookup id is rejected.
            _state.update { current -> current.reduce(completion) }
        }

        return lookup
    }

    /**
     * Cancels provider work when lyrics demand disappears.
     *
     * A completed usable result stays in memory so foregrounding the same playback identity can
     * resume immediately without a provider refetch. Loading and unusable terminal states are
     * cleared so they restart normally when demand returns.
     */
    override fun suspendLookup(): Boolean {
        activeJob?.cancel()
        activeJob = null

        return when (_state.value) {
            is LyricsState.Ready,
            is LyricsState.Degraded -> true

            else -> {
                _state.update { current -> current.reduce(LyricsStateEvent.Cleared) }
                false
            }
        }
    }

    /** Cancels active lookup work and returns observable state to idle. */
    override fun clear() {
        activeJob?.cancel()
        activeJob = null
        _state.update { current -> current.reduce(LyricsStateEvent.Cleared) }
    }

    private suspend fun searchProviders(request: LyricsRequest): List<ProviderAttempt> = supervisorScope {
        providers.map { provider ->
            async {
                try {
                    ProviderAttempt.Succeeded(provider.search(request))
                } catch (cancellation: CancellationException) {
                    // Cancellation represents supersession/lifecycle shutdown, not
                    // provider failure. Preserve structured concurrency semantics.
                    throw cancellation
                } catch (_: Exception) {
                    ProviderAttempt.Failed
                }
            }
        }.awaitAll()
    }

    private sealed interface ProviderAttempt {
        data class Succeeded(
            val candidates: List<LyricsCandidate>,
        ) : ProviderAttempt

        data object Failed : ProviderAttempt
    }
}
