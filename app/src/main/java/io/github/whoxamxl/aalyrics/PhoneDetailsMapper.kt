package io.github.whoxamxl.aalyrics

import io.github.whoxamxl.aalyrics.core.lyrics.LyricsState
import io.github.whoxamxl.aalyrics.core.model.LyricsDocument
import io.github.whoxamxl.aalyrics.core.model.PlaybackSnapshot
import io.github.whoxamxl.aalyrics.translation.api.TranslationLanguages
import io.github.whoxamxl.aalyrics.translation.api.TranslationModelPhase
import io.github.whoxamxl.aalyrics.translation.api.TranslationModelState
import io.github.whoxamxl.aalyrics.translation.api.TranslationSettings
import io.github.whoxamxl.aalyrics.translation.core.CanonicalLyricsIdentity
import io.github.whoxamxl.aalyrics.translation.core.LanguageProfile
import io.github.whoxamxl.aalyrics.translation.core.SecondaryActivation
import io.github.whoxamxl.aalyrics.translation.core.TranslationFailureReason
import io.github.whoxamxl.aalyrics.translation.core.TranslationRequestIdentity
import io.github.whoxamxl.aalyrics.translation.core.TranslationState
import io.github.whoxamxl.aalyrics.ui.phone.details.DetailsDiagnosticsUiState
import io.github.whoxamxl.aalyrics.ui.phone.details.DetailsLyricsUiState
import io.github.whoxamxl.aalyrics.ui.phone.details.DetailsLyricsUiStatus
import io.github.whoxamxl.aalyrics.ui.phone.details.DetailsScreenUiState
import io.github.whoxamxl.aalyrics.ui.phone.details.DetailsTrackUiState
import io.github.whoxamxl.aalyrics.ui.phone.details.DetailsTranslationModelPhaseUiState
import io.github.whoxamxl.aalyrics.ui.phone.details.DetailsTranslationModelUiState
import io.github.whoxamxl.aalyrics.ui.phone.details.DetailsTranslationRuntimeFailureUiReason
import io.github.whoxamxl.aalyrics.ui.phone.details.DetailsTranslationRuntimeUiState
import io.github.whoxamxl.aalyrics.ui.phone.details.DetailsTranslationUiState
import io.github.whoxamxl.aalyrics.ui.phone.details.DetailsVerboseProgressUiState
import java.util.Locale

/** Application-owned mapping from canonical runtime facts into Phone Details presentation state. */
internal fun mapPhoneDetailsState(
    playback: PlaybackSnapshot,
    lyricsState: LyricsState,
    verboseDetailsEnabled: Boolean,
    playbackSourceAppInfo: PlaybackSourceAppInfo? = null,
    translationSettings: TranslationSettings = TranslationSettings(enabled = false),
    translationState: TranslationState = TranslationState.Disabled,
    translationModelStates: Map<String, TranslationModelState> = emptyMap(),
    translationModelInventoryReconciled: Boolean = false,
    displayLocale: Locale = Locale.getDefault(),
): DetailsScreenUiState {
    val track = playback.track
    val resolvedLyrics = currentLyrics(playback, lyricsState)
    val lyricsDocument = resolvedLyrics.document
    val sourceAppInfo = playbackSourceAppInfo
        ?.takeIf { appInfo -> appInfo.packageName == playback.source?.id }

    return DetailsScreenUiState(
        track = track?.let {
            DetailsTrackUiState(
                title = it.title,
                artist = it.artists
                    .takeIf { artists -> artists.isNotEmpty() }
                    ?.joinToString(separator = ", "),
                album = it.album,
                durationLabel = it.durationMs?.let(::formatDuration),
                playbackSourceLabel = sourceAppInfo?.label,
            )
        },
        lyrics = lyricsDocument?.let { lyrics ->
            DetailsLyricsUiState(
                providerDisplayName = lyrics.attribution?.displayName,
                syncType = lyrics.syncType,
                languageLabel = lyrics.languageTag?.let { tag ->
                    languageLabel(tag, displayLocale)
                },
                lineCount = lyrics.lines.size,
            )
        },
        lyricsStatus = resolvedLyrics.status,
        translation = resolvedLyrics.canonicalIdentity?.let { canonicalIdentity ->
            mapDetailsTranslationState(
                canonicalIdentity = canonicalIdentity,
                settings = translationSettings,
                state = translationState,
                modelStates = translationModelStates,
                modelInventoryReconciled = translationModelInventoryReconciled,
                verboseDetailsEnabled = verboseDetailsEnabled,
                displayLocale = displayLocale,
            )
        },
        diagnostics = if (verboseDetailsEnabled) {
            DetailsDiagnosticsUiState(
                appPackageName = playback.source?.id,
                appCategory = sourceAppInfo?.category?.displayLabel,
                appMinSdkVersion = sourceAppInfo?.minSdkVersion,
                appTargetSdkVersion = sourceAppInfo?.targetSdkVersion,
                providerId = lyricsDocument?.attribution?.providerId,
                sourceId = lyricsDocument?.attribution?.sourceId,
                trackReferences = track
                    ?.references
                    .orEmpty()
                    .sortedWith(compareBy({ it.namespace }, { it.value }))
                    .map { reference -> "${reference.namespace}:${reference.value}" },
            )
        } else {
            null
        },
    )
}

