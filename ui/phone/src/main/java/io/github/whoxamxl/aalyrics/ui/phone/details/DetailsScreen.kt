package io.github.whoxamxl.aalyrics.ui.phone.details

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.whoxamxl.aalyrics.core.model.LyricsSyncType
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsColors
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsRadius
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsSpacing
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsStroke
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsTypography
import io.github.whoxamxl.aalyrics.ui.phone.R

@Composable
fun DetailsScreen(
    state: DetailsScreenUiState,
    rootResetKey: Int = 0,
    modifier: Modifier = Modifier,
    bottomOverlayInset: Dp = 0.dp,
) {
    val scrollState = rememberScrollState()

    LaunchedEffect(rootResetKey) {
        scrollState.scrollTo(0)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(
                start = AALyricsSpacing.Space16,
                top = AALyricsSpacing.Space16,
                end = AALyricsSpacing.Space16,
                bottom = bottomOverlayInset + AALyricsSpacing.Space16,
            ),
    ) {
        Text(
            text = stringResource(R.string.details_title),
            style = AALyricsTypography.LyricsSupporting,
            color = AALyricsColors.TextPrimary,
        )

        Spacer(Modifier.height(AALyricsSpacing.Space20))

        DetailsSection(title = stringResource(R.string.details_section_track)) {
            val track = state.track
            if (track == null) {
                DetailsStatusRow(stringResource(R.string.details_no_active_track))
            } else {
                DetailsValueRow(
                    label = stringResource(R.string.details_track_title),
                    value = track.title,
                )
                track.artist?.let {
                    DetailsDivider()
                    DetailsValueRow(
                        label = stringResource(R.string.details_track_artist),
                        value = it,
                    )
                }
                track.album?.let {
                    DetailsDivider()
                    DetailsValueRow(
                        label = stringResource(R.string.details_track_album),
                        value = it,
                    )
                }
                track.durationLabel?.let { durationLabel ->
                    DetailsDivider()
                    val verboseProgress = state.verboseProgress
                    DetailsValueRow(
                        label = detailsVerboseLabel(
                            baseLabel = stringResource(R.string.details_track_duration),
                            verbose = verboseProgress != null,
                        ),
                        value = verboseProgress
                            ?.playbackPositionLabel
                            ?.let { position -> "$position / $durationLabel" }
                            ?: durationLabel,
                    )
                }
                track.playbackSourceLabel?.let {
                    DetailsDivider()
                    DetailsValueRow(
                        label = stringResource(R.string.details_playback_source),
                        value = it,
                    )
                }
            }
        }

        Spacer(Modifier.height(AALyricsSpacing.Space20))

        DetailsSection(title = stringResource(R.string.details_section_lyrics)) {
            val lyrics = state.lyrics
            if (lyrics == null) {
                DetailsStatusRow(lyricsStatusLabel(state.lyricsStatus))
            } else {
                lyrics.providerDisplayName?.let {
                    DetailsValueRow(
                        label = stringResource(R.string.details_lyrics_provider),
                        value = it,
                    )
                    DetailsDivider()
                }
                DetailsValueRow(
                    label = stringResource(R.string.details_lyrics_sync_type),
                    value = syncTypeLabel(lyrics.syncType),
                )
                lyrics.languageLabel?.let {
                    DetailsDivider()
                    DetailsValueRow(
                        label = stringResource(R.string.details_lyrics_language),
                        value = it,
                    )
                }
                DetailsDivider()
                val verboseProgress = state.verboseProgress
                DetailsValueRow(
                    label = detailsVerboseLabel(
                        baseLabel = stringResource(R.string.details_lyrics_lines),
                        verbose = verboseProgress != null,
                    ),
                    value = if (verboseProgress != null) {
                        val currentLine = verboseProgress.currentLineNumber
                            ?.toString()
                            ?: stringResource(R.string.details_verbose_unknown)
                        "$currentLine / ${lyrics.lineCount}"
                    } else {
                        lyrics.lineCount.toString()
                    },
                )
            }
        }

        state.diagnostics?.let { diagnostics ->
            Spacer(Modifier.height(AALyricsSpacing.Space20))

            DetailsSection(
                title = stringResource(R.string.details_section_diagnostics),
            ) {
                var hasValue = false

                diagnostics.appPackageName?.let {
                    hasValue = true
                    DetailsValueRow(
                        label = stringResource(R.string.details_app_package),
                        value = it,
                    )
                }

                diagnostics.providerId?.let {
                    if (hasValue) DetailsDivider()
                    hasValue = true
                    DetailsValueRow(
                        label = stringResource(R.string.details_provider_id),
                        value = it,
                    )
                }

                diagnostics.sourceId?.let {
                    if (hasValue) DetailsDivider()
                    hasValue = true
                    DetailsValueRow(
                        label = stringResource(R.string.details_source_id),
                        value = it,
                    )
                }

                if (diagnostics.trackReferences.isNotEmpty()) {
                    if (hasValue) DetailsDivider()
                    hasValue = true
                    DetailsValueRow(
                        label = stringResource(R.string.details_track_references),
                        value = diagnostics.trackReferences.joinToString(separator = "\n"),
                    )
                }

                if (!hasValue) {
                    DetailsStatusRow(
                        stringResource(R.string.details_no_diagnostics),
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = AALyricsTypography.Label,
            color = AALyricsColors.AccentCyan,
            modifier = Modifier.padding(
                start = AALyricsSpacing.Space4,
                bottom = AALyricsSpacing.Space8,
            ),
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(AALyricsRadius.Radius16),
            color = AALyricsColors.BackgroundSurface,
            border = BorderStroke(
                width = AALyricsStroke.Thin,
                color = AALyricsColors.BorderSoft,
            ),
        ) {
            Column(content = content)
        }
    }
}

@Composable
private fun DetailsValueRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = AALyricsSpacing.Space48)
            .padding(
                horizontal = AALyricsSpacing.Space16,
                vertical = AALyricsSpacing.Space12,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = AALyricsTypography.TrackArtist,
            color = AALyricsColors.TextSecondary,
            modifier = Modifier.weight(0.42f),
        )
        Text(
            text = value,
            style = AALyricsTypography.AppTitle,
            color = AALyricsColors.TextPrimary,
            textAlign = TextAlign.End,
            modifier = Modifier
                .weight(0.58f)
                .padding(start = AALyricsSpacing.Space12),
        )
    }
}

