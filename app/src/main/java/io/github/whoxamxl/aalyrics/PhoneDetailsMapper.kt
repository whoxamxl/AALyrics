package io.github.whoxamxl.aalyrics

import io.github.whoxamxl.aalyrics.core.lyrics.LyricsState
import io.github.whoxamxl.aalyrics.core.model.LyricsDocument
import io.github.whoxamxl.aalyrics.core.model.PlaybackSnapshot
import io.github.whoxamxl.aalyrics.ui.phone.details.DetailsDiagnosticsUiState
import io.github.whoxamxl.aalyrics.ui.phone.details.DetailsLyricsUiState
import io.github.whoxamxl.aalyrics.ui.phone.details.DetailsLyricsUiStatus
import io.github.whoxamxl.aalyrics.ui.phone.details.DetailsScreenUiState
import io.github.whoxamxl.aalyrics.ui.phone.details.DetailsTrackUiState
import io.github.whoxamxl.aalyrics.ui.phone.details.DetailsVerboseProgressUiState
import java.util.Locale

/** Application-owned mapping from canonical runtime facts into Phone Details presentation state. */
internal fun mapPhoneDetailsState(
    playback: PlaybackSnapshot,
    lyricsState: LyricsState,
    verboseDetailsEnabled: Boolean,
    playbackSourceAppInfo: PlaybackSourceAppInfo? = null,
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
            CurrentLyrics(state.lyrics, DetailsLyricsUiStatus.READY)
        is LyricsState.Degraded ->
            CurrentLyrics(state.lyrics, DetailsLyricsUiStatus.READY)
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
