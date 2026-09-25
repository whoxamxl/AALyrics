package io.github.whoxamxl.aalyrics.translation.core

import io.github.whoxamxl.aalyrics.translation.api.TranslationLanguages
import io.github.whoxamxl.aalyrics.translation.api.TranslationProvider
import io.github.whoxamxl.aalyrics.translation.api.TranslationSettings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicLong

interface TranslationLifecycle {
    val state: StateFlow<TranslationState>

    fun update(
        canonical: CanonicalLyrics?,
        settings: TranslationSettings,
    )

    fun clear()
}

/** Owns one active Translation request and publishes only complete artifacts. */
class TranslationCoordinator(
    private val profiler: LanguageProfiler,
    private val planner: TranslationBlockPlanner,
    providers: List<TranslationProvider>,
    private val scope: CoroutineScope,
    private val assembler: TranslationArtifactAssembler = TranslationArtifactAssembler(),
) : TranslationLifecycle {
    private val providers = providers.toList()
    private val requestIds = AtomicLong(0L)
    private val _state = MutableStateFlow<TranslationState>(TranslationState.Idle)
    override val state: StateFlow<TranslationState> = _state.asStateFlow()

    private var activeKey: RequestKey? = null
    private var activeJob: Job? = null

    init {
        require(this.providers.isNotEmpty()) {
            "TranslationCoordinator requires at least one Translation Provider"
        }
    }

    @Synchronized
    override fun update(
        canonical: CanonicalLyrics?,
        settings: TranslationSettings,
    ) {
        if (!settings.enabled) {
            activeJob?.cancel()
            activeJob = null
            activeKey = null
            _state.value = TranslationState.Disabled
            return
        }
        if (canonical == null) {
            clear()
            return
        }

        val target = TranslationLanguages.normalizeTargetLanguage(settings.targetLanguage)
        val key = RequestKey(canonical.identity, target)
        if (key == activeKey) return

        activeJob?.cancel()
        activeKey = key
        val request = TranslationRequestIdentity(
            id = TranslationRequestId(requestIds.getAndIncrement()),
            canonicalLyrics = canonical.identity,
            targetLanguage = target,
        )
        _state.value = TranslationState.Translating(request)
        activeJob = scope.launch {
            val completion = try {
                execute(request, canonical)
            } catch (cancellation: CancellationException) {
                // Supersession/clear remains cancellation. A task-level cancellation
                // while this request coroutine is still active is a Translation failure.
                currentCoroutineContext().ensureActive()
                TranslationState.Failed(
                    request = request,
                    profile = currentProfileFor(request),
                    reason = TranslationFailureReason.UNEXPECTED,
                )
            } catch (_: Exception) {
                TranslationState.Failed(
                    request = request,
                    profile = currentProfileFor(request),
                    reason = TranslationFailureReason.UNEXPECTED,
                )
            }
            _state.update { current ->
                if (current is TranslationState.Translating && current.request == request) {
                    completion
                } else {
                    current
                }
            }
        }
    }

    @Synchronized
    override fun clear() {
        activeJob?.cancel()
        activeJob = null
        activeKey = null
        _state.value = TranslationState.Idle
    }

    private suspend fun execute(
        request: TranslationRequestIdentity,
        canonical: CanonicalLyrics,
    ): TranslationState {
        val profile = try {
            profiler.profile(canonical.document, request.targetLanguage)
        } catch (cancellation: CancellationException) {
            currentCoroutineContext().ensureActive()
            return TranslationState.Failed(
                request = request,
                reason = TranslationFailureReason.LANGUAGE_PROFILING_FAILED,
            )
        } catch (_: Exception) {
            return TranslationState.Failed(
                request = request,
                reason = TranslationFailureReason.LANGUAGE_PROFILING_FAILED,
            )
        }

        publishProfileIfCurrent(request, profile)

        val plan = try {
            planner.plan(canonical.document, profile, request.targetLanguage)
        } catch (_: Exception) {
            return TranslationState.Failed(
                request = request,
                profile = profile,
                reason = TranslationFailureReason.TRANSLATION_PLANNING_FAILED,
            )
        }
        if (plan.blocks.isEmpty()) {
            return TranslationState.NotRequired(request, profile)
        }

        providers.forEach { provider ->
            val artifact = try {
                assembler.assemble(
                    request = request,
                    canonical = canonical,
                    profile = profile,
                    plan = plan,
                    provider = provider,
                )
            } catch (cancellation: CancellationException) {
                currentCoroutineContext().ensureActive()
                return TranslationState.Failed(
                    request = request,
                    profile = profile,
                    reason = TranslationFailureReason.PROVIDER_EXECUTION_FAILED,
                )
            } catch (_: Exception) {
                null
            }
            if (artifact != null) return TranslationState.Ready(artifact)
        }
        return TranslationState.Failed(
            request = request,
            profile = profile,
            reason = TranslationFailureReason.PROVIDER_EXECUTION_FAILED,
        )
    }

    private fun publishProfileIfCurrent(
        request: TranslationRequestIdentity,
        profile: LanguageProfile,
    ) {
        _state.update { current ->
            if (current is TranslationState.Translating && current.request == request) {
                current.copy(profile = profile)
            } else {
                current
            }
        }
    }

    private fun currentProfileFor(request: TranslationRequestIdentity): LanguageProfile? =
        (_state.value as? TranslationState.Translating)
            ?.takeIf { it.request == request }
            ?.profile

    private data class RequestKey(
        val canonicalLyrics: CanonicalLyricsIdentity,
        val targetLanguage: String,
    )
}
