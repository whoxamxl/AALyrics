package io.github.whoxamxl.aalyrics.ui.phone.lyrics

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.whoxamxl.aalyrics.ui.phone.R
import io.github.whoxamxl.aalyrics.ui.designsystem.icon.AALyricsIcons
import io.github.whoxamxl.aalyrics.ui.designsystem.component.AALyricsArtworkFallback
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsColors
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsRadius
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsSpacing
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsStroke
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsTypography
import io.github.whoxamxl.aalyrics.ui.phone.component.TrackIdentityMarquee

/**
 * Compact current-track identity for the Lyrics destination.
 *
 * Artwork loading/decoding stays outside this component. Callers may provide already-renderable
 * artwork content; otherwise a neutral artwork placeholder is shown.
 */
@Composable
fun TrackCard(
    state: TrackCardUiState,
    modifier: Modifier = Modifier,
    onTranslationRetry: (() -> Unit)? = null,
    artwork: (@Composable BoxScope.() -> Unit)? = null,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AALyricsRadius.Radius16),
        color = AALyricsColors.BackgroundSurfaceStrong,
        border = BorderStroke(
            width = AALyricsStroke.Thin,
            color = AALyricsColors.BorderSoft,
        ),
    ) {
        Row(
            modifier = Modifier.padding(AALyricsSpacing.Space12),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TrackArtwork(artwork = artwork)
            Spacer(Modifier.width(AALyricsSpacing.Space12))
            TrackIdentity(
                state = state,
                onTranslationRetry = onTranslationRetry,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun TrackArtwork(
    artwork: (@Composable BoxScope.() -> Unit)?,
) {
    Box(
        modifier = Modifier
            .size(AALyricsSpacing.Space64)
            .clip(RoundedCornerShape(AALyricsRadius.Radius12))
            .background(AALyricsColors.OverlaySoft),
        contentAlignment = Alignment.Center,
    ) {
        if (artwork != null) {
            artwork.invoke(this)
        } else {
            AALyricsArtworkFallback()
        }
    }
}

@Composable
private fun TrackIdentity(
    state: TrackCardUiState,
    onTranslationRetry: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val artist = state.artist?.takeIf { it.isNotBlank() }
    val metadata = listOfNotNull(
        state.providerLabel?.takeIf { it.isNotBlank() },
        state.syncLabel?.takeIf { it.isNotBlank() },
    ).joinToString(separator = " • ")

    Column(modifier = modifier) {
        TrackIdentityMarquee(
            title = state.title,
            artist = artist,
        )
        when (state.lyricsStatus) {
            TrackCardLyricsStatus.LOADING -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(12.dp),
                        color = AALyricsColors.AccentCyan,
                        strokeWidth = 1.5.dp,
                    )
                    Spacer(Modifier.width(AALyricsSpacing.Space4))
                    Text(
                        text = stringResource(R.string.track_card_lyrics_loading),
                        style = AALyricsTypography.Label,
                        color = AALyricsColors.AccentCyan,
                        maxLines = 1,
                    )
                }
            }

            TrackCardLyricsStatus.NOT_FOUND -> {
                Text(
                    text = stringResource(R.string.track_card_lyrics_not_found),
                    style = AALyricsTypography.Label,
                    color = AALyricsColors.TextSecondary,
                    maxLines = 1,
                )
            }

            TrackCardLyricsStatus.FAILED -> {
                Text(
                    text = stringResource(R.string.track_card_lyrics_failed),
                    style = AALyricsTypography.Label,
                    color = AALyricsColors.Error,
                    maxLines = 1,
                )
            }

            TrackCardLyricsStatus.READY -> {
                if (metadata.isNotEmpty()) {
                    Text(
                        text = metadata,
                        style = AALyricsTypography.Label,
                        color = AALyricsColors.AccentCyan,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            TrackCardLyricsStatus.IDLE -> Unit
        }

        TranslationStatusRow(
            state = state.translation,
            onRetry = onTranslationRetry,
        )
    }
}

@Composable
private fun TranslationStatusRow(
    state: TrackCardTranslationUiState,
    onRetry: (() -> Unit)?,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = TranslationStatusRowMinHeight),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when (state) {
            TrackCardTranslationUiState.Off -> Unit

            TrackCardTranslationUiState.On -> TranslationStatusText(
                text = stringResource(R.string.track_card_translation_on),
                color = AALyricsColors.AccentBlue,
            )

            TrackCardTranslationUiState.DownloadingModels -> TranslationLoadingStatus(
                text = stringResource(R.string.track_card_translation_downloading_models),
                color = AALyricsColors.AccentBlue,
            )

            TrackCardTranslationUiState.Translating -> TranslationLoadingStatus(
                text = stringResource(R.string.track_card_translation_translating),
                color = AALyricsColors.AccentBlue,
            )

            is TrackCardTranslationUiState.Ready -> TranslationStatusText(
                text = stringResource(
                    R.string.track_card_translation_route,
                    state.sourceLanguageLabel,
                    state.targetLanguageLabel,
                ),
                color = AALyricsColors.Success,
            )

            TrackCardTranslationUiState.NotRequired -> TranslationStatusText(
                text = stringResource(R.string.track_card_translation_not_required),
                color = AALyricsColors.TextSecondary,
            )

            TrackCardTranslationUiState.Failed -> {
                TranslationStatusText(
                    text = stringResource(R.string.track_card_translation_failed),
                    color = AALyricsColors.Error,
                    modifier = Modifier.weight(1f),
                )
                onRetry?.let { retry ->
                    Row(
                        modifier = Modifier
                            .clickable(
                                role = Role.Button,
                                onClick = retry,
                            )
                            .padding(
                                horizontal = AALyricsSpacing.Space8,
                                vertical = 2.dp,
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = AALyricsIcons.Retry,
                            contentDescription = null,
                            tint = AALyricsColors.AccentCyan,
                            modifier = Modifier.size(AALyricsSpacing.Space16),
                        )
                        Spacer(Modifier.width(AALyricsSpacing.Space4))
                        Text(
                            text = stringResource(R.string.track_card_translation_retry),
                            style = AALyricsTypography.Label,
                            color = AALyricsColors.AccentCyan,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TranslationLoadingStatus(
    text: String,
    color: androidx.compose.ui.graphics.Color,
) {
    CircularProgressIndicator(
        modifier = Modifier.size(12.dp),
        color = color,
        strokeWidth = 1.5.dp,
    )
    Spacer(Modifier.width(AALyricsSpacing.Space4))
    TranslationStatusText(
        text = text,
        color = color,
    )
}

@Composable
private fun TranslationStatusText(
    text: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = AALyricsTypography.Label,
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

private val TranslationStatusRowMinHeight = 20.dp

