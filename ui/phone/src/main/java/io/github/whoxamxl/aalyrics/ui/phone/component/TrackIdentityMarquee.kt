package io.github.whoxamxl.aalyrics.ui.phone.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
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
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

/**
 * One-line title/artist identity using row-aware AALyrics marquee behavior.
 *
 * Only an overflowing line moves when its sibling still fits. When both lines overflow, the
 * identity keeps one shared marquee offset so both leading edges stay aligned. Overflowing
 * marquee content can also be dragged horizontally within one bounded marquee cycle; auto motion
 * resumes from the released position after a short pause.
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
        val titleWidthPx = remember(title, titleStyle, textMeasurer) {
            textMeasurer.singleLineWidthPx(
                text = title,
                style = titleStyle,
            )
        }
        val artistWidthPx = artistText?.let { text ->
            remember(text, artistStyle, textMeasurer) {
                textMeasurer.singleLineWidthPx(
                    text = text,
                    style = artistStyle,
                )
            }
        } ?: 0

        val titleOverflows = constraints.hasBoundedWidth &&
            availableWidthPx > 0 &&
            titleWidthPx > availableWidthPx
        val artistOverflows = constraints.hasBoundedWidth &&
            availableWidthPx > 0 &&
            artistWidthPx > availableWidthPx

        when (
            trackIdentityMarqueeMode(
                titleOverflows = titleOverflows,
                artistOverflows = artistOverflows,
            )
        ) {
            TrackIdentityMarqueeMode.STATIC -> {
                TrackIdentityStatic(
                    title = title,
                    artist = artistText,
                    titleStyle = titleStyle,
                    artistStyle = artistStyle,
                    titleColor = titleColor,
                    artistColor = artistColor,
                )
            }

            TrackIdentityMarqueeMode.TITLE_ONLY -> {
                Column(modifier = Modifier.fillMaxWidth()) {
                    InteractiveMarqueeViewport(
                        contentWidthPx = titleWidthPx,
                        contentKey = title,
                    ) { contentModifier ->
                        TrackIdentityText(
                            text = title,
                            style = titleStyle,
                            color = titleColor,
                            modifier = contentModifier,
                        )
                    }

                    artistText?.let { text ->
                        TrackIdentityText(
                            text = text,
                            style = artistStyle,
                            color = artistColor,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            TrackIdentityMarqueeMode.ARTIST_ONLY -> {
                Column(modifier = Modifier.fillMaxWidth()) {
                    TrackIdentityText(
                        text = title,
                        style = titleStyle,
                        color = titleColor,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    artistText?.let { text ->
                        InteractiveMarqueeViewport(
                            contentWidthPx = artistWidthPx,
                            contentKey = text,
                        ) { contentModifier ->
                            TrackIdentityText(
                                text = text,
                                style = artistStyle,
                                color = artistColor,
                                modifier = contentModifier,
                            )
                        }
                    }
                }
            }

            TrackIdentityMarqueeMode.SYNCHRONIZED -> {
                val synchronizedWidthPx = max(titleWidthPx, artistWidthPx)
                InteractiveMarqueeViewport(
                    contentWidthPx = synchronizedWidthPx,
                    contentKey = title to artistText,
                ) { contentModifier ->
                    Column(modifier = contentModifier) {
                        TrackIdentityText(
                            text = title,
                            style = titleStyle,
                            color = titleColor,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        artistText?.let { text ->
                            TrackIdentityText(
                                text = text,
                                style = artistStyle,
                                color = artistColor,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TrackIdentityStatic(
    title: String,
    artist: String?,
    titleStyle: TextStyle,
    artistStyle: TextStyle,
    titleColor: Color,
    artistColor: Color,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        TrackIdentityText(
            text = title,
            style = titleStyle,
            color = titleColor,
            modifier = Modifier.fillMaxWidth(),
        )
        artist?.let { text ->
            TrackIdentityText(
                text = text,
                style = artistStyle,
                color = artistColor,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun TrackIdentityText(
    text: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier,
) {
    Text(
        text = text,
        style = style,
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Clip,
        modifier = modifier,
    )
}

@Composable
private fun InteractiveMarqueeViewport(
    contentWidthPx: Int,
    contentKey: Any?,
    content: @Composable (Modifier) -> Unit,
) {
    val density = LocalDensity.current
    val contentWidthDp = with(density) { contentWidthPx.toDp() }
    val gapPx = with(density) { AALyricsSpacing.Space32.toPx() }
    val velocityPxPerSecond = with(density) { TrackMarqueeVelocity.toPx() }
    val cycleDistancePx = contentWidthPx.toFloat() + gapPx

    var offsetPx by remember(contentKey, cycleDistancePx) { mutableFloatStateOf(0f) }
    var isDragging by remember(contentKey, cycleDistancePx) { mutableStateOf(false) }
    var restartToken by remember(contentKey, cycleDistancePx) { mutableIntStateOf(0) }
    var nextResumeDelayMillis by remember(contentKey, cycleDistancePx) {
        mutableLongStateOf(TrackMarqueePauseMillis.toLong())
    }

    val dragState = rememberDraggableState { dragDeltaPx ->
        offsetPx = manualMarqueeOffsetPx(
            currentOffsetPx = offsetPx,
            dragDeltaPx = dragDeltaPx,
            cycleDistancePx = cycleDistancePx,
        )
    }

    LaunchedEffect(contentKey, cycleDistancePx, restartToken) {
        if (isDragging) return@LaunchedEffect

        delay(nextResumeDelayMillis)

        while (true) {
            val remainingDistancePx = (cycleDistancePx - offsetPx).coerceAtLeast(0f)
            if (remainingDistancePx > TrackMarqueePositionEpsilonPx) {
                animate(
                    initialValue = offsetPx,
                    targetValue = cycleDistancePx,
                    animationSpec = tween(
                        durationMillis = marqueeTravelDurationMillis(
                            distancePx = remainingDistancePx,
                            velocityPxPerSecond = velocityPxPerSecond,
                        ),
                        easing = LinearEasing,
                    ),
                ) { value, _ ->
                    offsetPx = value
                }
            }

            offsetPx = 0f
            nextResumeDelayMillis = TrackMarqueePauseMillis.toLong()
            delay(TrackMarqueePauseMillis.toLong())
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clipToBounds()
            .draggable(
                state = dragState,
                orientation = Orientation.Horizontal,
                onDragStarted = {
                    isDragging = true
                    restartToken += 1
                },
                onDragStopped = {
                    isDragging = false
                    nextResumeDelayMillis = TrackMarqueeManualResumeDelayMillis
                    restartToken += 1
                },
            ),
    ) {
        Row(
            modifier = Modifier
                .wrapContentWidth(
                    align = Alignment.Start,
                    unbounded = true,
                )
                .graphicsLayer {
                    translationX = -offsetPx
                },
        ) {
            content(Modifier.width(contentWidthDp))
            Spacer(Modifier.width(AALyricsSpacing.Space32))
            content(
                Modifier
                    .width(contentWidthDp)
                    .clearAndSetSemantics { },
            )
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

internal fun manualMarqueeOffsetPx(
    currentOffsetPx: Float,
    dragDeltaPx: Float,
    cycleDistancePx: Float,
): Float {
    require(cycleDistancePx >= 0f) { "Marquee cycle distance must be non-negative" }
    return (currentOffsetPx - dragDeltaPx).coerceIn(0f, cycleDistancePx)
}

internal fun marqueeTravelDurationMillis(
    distancePx: Float,
    velocityPxPerSecond: Float,
): Int {
    require(velocityPxPerSecond > 0f) { "Marquee velocity must be positive" }
    if (distancePx <= 0f) return 1
    return ((distancePx / velocityPxPerSecond) * 1_000f)
        .roundToInt()
        .coerceAtLeast(1)
}

private fun TextMeasurer.singleLineWidthPx(
    text: String,
    style: TextStyle,
): Int = measure(
    text = AnnotatedString(text),
    style = style,
    overflow = TextOverflow.Clip,
    softWrap = false,
    maxLines = 1,
    constraints = Constraints(),
).size.width

private const val TrackMarqueePauseMillis = 4_000
private const val TrackMarqueeManualResumeDelayMillis = 400L
private const val TrackMarqueePositionEpsilonPx = 0.5f
private val TrackMarqueeVelocity = 30.dp