private data class CurrentLyrics(
    val document: LyricsDocument?,
    val status: DetailsLyricsUiStatus,
    val canonicalIdentity: CanonicalLyricsIdentity? = null,
)

private fun currentLyrics(
    playback: PlaybackSnapshot,
    state: LyricsState,
): CurrentLyrics {
    val track = playback.track
    if (track == null) {
        return CurrentLyrics(
            document = null,
            status = DetailsLyricsUiStatus.UNAVAILABLE,
        )
    }

    val lookupState = state as? LyricsState.ForLookup
        ?: return CurrentLyrics(null, DetailsLyricsUiStatus.UNAVAILABLE)
    if (lookupState.lookup.playbackIdentity != playback.trackIdentity) {
        return CurrentLyrics(null, DetailsLyricsUiStatus.UNAVAILABLE)
    }

    return when (state) {
        is LyricsState.Ready ->
            CurrentLyrics(
                document = state.lyrics,
                status = DetailsLyricsUiStatus.READY,
                canonicalIdentity = state.canonicalLyricsOrNull()?.identity,
            )
        is LyricsState.Degraded ->
            CurrentLyrics(
                document = state.lyrics,
                status = DetailsLyricsUiStatus.READY,
                canonicalIdentity = state.canonicalLyricsOrNull()?.identity,
            )
        is LyricsState.Loading ->
            CurrentLyrics(null, DetailsLyricsUiStatus.LOADING)
        is LyricsState.NotFound ->
            CurrentLyrics(null, DetailsLyricsUiStatus.NOT_FOUND)
        is LyricsState.Failed ->
            CurrentLyrics(null, DetailsLyricsUiStatus.FAILED)
        LyricsState.Idle ->
            CurrentLyrics(null, DetailsLyricsUiStatus.UNAVAILABLE)
    }
}

private fun mapDetailsTranslationState(
    canonicalIdentity: CanonicalLyricsIdentity,
    settings: TranslationSettings,
    state: TranslationState,
    modelStates: Map<String, TranslationModelState>,
    modelInventoryReconciled: Boolean,
    verboseDetailsEnabled: Boolean,
    displayLocale: Locale,
): DetailsTranslationUiState {
    val targetLanguage = TranslationLanguages.normalizeTargetLanguage(settings.targetLanguage)
    val matchingProfile = state.matchingProfile(
        canonicalIdentity = canonicalIdentity,
        targetLanguage = targetLanguage,
    )
    val primary = matchingProfile?.primary
    val activeSecondary = matchingProfile
        ?.secondaryCandidate
        ?.takeIf { matchingProfile.secondaryActivation == SecondaryActivation.ACTIVE }

    val sourceLanguageLabel = primary?.let { primaryLanguage ->
        val primaryLabel = TranslationLanguages.displayName(primaryLanguage, displayLocale)
        activeSecondary?.let { secondaryLanguage ->
            "$primaryLabel (${TranslationLanguages.displayName(secondaryLanguage, displayLocale)})"
        } ?: primaryLabel
    }

    if (!verboseDetailsEnabled) {
        return DetailsTranslationUiState(
            sourceLanguageLabel = sourceLanguageLabel,
            targetLanguageLabel = TranslationLanguages.displayName(targetLanguage, displayLocale),
        )
    }

    val sourceModelLanguages = buildList {
        primary?.let { add(it) }
        activeSecondary?.let { add(it) }
    }
        .mapNotNull(TranslationLanguages::normalizeLanguageTag)
        .distinct()

    return DetailsTranslationUiState(
        sourceLanguageLabel = sourceLanguageLabel,
        targetLanguageLabel = TranslationLanguages.displayName(targetLanguage, displayLocale),
        runtimeState = state.runtimeUiStateFor(
            settingsEnabled = settings.enabled,
            canonicalIdentity = canonicalIdentity,
            targetLanguage = targetLanguage,
        ),
        runtimeFailureReason = state.runtimeFailureUiReasonFor(
            canonicalIdentity = canonicalIdentity,
            targetLanguage = targetLanguage,
        ),
        sourceModel = sourceModelLanguages
            .takeIf { it.isNotEmpty() }
            ?.let { languages ->
                aggregateModelState(
                    languages = languages,
                    translationEnabled = settings.enabled,
                    modelStates = modelStates,
                    modelInventoryReconciled = modelInventoryReconciled,
                )
            },
        targetModel = modelUiState(
            languageTag = targetLanguage,
            translationEnabled = settings.enabled,
            modelStates = modelStates,
            modelInventoryReconciled = modelInventoryReconciled,
        ),
    )
}

