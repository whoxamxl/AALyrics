package io.github.whoxamxl.aalyrics.ui.phone.component

import androidx.compose.foundation.MarqueeSpacing
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsColors
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsSpacing
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsTypography

/**
 * Synchronized one-line title/artist identity with the established delayed AALyrics marquee.
 *
 * The two lines move as one block so their leading edges remain aligned throughout the cycle.
 */
@Composable
internal fun TrackIdentityMarquee(
    title: String,
    artist: String?,
    modifier: Modifier = Modifier,
    titleStyle: TextStyle = AALyricsTypography.TrackTitle,
    artistStyle: TextStyle = AALyricsTypography.TrackArtist,
    titleColor: Color = AALyricsColors.TextPrimary,
    artistColor: Color = AALyricsColors.TextSecondary,
) {
    Column(
        modifier = modifier.basicMarquee(
            iterations = Int.MAX_VALUE,
            repeatDelayMillis = TrackMarqueePauseMillis,
            initialDelayMillis = TrackMarqueePauseMillis,
            spacing = MarqueeSpacing(AALyricsSpacing.Space32),
            velocity = TrackMarqueeVelocity,
        ),
    ) {
        Text(
            text = title,
            style = titleStyle,
            color = titleColor,
            maxLines = 1,
            overflow = TextOverflow.Clip,
        )
        artist?.takeIf { it.isNotBlank() }?.let {
            Text(
                text = it,
                style = artistStyle,
                color = artistColor,
                maxLines = 1,
                overflow = TextOverflow.Clip,
            )
        }
    }
}

private const val TrackMarqueePauseMillis = 4_000
private val TrackMarqueeVelocity = 30.dp
