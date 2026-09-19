package io.github.whoxamxl.aalyrics.ui.phone.lyrics

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.MarqueeSpacing
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsColors
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsRadius
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsSpacing
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsStroke
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsTypography

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
        artwork?.invoke(this)
    }
}

@Composable
private fun TrackIdentity(
    state: TrackCardUiState,
    modifier: Modifier = Modifier,
) {
    val artist = state.artist?.takeIf { it.isNotBlank() }
    val metadata = listOfNotNull(
        state.providerLabel?.takeIf { it.isNotBlank() },
        state.syncLabel?.takeIf { it.isNotBlank() },
    ).joinToString(separator = " • ")

    Column(modifier = modifier) {
        Column(
            modifier = Modifier.basicMarquee(
                iterations = Int.MAX_VALUE,
                repeatDelayMillis = TrackMarqueePauseMillis,
                initialDelayMillis = TrackMarqueePauseMillis,
                spacing = MarqueeSpacing(AALyricsSpacing.Space32),
                velocity = TrackMarqueeVelocity,
            ),
        ) {
            Text(
                text = state.title,
                style = AALyricsTypography.TrackTitle,
                color = AALyricsColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Clip,
            )
            artist?.let {
                Text(
                    text = it,
                    style = AALyricsTypography.TrackArtist,
                    color = AALyricsColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                )
            }
        }
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
}

private const val TrackMarqueePauseMillis = 4_000
private val TrackMarqueeVelocity = 30.dp