private fun TranslationState.matchingProfile(
    canonicalIdentity: CanonicalLyricsIdentity,
    targetLanguage: String,
): LanguageProfile? = when (this) {
    is TranslationState.Translating ->
        profile.takeIf { request.matches(canonicalIdentity, targetLanguage) }
    is TranslationState.NotRequired ->
        profile.takeIf { request.matches(canonicalIdentity, targetLanguage) }
    is TranslationState.Ready ->
        artifact.profile.takeIf { artifact.request.matches(canonicalIdentity, targetLanguage) }
    is TranslationState.Failed ->
        profile.takeIf { request.matches(canonicalIdentity, targetLanguage) }
    TranslationState.Disabled,
    TranslationState.Idle -> null
}

private fun TranslationState.runtimeUiStateFor(
    settingsEnabled: Boolean,
    canonicalIdentity: CanonicalLyricsIdentity,
    targetLanguage: String,
): DetailsTranslationRuntimeUiState {
    if (!settingsEnabled) return DetailsTranslationRuntimeUiState.DISABLED

    return when (this) {
        TranslationState.Disabled -> DetailsTranslationRuntimeUiState.DISABLED
        TranslationState.Idle -> DetailsTranslationRuntimeUiState.IDLE
        is TranslationState.Translating ->
            if (request.matches(canonicalIdentity, targetLanguage)) {
                DetailsTranslationRuntimeUiState.TRANSLATING
            } else {
                DetailsTranslationRuntimeUiState.IDLE
            }
        is TranslationState.NotRequired ->
            if (request.matches(canonicalIdentity, targetLanguage)) {
                DetailsTranslationRuntimeUiState.NOT_REQUIRED
            } else {
                DetailsTranslationRuntimeUiState.IDLE
            }
        is TranslationState.Ready ->
            if (artifact.request.matches(canonicalIdentity, targetLanguage)) {
                DetailsTranslationRuntimeUiState.READY
            } else {
                DetailsTranslationRuntimeUiState.IDLE
            }
        is TranslationState.Failed ->
            if (request.matches(canonicalIdentity, targetLanguage)) {
                DetailsTranslationRuntimeUiState.FAILED
            } else {
                DetailsTranslationRuntimeUiState.IDLE
            }
    }
}

private fun TranslationState.runtimeFailureUiReasonFor(
    canonicalIdentity: CanonicalLyricsIdentity,
    targetLanguage: String,
): DetailsTranslationRuntimeFailureUiReason? =
    (this as? TranslationState.Failed)
        ?.takeIf { it.request.matches(canonicalIdentity, targetLanguage) }
        ?.reason
        ?.let { reason ->
            when (reason) {
                TranslationFailureReason.LANGUAGE_PROFILING_FAILED ->
                    DetailsTranslationRuntimeFailureUiReason.LANGUAGE_PROFILING_FAILED
                TranslationFailureReason.TRANSLATION_PLANNING_FAILED ->
                    DetailsTranslationRuntimeFailureUiReason.TRANSLATION_PLANNING_FAILED
                TranslationFailureReason.PROVIDER_EXECUTION_FAILED ->
                    DetailsTranslationRuntimeFailureUiReason.PROVIDER_EXECUTION_FAILED
                TranslationFailureReason.UNEXPECTED ->
                    DetailsTranslationRuntimeFailureUiReason.UNEXPECTED
            }
        }

private fun TranslationRequestIdentity.matches(
    canonicalIdentity: CanonicalLyricsIdentity,
    targetLanguage: String,
): Boolean =
    canonicalLyrics == canonicalIdentity &&
        this.targetLanguage == targetLanguage

private fun aggregateModelState(
    languages: List<String>,
    translationEnabled: Boolean,
    modelStates: Map<String, TranslationModelState>,
    modelInventoryReconciled: Boolean,
): DetailsTranslationModelUiState {
    val perLanguage = languages.map { language ->
        language to modelUiState(
            languageTag = language,
            translationEnabled = translationEnabled,
            modelStates = modelStates,
            modelInventoryReconciled = modelInventoryReconciled,
        )
    }
    val aggregatePhase = perLanguage
        .map { it.second.phase }
        .maxBy { phase -> modelPhasePriority(phase) }
    val failureReason = perLanguage
        .filter { (_, model) ->
            model.phase == DetailsTranslationModelPhaseUiState.FAILED ||
                model.phase == DetailsTranslationModelPhaseUiState.TIMED_OUT
        }
        .mapNotNull { (language, model) ->
            model.failureReason?.let { reason ->
                "${shortLanguageLabel(language)}: $reason"
            }
        }
        .joinToString(separator = "\n")
        .ifBlank { null }

    return DetailsTranslationModelUiState(
        languageLabel = languages.joinToString(separator = ", ", transform = ::shortLanguageLabel),
        phase = aggregatePhase,
        failureReason = failureReason,
    )
}