@Composable
private fun DetailsStatusRow(text: String) {
    Text(
        text = text,
        style = AALyricsTypography.TrackArtist,
        color = AALyricsColors.TextSecondary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(AALyricsSpacing.Space16),
    )
}

@Composable
private fun DetailsDivider() {
    androidx.compose.material3.HorizontalDivider(
        color = AALyricsColors.BorderSoft,
        thickness = AALyricsStroke.Thin,
        modifier = Modifier.padding(horizontal = AALyricsSpacing.Space16),
    )
}

@Composable
private fun lyricsStatusLabel(status: DetailsLyricsUiStatus): String = when (status) {
    DetailsLyricsUiStatus.LOADING -> stringResource(R.string.details_lyrics_loading)
    DetailsLyricsUiStatus.READY -> stringResource(R.string.details_lyrics_unavailable)
    DetailsLyricsUiStatus.NOT_FOUND -> stringResource(R.string.details_lyrics_not_found)
    DetailsLyricsUiStatus.FAILED -> stringResource(R.string.details_lyrics_failed)
    DetailsLyricsUiStatus.UNAVAILABLE -> stringResource(R.string.details_lyrics_unavailable)
}

@Composable
private fun syncTypeLabel(syncType: LyricsSyncType): String = when (syncType) {
    LyricsSyncType.PLAIN -> stringResource(R.string.details_sync_plain)
    LyricsSyncType.LINE -> stringResource(R.string.details_sync_line)
    LyricsSyncType.WORD -> stringResource(R.string.details_sync_word)
}

@Composable
private fun detailsVerboseLabel(
    baseLabel: String,
    verbose: Boolean,
): String = if (verbose) {
    stringResource(R.string.details_verbose_label, baseLabel)
} else {
    baseLabel
}
