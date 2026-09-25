package io.github.whoxamxl.aalyrics

import io.github.whoxamxl.aalyrics.core.lyrics.LyricsState
import io.github.whoxamxl.aalyrics.core.model.LyricsDocument
import io.github.whoxamxl.aalyrics.core.model.LyricsSyncType
import io.github.whoxamxl.aalyrics.core.model.PlaybackSnapshot
import io.github.whoxamxl.aalyrics.core.model.TimedLyricLine
import io.github.whoxamxl.aalyrics.translation.api.TranslationLanguages
import io.github.whoxamxl.aalyrics.translation.api.TranslationModelPhase
import io.github.whoxamxl.aalyrics.translation.api.TranslationModelState
import io.github.whoxamxl.aalyrics.translation.api.TranslationSettings
import io.github.whoxamxl.aalyrics.translation.core.CanonicalLyricsIdentity
import io.github.whoxamxl.aalyrics.translation.core.LanguageProfile
import io.github.whoxamxl.aalyrics.translation.core.SecondaryActivation
import io.github.whoxamxl.aalyrics.translation.core.TranslationState
import io.github.whoxamxl.aalyrics.ui.phone.lyrics.LyricsScreenUiState
import io.github.whoxamxl.aalyrics.ui.phone.lyrics.LyricsViewportInteractionMode
import io.github.whoxamxl.aalyrics.ui.phone.lyrics.LyricsViewportLineUiState
import io.github.whoxamxl.aalyrics.ui.phone.lyrics.LyricsViewportUiState
import io.github.whoxamxl.aalyrics.ui.phone.lyrics.TrackCardLyricsStatus
import io.github.whoxamxl.aalyrics.ui.phone.lyrics.TrackCardTranslationUiState
import io.github.whoxamxl.aalyrics.ui.phone.lyrics.TrackCardUiState
import java.util.Locale
import kotlin.math.roundToLong

internal fun mapPhoneLyricsState(
    playback: PlaybackSnapshot,
    lyricsState: LyricsState,
    plainLyricsAutoScrollEnabled: Boolean,
    interactionMode: LyricsViewportInteractionMode,
    currentMonotonicTimeMs: Long,
    translationState: TranslationState = TranslationState.Idle,
    translationSettings: TranslationSettings = TranslationSettings(enabled = false),
    translationModelStates: Map<String, TranslationModelState> = emptyMap(),
): LyricsScreenUiState {
    val track = playback.track
    val matchingLyricsState = lyricsState
        .takeIf { state ->
            state !is LyricsState.ForLookup ||
                state.lookup.playbackIdentity == playback.trackIdentity
        }
    val document = when (matchingLyricsState) {
        is LyricsState.Ready -> matchingLyricsState.lyrics
        is LyricsState.Degraded -> matchingLyricsState.lyrics
        else -> null
    }
    val currentCanonicalIdentity = matchingLyricsState
        ?.canonicalLyricsOrNull()
        ?.identity
    val normalizedTargetLanguage =
        TranslationLanguages.normalizeTargetLanguage(translationSettings.targetLanguage)
    val translatedLines = (translationState as? TranslationState.Ready)
        ?.artifact
        ?.takeIf { artifact ->
            translationSettings.enabled &&
                artifact.request.targetLanguage == normalizedTargetLanguage &&
                artifact.request.canonicalLyrics == currentCanonicalIdentity &&
                artifact.lines.size == document?.lines?.size
        }
        ?.lines
    val trackCardTranslationState = mapTrackCardTranslationState(
        settings = translationSettings,
        state = translationState,
        modelStates = translationModelStates,
        currentCanonicalIdentity = currentCanonicalIdentity,
        fallbackSourceLanguage = document?.languageTag,
    )
    val positionMs = projectedPlaybackPosition(playback, currentMonotonicTimeMs)
    val sourceSyncType = document?.syncType ?: LyricsSyncType.PLAIN
    val displaySyncType = if (sourceSyncType == LyricsSyncType.WORD) {
        LyricsSyncType.LINE
    } else {
        sourceSyncType
    }

    return LyricsScreenUiState(
        trackCard = TrackCardUiState(
            title = track?.title ?: "No active track",
            artist = track
                ?.artists
                ?.takeIf { it.isNotEmpty() }
                ?.joinToString(separator = ", "),
            providerLabel = document?.attribution?.displayName,
            syncLabel = document?.let { sourceSyncType.label() },
            lyricsStatus = when (matchingLyricsState) {
                is LyricsState.Loading -> TrackCardLyricsStatus.LOADING
                is LyricsState.Ready,
                is LyricsState.Degraded -> TrackCardLyricsStatus.READY
                is LyricsState.NotFound -> TrackCardLyricsStatus.NOT_FOUND
                is LyricsState.Failed -> TrackCardLyricsStatus.FAILED
                else -> TrackCardLyricsStatus.IDLE
            },
            translation = trackCardTranslationState,
        ),
        viewport = LyricsViewportUiState(
            lines = document
                ?.lines
                .orEmpty()
                .mapIndexed { index, line ->
                    LyricsViewportLineUiState(
                        text = line.text,
                        translatedText = translatedLines
                            ?.get(index)
                            ?.takeIf { it.translated && it.text.isNotBlank() }
                            ?.text,
                    )
                },
            syncType = displaySyncType,
            currentLineIndex = document?.currentTimedLineIndex(positionMs),
            playbackProgress = track
                ?.durationMs
                ?.takeIf { it > 0L }
                ?.let { duration ->
                    (positionMs.toDouble() / duration.toDouble())
                        .coerceIn(0.0, 1.0)
                        .toFloat()
                },
            plainAutoScrollEnabled = plainLyricsAutoScrollEnabled,
            interactionMode = interactionMode,
        ),
    )
}