private fun modelUiState(
    languageTag: String,
    translationEnabled: Boolean,
    modelStates: Map<String, TranslationModelState>,
    modelInventoryReconciled: Boolean,
): DetailsTranslationModelUiState {
    val normalized = TranslationLanguages.normalizeLanguageTag(languageTag)
        ?: languageTag.lowercase(Locale.US)
    if (normalized == "en") {
        return DetailsTranslationModelUiState(
            languageLabel = shortLanguageLabel(normalized),
            phase = DetailsTranslationModelPhaseUiState.READY,
        )
    }

    val modelState = modelStates[normalized]
    val phase = when {
        modelState == null && !modelInventoryReconciled ->
            DetailsTranslationModelPhaseUiState.CHECKING
        modelState == null && !translationEnabled ->
            DetailsTranslationModelPhaseUiState.NOT_REQUIRED
        modelState == null ->
            DetailsTranslationModelPhaseUiState.CHECKING
        modelState.phase == TranslationModelPhase.CHECKING ->
            DetailsTranslationModelPhaseUiState.CHECKING
        modelState.phase == TranslationModelPhase.DOWNLOADING ->
            DetailsTranslationModelPhaseUiState.DOWNLOADING
        modelState.phase == TranslationModelPhase.WAITING_FOR_SYSTEM ->
            DetailsTranslationModelPhaseUiState.WAITING_FOR_SYSTEM
        modelState.phase == TranslationModelPhase.READY ->
            DetailsTranslationModelPhaseUiState.READY
        modelState.phase == TranslationModelPhase.FAILED ->
            DetailsTranslationModelPhaseUiState.FAILED
        else ->
            DetailsTranslationModelPhaseUiState.TIMED_OUT
    }
    val failureReason = when (phase) {
        DetailsTranslationModelPhaseUiState.FAILED,
        DetailsTranslationModelPhaseUiState.TIMED_OUT ->
            modelState?.error?.trim()?.takeIf(String::isNotEmpty)
        else -> null
    }

    return DetailsTranslationModelUiState(
        languageLabel = shortLanguageLabel(normalized),
        phase = phase,
        failureReason = failureReason,
    )
}

private fun modelPhasePriority(
    phase: DetailsTranslationModelPhaseUiState,
): Int = when (phase) {
    DetailsTranslationModelPhaseUiState.READY -> 0
    DetailsTranslationModelPhaseUiState.NOT_REQUIRED -> 1
    DetailsTranslationModelPhaseUiState.CHECKING -> 2
    DetailsTranslationModelPhaseUiState.DOWNLOADING -> 3
    DetailsTranslationModelPhaseUiState.WAITING_FOR_SYSTEM -> 4
    DetailsTranslationModelPhaseUiState.TIMED_OUT -> 5
    DetailsTranslationModelPhaseUiState.FAILED -> 6
}

private fun shortLanguageLabel(languageTag: String): String =
    (TranslationLanguages.normalizeLanguageTag(languageTag) ?: languageTag)
        .uppercase(Locale.US)

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1_000L
    val seconds = totalSeconds % 60L
    val totalMinutes = totalSeconds / 60L
    val minutes = totalMinutes % 60L
    val hours = totalMinutes / 60L

    return if (hours > 0L) {
        String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.ROOT, "%d:%02d", totalMinutes, seconds)
    }
}

private fun languageLabel(
    languageTag: String,
    displayLocale: Locale,
): String {
    val locale = Locale.forLanguageTag(languageTag)
    return locale
        .getDisplayLanguage(displayLocale)
        .trim()
        .takeIf(String::isNotEmpty)
        ?: languageTag
}


/**
 * Adds live, presentation-only verbose progress without changing the underlying
 * application-owned Details metadata or triggering any additional lookup work.
 */
internal fun mapPhoneDetailsVerboseProgress(
    playback: PlaybackSnapshot,
    lyricsState: LyricsState,
    currentMonotonicTimeMs: Long,
): DetailsVerboseProgressUiState {
    val positionMs = projectedPlaybackPosition(
        playback = playback,
        currentMonotonicTimeMs = currentMonotonicTimeMs,
    )
    val lyricsDocument = currentLyrics(playback, lyricsState).document

    return DetailsVerboseProgressUiState(
        playbackPositionLabel = playback.track
            ?.durationMs
            ?.let { formatDuration(positionMs) },
        currentLineNumber = lyricsDocument
            ?.currentTimedLineIndex(positionMs)
            ?.plus(1),
    )
}
