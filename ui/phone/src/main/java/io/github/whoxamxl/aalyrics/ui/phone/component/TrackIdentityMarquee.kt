package io.github.whoxamxl.aalyrics.ui.phone.component

import androidx.compose.foundation.MarqueeSpacing
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsColors
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsSpacing
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsTypography

/**
 * One-line title/artist identity using row-aware AALyrics marquee behavior.
 *
 * Only an overflowing line moves when its sibling still fits. When both lines overflow, the
 * identity keeps the established synchronized block marquee so both leading edges stay aligned.
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
    val artistText = artist?.takeIf { it.isNotBlank() }
    val textMeasurer = rememberTextMeasurer()

    BoxWithConstraints(modifier = modifier) {
        val availableWidthPx = constraints.maxWidth
        val titleOverflows = if (constraints.hasBoundedWidth && availableWidthPx > 0) {
            remember(title, titleStyle, availableWidthPx, textMeasurer) {
                textMeasurer.singleLineOverflows(
                    text = title,
                    style = titleStyle,
                    maxWidthPx = availableWidthPx,
                )
            }
        } else {
            false
        }
        val artistOverflows = if (
            constraints.hasBoundedWidth &&
            availableWidthPx > 0 &&
            artistText != null
        ) {
            remember(artistText, artistStyle, availableWidthPx, textMeasurer) {
                textMeasurer.singleLineOverflows(
                    text = artistText,
                    style = artistStyle,
                    maxWidthPx = availableWidthPx,
                )
            }
        } else {
            false
        }

        val marqueeMode = trackIdentityMarqueeMode(
            titleOverflows = titleOverflows,
            artistOverflows = artistOverflows,
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (marqueeMode == TrackIdentityMarqueeMode.SYNCHRONIZED) {
                        Modifier.identityMarqueeMotion()
                    } else {
                        Modifier
                    },
                ),
        ) {
            Text(
                text = title,
                style = titleStyle,
                color = titleColor,
                maxLines = 1,
                overflow = TextOverflow.Clip,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (marqueeMode == TrackIdentityMarqueeMode.TITLE_ONLY) {
                            Modifier.identityMarqueeMotion()
                        } else {
                            Modifier
                        },
                    ),
            )

            artistText?.let { text ->
                Text(
                    text = text,
                    style = artistStyle,
                    color = artistColor,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (marqueeMode == TrackIdentityMarqueeMode.ARTIST_ONLY) {
                                Modifier.identityMarqueeMotion()
                            } else {
                                Modifier
                            },
                        ),
                )
            }
        }
    }
}

internal enum class TrackIdentityMarqueeMode {
    STATIC,
    TITLE_ONLY,
    ARTIST_ONLY,
    SYNCHRONIZED,
}

internal fun trackIdentityMarqueeMode(
    titleOverflows: Boolean,
    artistOverflows: Boolean,
): TrackIdentityMarqueeMode = when {
    titleOverflows && artistOverflows -> TrackIdentityMarqueeMode.SYNCHRONIZED
    titleOverflows -> TrackIdentityMarqueeMode.TITLE_ONLY
    artistOverflows -> TrackIdentityMarqueeMode.ARTIST_ONLY
    else -> TrackIdentityMarqueeMode.STATIC
}

private fun TextMeasurer.singleLineOverflows(
    text: String,
    style: TextStyle,
    maxWidthPx: Int,
): Boolean = measure(
    text = AnnotatedString(text),
    style = style,
    overflow = TextOverflow.Clip,
    softWrap = false,
    maxLines = 1,
    constraints = Constraints(maxWidth = maxWidthPx),
).didOverflowWidth

private fun Modifier.identityMarqueeMotion(): Modifier = basicMarquee(
    iterations = Int.MAX_VALUE,
    repeatDelayMillis = TrackMarqueePauseMillis,
    initialDelayMillis = TrackMarqueePauseMillis,
    spacing = MarqueeSpacing(AALyricsSpacing.Space32),
    velocity = TrackMarqueeVelocity,
)

private const val TrackMarqueePauseMillis = 4_000
private val TrackMarqueeVelocity = 30.dp