private fun mapTrackCardTranslationState(
    settings: TranslationSettings,
    state: TranslationState,
    modelStates: Map<String, TranslationModelState>,
    currentCanonicalIdentity: CanonicalLyricsIdentity?,
    fallbackSourceLanguage: String?,
): TrackCardTranslationUiState {
    if (!settings.enabled) return TrackCardTranslationUiState.Off

    val targetLanguage = TranslationLanguages.normalizeTargetLanguage(settings.targetLanguage)

    fun requestMatches(
        requestCanonicalLyrics: CanonicalLyricsIdentity,
        requestTargetLanguage: String,
    ): Boolean =
        requestCanonicalLyrics == currentCanonicalIdentity &&
            requestTargetLanguage == targetLanguage

    fun modelPreparationActive(profile: LanguageProfile?): Boolean {
        val routeLanguages = buildSet {
            add(targetLanguage)
            profile?.primary
                ?.let(TranslationLanguages::normalizeLanguageTag)
                ?.takeIf { languageTag -> TranslationLanguages.isModelSupported(languageTag) }
                ?.let { languageTag -> add(languageTag) }

            val secondaryCandidate = profile
                ?.takeIf { currentProfile ->
                    currentProfile.secondaryActivation == SecondaryActivation.ACTIVE
                }
                ?.secondaryCandidate
            secondaryCandidate
                ?.let(TranslationLanguages::normalizeLanguageTag)
                ?.takeIf { languageTag -> TranslationLanguages.isModelSupported(languageTag) }
                ?.let { languageTag -> add(languageTag) }
        }

        return routeLanguages.any { languageTag ->
            when (modelStates[languageTag]?.phase) {
                TranslationModelPhase.DOWNLOADING,
                TranslationModelPhase.WAITING_FOR_SYSTEM -> true
                else -> false
            }
        }
    }

    when (state) {
        is TranslationState.Ready -> {
            val artifact = state.artifact
            if (
                requestMatches(
                    artifact.request.canonicalLyrics,
                    artifact.request.targetLanguage,
                )
            ) {
                val sourceLanguage = artifact.lines
                    .firstOrNull { line ->
                        line.translated && !line.sourceLanguage.isNullOrBlank()
                    }
                    ?.sourceLanguage
                    ?: artifact.profile.primary
                    ?: fallbackSourceLanguage
                return TrackCardTranslationUiState.Ready(
                    sourceLanguageLabel = shortLanguageLabel(sourceLanguage),
                    targetLanguageLabel = shortLanguageLabel(artifact.request.targetLanguage),
                )
            }
        }

        is TranslationState.NotRequired -> {
            if (
                requestMatches(
                    state.request.canonicalLyrics,
                    state.request.targetLanguage,
                )
            ) {
                return TrackCardTranslationUiState.NotRequired
            }
        }

        is TranslationState.Translating -> {
            if (
                requestMatches(
                    state.request.canonicalLyrics,
                    state.request.targetLanguage,
                )
            ) {
                return if (modelPreparationActive(state.profile)) {
                    TrackCardTranslationUiState.DownloadingModels
                } else {
                    TrackCardTranslationUiState.Translating
                }
            }
        }

        is TranslationState.Failed -> {
            if (
                requestMatches(
                    state.request.canonicalLyrics,
                    state.request.targetLanguage,
                )
            ) {
                return TrackCardTranslationUiState.Failed
            }
        }

        TranslationState.Disabled,
        TranslationState.Idle -> Unit
    }

    return TrackCardTranslationUiState.Enabled
}

private fun shortLanguageLabel(languageTag: String?): String =
    TranslationLanguages.normalizeLanguageTag(languageTag)
        ?.uppercase(Locale.US)
        ?: "AUTO"

internal fun projectedPlaybackPosition(
    playback: PlaybackSnapshot,
    currentMonotonicTimeMs: Long,
): Long {
    val base = playback.positionMs
    if (!playback.isPlaying || playback.playbackRate <= 0f) {
        return playback.track?.durationMs?.let { base.coerceIn(0L, it) } ?: base
    }

    val elapsedMs = playback.positionUpdatedAtMonotonicMs
        ?.let { updatedAt -> (currentMonotonicTimeMs - updatedAt).coerceAtLeast(0L) }
        ?: 0L
    val projected = base + (elapsedMs * playback.playbackRate)
        .toDouble()
        .roundToLong()

    return playback.track?.durationMs
        ?.let { projected.coerceIn(0L, it) }
        ?: projected.coerceAtLeast(0L)
}

internal fun LyricsDocument.currentTimedLineIndex(positionMs: Long): Int? =
    lines.indices
        .filter { index ->
            val line = lines[index]
            line is TimedLyricLine && line.startMs <= positionMs
        }
        .lastOrNull()

private fun LyricsSyncType.label(): String = when (this) {
    LyricsSyncType.PLAIN -> "Plain"
    LyricsSyncType.LINE -> "Line synced"
    LyricsSyncType.WORD -> "Word synced"
}
