package io.github.whoxamxl.aalyrics.ui.phone.lyrics

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.whoxamxl.aalyrics.core.model.LyricsSyncType
import io.github.whoxamxl.aalyrics.ui.designsystem.icon.AALyricsIcons
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsColors
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsSpacing
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsStroke
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsTypography
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * Responsive lyrics-reading surface shared by WORD, LINE, and PLAIN presentation states.
 *
 * Runtime timing and media ownership stay outside this component. The viewport renders
 * presentation-ready rows and reports when direct user scrolling takes ownership.
 */
@Composable
fun LyricsViewport(
    state: LyricsViewportUiState,
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
    onInteractionModeChange: (LyricsViewportInteractionMode) -> Unit = {},
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val viewportHeightPx = with(density) { maxHeight.roundToPx() }
        val minimumContentPaddingPx = with(density) { AALyricsSpacing.Space20.roundToPx() }
        val rowSpacingPx = with(density) { AALyricsSpacing.Space16.roundToPx() }
        val hasOpeningFocusRow =
            state.syncType != LyricsSyncType.PLAIN && state.lines.isNotEmpty()
        val openingFocusRowHeightPx = if (hasOpeningFocusRow) {
            with(density) { StableLyricsLineHeight.roundToPx() }
        } else {
            0
        }
        val lineHeights = remember(state.lines) { mutableStateMapOf<Int, Int>() }
        val canonicalRows = state.lines.map { it.text to it.words }
        val lastLineHeightPx = lineHeights[state.lines.lastIndex] ?: 0
        val openingContentStartPx = (viewportHeightPx * TopEdgeFadeFraction).roundToInt()
        val endingBoundaryStartPx = (
            viewportHeightPx * EndBoundaryStartFraction
            ).roundToInt()
        val topContentPaddingPx = if (hasOpeningFocusRow) {
            (
                openingContentStartPx -
                    openingFocusRowHeightPx -
                    rowSpacingPx
                ).coerceAtLeast(minimumContentPaddingPx)
        } else {
            openingContentStartPx.coerceAtLeast(minimumContentPaddingPx)
        }
        val bottomContentPaddingPx = (
            endingBoundaryStartPx - lastLineHeightPx
            ).coerceAtLeast(minimumContentPaddingPx)
        val topContentPadding = with(density) { topContentPaddingPx.toDp() }
        val bottomContentPadding = with(density) { bottomContentPaddingPx.toDp() }
        val scope = rememberCoroutineScope()

        val targetFocusIndex = timedFocusIndex(state)
        val animatedFocusIndex = remember(canonicalRows, state.syncType) {
            Animatable(targetFocusIndex)
        }

        val latestMode = rememberUpdatedState(state.interactionMode)
        val latestModeChange = rememberUpdatedState(onInteractionModeChange)
        val userScrollConnection = remember {
            object : NestedScrollConnection {
                override fun onPreScroll(
                    available: Offset,
                    source: NestedScrollSource,
                ): Offset {
                    if (
                        source == NestedScrollSource.UserInput &&
                        available.y != 0f &&
                        latestMode.value != LyricsViewportInteractionMode.BROWSE
                    ) {
                        latestModeChange.value(LyricsViewportInteractionMode.BROWSE)
                    }
                    return Offset.Zero
                }
            }
        }

        val measuredHeightSignature = lineHeights.entries.sumOf { (index, height) ->
            (index + 1) * 31 + height
        } + (openingFocusRowHeightPx * 37)

        val syncedPlaybackTargetScrollPx = syncedScrollPxForFocusIndex(
            focusIndex = targetFocusIndex,
            lineHeights = lineHeights,
            openingFocusRowHeightPx = openingFocusRowHeightPx,
            rowSpacingPx = rowSpacingPx,
            topContentPaddingPx = topContentPaddingPx,
            viewportHeightPx = viewportHeightPx,
        )?.coerceIn(0, scrollState.maxValue)
        val plainTargetScrollPx = plainTargetScrollPx(
            state = state,
            maxScrollPx = scrollState.maxValue,
        )
        val playbackTargetScrollPx = when (state.syncType) {
            LyricsSyncType.PLAIN -> plainTargetScrollPx
            LyricsSyncType.LINE,
            LyricsSyncType.WORD -> syncedPlaybackTargetScrollPx
        }

        LaunchedEffect(targetFocusIndex, state.syncType, canonicalRows) {
            if (state.syncType == LyricsSyncType.PLAIN) return@LaunchedEffect
            if (abs(targetFocusIndex - animatedFocusIndex.value) > FocusSnapJumpRows) {
                animatedFocusIndex.snapTo(targetFocusIndex)
            } else {
                animatedFocusIndex.animateTo(
                    targetValue = targetFocusIndex,
                    animationSpec = spring(
                        dampingRatio = FocusSpringDampingRatio,
                        stiffness = FocusSpringStiffness,
                        visibilityThreshold = FocusSpringVisibilityThreshold,
                    ),
                )
            }
        }

        LaunchedEffect(
            state.interactionMode,
            state.syncType,
            measuredHeightSignature,
            scrollState.maxValue,
            viewportHeightPx,
            topContentPaddingPx,
        ) {
            if (
                state.interactionMode != LyricsViewportInteractionMode.FOLLOW ||
                state.syncType == LyricsSyncType.PLAIN
            ) {
                return@LaunchedEffect
            }

            snapshotFlow { animatedFocusIndex.value }.collect { focusIndex ->
                val target = syncedScrollPxForFocusIndex(
                    focusIndex = focusIndex,
                    lineHeights = lineHeights,
                    openingFocusRowHeightPx = openingFocusRowHeightPx,
                    rowSpacingPx = rowSpacingPx,
                    topContentPaddingPx = topContentPaddingPx,
                    viewportHeightPx = viewportHeightPx,
                ) ?: return@collect
                scrollState.scrollTo(target.coerceIn(0, scrollState.maxValue))
            }
        }

        LaunchedEffect(
            state.interactionMode,
            plainTargetScrollPx,
            scrollState.maxValue,
            state.syncType,
        ) {
            if (
                state.interactionMode != LyricsViewportInteractionMode.FOLLOW ||
                state.syncType != LyricsSyncType.PLAIN ||
                plainTargetScrollPx == null
            ) {
                return@LaunchedEffect
            }

            val target = plainTargetScrollPx.coerceIn(0, scrollState.maxValue)
            if (abs(scrollState.value - target) > 1) {
                scrollState.animateScrollTo(
                    value = target,
                    animationSpec = tween(
                        durationMillis = PlainFollowScrollDurationMillis,
                        easing = LinearEasing,
                    ),
                )
            }
        }

        val returnDirection = if (
            state.interactionMode == LyricsViewportInteractionMode.BROWSE &&
            playbackTargetScrollPx != null
        ) {
            playbackRegionDirection(
                state = state,
                targetScrollPx = playbackTargetScrollPx,
                currentScrollPx = scrollState.value,
                viewportHeightPx = viewportHeightPx,
            )
        } else {
            null
        }

        LaunchedEffect(
            state.interactionMode,
            returnDirection,
            scrollState.isScrollInProgress,
            playbackTargetScrollPx,
        ) {
            if (
                state.interactionMode == LyricsViewportInteractionMode.BROWSE &&
                playbackTargetScrollPx != null &&
                returnDirection == null &&
                !scrollState.isScrollInProgress
            ) {
                latestModeChange.value(LyricsViewportInteractionMode.FOLLOW)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(userScrollConnection)
                .graphicsLayer {
                    compositingStrategy = CompositingStrategy.Offscreen
                }
                .drawWithContent {
                    drawContent()
                    drawRect(
                        brush = Brush.verticalGradient(
                            0f to Color.Transparent,
                            TopEdgeFadeFraction to Color.Black,
                            (1f - BottomEdgeFadeFraction) to Color.Black,
                            1f to Color.Transparent,
                        ),
                        blendMode = BlendMode.DstIn,
                    )
                },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(
                        start = AALyricsSpacing.Space20,
                        top = topContentPadding,
                        end = AALyricsSpacing.Space20,
                        bottom = bottomContentPadding,
                    ),
                verticalArrangement = Arrangement.spacedBy(AALyricsSpacing.Space16),
            ) {
                if (hasOpeningFocusRow) {
                    OpeningFocusRow(
                        focusPosition = animatedFocusIndex,
                        modifier = Modifier.fillMaxWidth(TimedTextWidthFraction),
                    )
                }

                state.lines.forEachIndexed { index, line ->
                    LyricsViewportRow(
                        state = state,
                        line = line,
                        index = index,
                        focusPosition = animatedFocusIndex,
                        rowSpacingPx = rowSpacingPx,
                        modifier = Modifier.fillMaxWidth(),
                        onTextHeightChanged = { height ->
                            lineHeights[index] = height
                        },
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = returnDirection != null,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(
                    bottom = (maxHeight * (BottomEdgeFadeFraction / 3f)) + ReturnControlBottomInset,
                ),
            enter = fadeIn(animationSpec = tween(ReturnControlFadeMillis)),
            exit = fadeOut(animationSpec = tween(ReturnControlFadeMillis)),
        ) {
            returnDirection?.let { direction ->
                ReturnToPlaybackControl(
                    direction = direction,
                    onClick = {
                        val target = playbackTargetScrollPx ?: return@ReturnToPlaybackControl
                        scope.launch {
                            scrollState.animateScrollTo(
                                value = target.coerceIn(0, scrollState.maxValue),
                                animationSpec = tween(
                                    durationMillis = ReturnScrollDurationMillis,
                                    easing = FastOutSlowInEasing,
                                ),
                            )
                            latestModeChange.value(LyricsViewportInteractionMode.FOLLOW)
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun LyricsViewportRow(
    state: LyricsViewportUiState,
    line: LyricsViewportLineUiState,
    index: Int,
    focusPosition: Animatable<Float, AnimationVector1D>,
    rowSpacingPx: Int,
    modifier: Modifier = Modifier,
    onTextHeightChanged: (Int) -> Unit,
) {
    val isCurrent = state.syncType != LyricsSyncType.PLAIN && index == state.currentLineIndex
    val virtualIndex = index + 1f

    val karaokeLine = state.karaokeLine?.takeIf { karaoke ->
        val sweepValid = karaoke.sweep?.let { sweep ->
            sweep.start >= 0 && sweep.end <= line.text.length &&
                sweep.start < sweep.end && sweep.progress.isFinite()
        } ?: true
        isCurrent && karaoke.completedEnd in 0..line.text.length && sweepValid
    }

    val isTimed = state.syncType != LyricsSyncType.PLAIN

    Box(
        modifier = if (isTimed) {
            modifier.reserveTimedScaleHeight(rowSpacingPx)
        } else {
            modifier
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(
                    fraction = if (state.syncType == LyricsSyncType.PLAIN) {
                        1f
                    } else {
                        TimedTextWidthFraction
                    },
                )
                .onSizeChanged { size ->
                    onTextHeightChanged(
                        if (isTimed) {
                            reservedTimedRowHeightPx(
                                unscaledHeightPx = size.height,
                                rowSpacingPx = rowSpacingPx,
                                maxScale = CurrentScale,
                            )
                        } else {
                            size.height
                        },
                    )
                }
                .graphicsLayer {
                    if (state.syncType == LyricsSyncType.PLAIN) {
                        scaleX = 1f
                        scaleY = 1f
                        alpha = 1f
                    } else {
                        val focus = focusAmount(
                            rowIndex = virtualIndex,
                            focusIndex = focusPosition.value,
                        )
                        val scale = SupportingScale +
                            ((CurrentScale - SupportingScale) * focus)
                        val supportingAlpha = if (virtualIndex > focusPosition.value) {
                            FutureSupportingAlpha
                        } else {
                            PastSupportingAlpha
                        }
                        scaleX = scale
                        scaleY = scale
                        alpha = supportingAlpha + ((1f - supportingAlpha) * focus)
                        transformOrigin = TransformOrigin(0f, 0.5f)
                    }
                },
            verticalArrangement = Arrangement.spacedBy(TranslationIntraRowGap),
        ) {
            val lyricStyle = AALyricsTypography.LyricsSupporting.copy(
                    fontSize = if (state.syncType == LyricsSyncType.PLAIN) {
                        PlainLyricsFontSize
                    } else {
                        StableLyricsFontSize
                    },
                    lineHeight = StableLyricsLineHeight,
                    fontWeight = if (state.syncType == LyricsSyncType.PLAIN) {
                        FontWeight.Medium
                    } else {
                        FontWeight.Bold
                    },
                )
            val lyricColor = if (state.syncType == LyricsSyncType.PLAIN) {
                    AALyricsColors.TextSecondary
                } else {
                    AALyricsColors.TextPrimary
                }
            if (karaokeLine != null) {
                KaraokeLineText(
                    text = line.text,
                    karaoke = karaokeLine,
                    style = lyricStyle,
                )
            } else {
                Text(
                    text = line.text,
                    style = lyricStyle,
                    color = lyricColor,
                    textAlign = TextAlign.Start,
                )
            }
            line.translatedText?.let { translatedText ->
                Text(
                    text = translatedText,
                    style = AALyricsTypography.LyricsSupporting.copy(
                        fontSize = TranslationFontSize,
                        lineHeight = TranslationLineHeight,
                        fontWeight = FontWeight.Medium,
                    ),
                    color = AALyricsColors.TextSecondary.copy(alpha = TranslationTextAlpha),
                    textAlign = TextAlign.Start,
                )
            }
        }
    }
}

@Composable
private fun OpeningFocusRow(
    focusPosition: Animatable<Float, AnimationVector1D>,
    modifier: Modifier = Modifier,
) {
    Text(
        text = "♪",
        modifier = modifier.graphicsLayer {
            val focus = focusAmount(
                rowIndex = OpeningFocusVirtualIndex,
                focusIndex = focusPosition.value,
            )
            val scale = SupportingScale + ((CurrentScale - SupportingScale) * focus)
            scaleX = scale
            scaleY = scale
            alpha = PastSupportingAlpha + ((1f - PastSupportingAlpha) * focus)
            transformOrigin = TransformOrigin(0f, 0.5f)
        },
        style = AALyricsTypography.LyricsSupporting.copy(
            fontSize = StableLyricsFontSize,
            lineHeight = StableLyricsLineHeight,
            fontWeight = FontWeight.Bold,
        ),
        color = AALyricsColors.TextPrimary,
        textAlign = TextAlign.Start,
    )
}

@Composable
private fun KaraokeLineText(
    text: String,
    karaoke: KaraokeLineUiState,
    style: androidx.compose.ui.text.TextStyle,
) {
    val primary = AALyricsColors.TextPrimary
    val secondary = AALyricsColors.TextSecondary.copy(alpha = 0.72f)
    val base = buildAnnotatedString {
        withStyle(SpanStyle(color = primary)) { append(text, 0, karaoke.completedEnd) }
        withStyle(SpanStyle(color = secondary)) { append(text, karaoke.completedEnd, text.length) }
    }
    val sweep = karaoke.sweep
    var layout by remember(text) { mutableStateOf<TextLayoutResult?>(null) }

    Box(Modifier.fillMaxWidth()) {
        Text(text = base, style = style, textAlign = TextAlign.Start)
        if (sweep != null) {
            val overlay = buildAnnotatedString {
                withStyle(SpanStyle(color = Color.Transparent)) { append(text, 0, sweep.start) }
                withStyle(SpanStyle(color = primary)) { append(text, sweep.start, sweep.end) }
                withStyle(SpanStyle(color = Color.Transparent)) {
                    append(text, sweep.end, text.length)
                }
            }
            Text(
                text = overlay,
                style = style,
                textAlign = TextAlign.Start,
                onTextLayout = { layout = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .clearAndSetSemantics {}
                    .drawWithContent {
                        val textLayout = layout ?: return@drawWithContent
                        val visualBoxes = (sweep.start until sweep.end).map { index ->
                            KaraokeVisualBox(
                                line = textLayout.getLineForOffset(index),
                                bounds = textLayout.getBoundingBox(index),
                            )
                        }
                        val clip = Path()
                        karaokeVisualSweepClipRects(
                            boxes = visualBoxes,
                            progress = sweep.progress,
                        ).forEach(clip::addRect)
                        val contentScope = this
                        clipPath(clip) { contentScope.drawContent() }
                    },
            )
        }
    }
}

internal data class KaraokeVisualBox(
    val line: Int,
    val bounds: Rect,
)

internal fun karaokeVisualSweepClipRects(
    boxes: List<KaraokeVisualBox>,
    progress: Float,
): List<Rect> {
    val segments = boxes
        .filter { it.bounds.width > 0f && it.bounds.height > 0f }
        .groupBy(KaraokeVisualBox::line)
        .toSortedMap()
        .values
        .flatMap { lineBoxes ->
            val sorted = lineBoxes
                .map(KaraokeVisualBox::bounds)
                .sortedWith(compareBy<Rect> { it.left }.thenBy { it.right })
            if (sorted.isEmpty()) {
                emptyList()
            } else {
                buildList {
                    var current = sorted.first()
                    sorted.drop(1).forEach { next ->
                        current = if (next.left <= current.right) {
                            Rect(
                                left = current.left,
                                top = minOf(current.top, next.top),
                                right = maxOf(current.right, next.right),
                                bottom = maxOf(current.bottom, next.bottom),
                            )
                        } else {
                            add(current)
                            next
                        }
                    }
                    add(current)
                }
            }
        }

    val totalWidth = segments.sumOf { it.width.toDouble() }.toFloat()
    var remaining = totalWidth * progress.coerceIn(0f, 1f)

    return buildList {
        for (segment in segments) {
            if (remaining <= 0f) break
            val width = remaining.coerceAtMost(segment.width)
            add(
                Rect(
                    left = segment.left,
                    top = segment.top,
                    right = segment.left + width,
                    bottom = segment.bottom,
                ),
            )
            remaining -= width
        }
    }
}

@Composable
private fun ReturnToPlaybackControl(
    direction: PlaybackRegionDirection,
    onClick: () -> Unit,
) {
    val bounceOffset = remember { Animatable(0f) }

    LaunchedEffect(direction) {
        bounceOffset.snapTo(0f)
        delay(ReturnBounceStartDelayMillis)
        val target = if (direction == PlaybackRegionDirection.ABOVE) {
            -ReturnBounceDistanceDp
        } else {
            ReturnBounceDistanceDp
        }
        bounceOffset.animateTo(
            targetValue = target,
            animationSpec = tween(
                durationMillis = ReturnBounceHalfCycleMillis,
                easing = FastOutSlowInEasing,
            ),
        )
        bounceOffset.animateTo(
            targetValue = 0f,
            animationSpec = tween(
                durationMillis = ReturnBounceHalfCycleMillis,
                easing = FastOutSlowInEasing,
            ),
        )
    }

    Box(
        modifier = Modifier
            .size(AALyricsSpacing.Space48)
            .clip(CircleShape)
            .semantics {
                contentDescription = "Return to current playback position"
                role = Role.Button
            }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(ReturnControlVisualSize)
                .background(
                    color = AALyricsColors.BackgroundSurfaceStrong.copy(alpha = ReturnControlFillAlpha),
                    shape = CircleShape,
                )
                .border(
                    width = AALyricsStroke.Thin,
                    color = AALyricsColors.BorderSoft.copy(alpha = ReturnControlBorderAlpha),
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (direction == PlaybackRegionDirection.ABOVE) {
                    AALyricsIcons.PlaybackAbove
                } else {
                    AALyricsIcons.PlaybackBelow
                },
                contentDescription = null,
                modifier = Modifier
                    .size(ReturnChevronSize)
                    .offset(y = bounceOffset.value.dp),
                tint = AALyricsColors.AccentCyan.copy(alpha = ReturnChevronAlpha),
            )
        }
    }
}

private fun timedFocusIndex(state: LyricsViewportUiState): Float {
    if (state.syncType == LyricsSyncType.PLAIN) return OpeningFocusVirtualIndex
    val currentIndex = state.currentLineIndex
        ?.takeIf { it in state.lines.indices }
        ?: return OpeningFocusVirtualIndex
    return currentIndex + 1f
}

private fun syncedScrollPxForFocusIndex(
    focusIndex: Float,
    lineHeights: Map<Int, Int>,
    openingFocusRowHeightPx: Int,
    rowSpacingPx: Int,
    topContentPaddingPx: Int,
    viewportHeightPx: Int,
): Int? {
    if (openingFocusRowHeightPx <= 0) return null
    val maxVirtualIndex = lineHeights.keys.maxOrNull()?.plus(1) ?: return null
    val clamped = focusIndex.coerceIn(OpeningFocusVirtualIndex, maxVirtualIndex.toFloat())
    val lower = kotlin.math.floor(clamped).toInt()
    val upper = kotlin.math.ceil(clamped).toInt().coerceAtMost(maxVirtualIndex)
    val lowerCenter = documentCenterForVirtualRow(
        virtualIndex = lower,
        lineHeights = lineHeights,
        openingFocusRowHeightPx = openingFocusRowHeightPx,
        rowSpacingPx = rowSpacingPx,
        topContentPaddingPx = topContentPaddingPx,
    ) ?: return null
    val upperCenter = documentCenterForVirtualRow(
        virtualIndex = upper,
        lineHeights = lineHeights,
        openingFocusRowHeightPx = openingFocusRowHeightPx,
        rowSpacingPx = rowSpacingPx,
        topContentPaddingPx = topContentPaddingPx,
    ) ?: return null
    val fraction = clamped - lower
    val interpolatedCenter = lowerCenter + ((upperCenter - lowerCenter) * fraction)
    val viewportFocusCenter = viewportHeightPx * FocusCenterFraction
    return (interpolatedCenter - viewportFocusCenter).roundToInt()
}

private fun documentCenterForVirtualRow(
    virtualIndex: Int,
    lineHeights: Map<Int, Int>,
    openingFocusRowHeightPx: Int,
    rowSpacingPx: Int,
    topContentPaddingPx: Int,
): Float? {
    if (virtualIndex == 0) {
        return topContentPaddingPx + (openingFocusRowHeightPx / 2f)
    }

    val lyricIndex = virtualIndex - 1
    val currentHeight = lineHeights[lyricIndex] ?: return null
    var topPx = topContentPaddingPx +
        openingFocusRowHeightPx +
        rowSpacingPx

    for (index in 0 until lyricIndex) {
        topPx += (lineHeights[index] ?: return null) + rowSpacingPx
    }

    return topPx + (currentHeight / 2f)
}

private fun Modifier.reserveTimedScaleHeight(
    rowSpacingPx: Int,
): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    val reservedHeight = reservedTimedRowHeightPx(
        unscaledHeightPx = placeable.height,
        rowSpacingPx = rowSpacingPx,
        maxScale = CurrentScale,
    )
    layout(placeable.width, reservedHeight) {
        placeable.placeRelative(
            x = 0,
            y = (reservedHeight - placeable.height) / 2,
        )
    }
}

internal fun reservedTimedRowHeightPx(
    unscaledHeightPx: Int,
    rowSpacingPx: Int,
    maxScale: Float,
): Int {
    require(unscaledHeightPx >= 0) { "Unscaled row height must not be negative" }
    require(rowSpacingPx >= 0) { "Row spacing must not be negative" }
    require(maxScale >= 1f && maxScale.isFinite()) {
        "Maximum row scale must be finite and at least 1"
    }

    val scaledHeightPx = ceil(unscaledHeightPx * maxScale).toInt()
    return maxOf(
        unscaledHeightPx,
        scaledHeightPx - rowSpacingPx,
    )
}

private fun focusAmount(
    rowIndex: Float,
    focusIndex: Float,
): Float {
    val proximity = (1f - abs(rowIndex - focusIndex)).coerceIn(0f, 1f)
    return proximity * proximity * (3f - (2f * proximity))
}

private fun plainTargetScrollPx(
    state: LyricsViewportUiState,
    maxScrollPx: Int,
): Int? {
    if (
        state.syncType != LyricsSyncType.PLAIN ||
        !state.plainAutoScrollEnabled ||
        maxScrollPx <= 0
    ) {
        return null
    }

    val playbackProgress = state.playbackProgress ?: return null
    val documentProgress = (
        (playbackProgress.coerceIn(0f, 1f) - PlainLeadInFraction) /
            (1f - PlainLeadInFraction - PlainLeadOutFraction)
        ).coerceIn(0f, 1f)

    return (maxScrollPx * documentProgress).roundToInt()
}

private fun playbackRegionDirection(
    state: LyricsViewportUiState,
    targetScrollPx: Int,
    currentScrollPx: Int,
    viewportHeightPx: Int,
): PlaybackRegionDirection? {
    if (viewportHeightPx <= 0) return null

    val delta = targetScrollPx - currentScrollPx
    val toleranceFraction = if (state.syncType == LyricsSyncType.PLAIN) {
        PlainFocusToleranceFraction
    } else {
        SyncedFocusToleranceFraction
    }
    val tolerance = viewportHeightPx * toleranceFraction

    return when {
        delta < -tolerance -> PlaybackRegionDirection.ABOVE
        delta > tolerance -> PlaybackRegionDirection.BELOW
        else -> null
    }
}

private enum class PlaybackRegionDirection {
    ABOVE,
    BELOW,
}

private const val FocusCenterFraction = 0.45f
private const val EndBoundaryStartFraction = 0.50f
private const val TopEdgeFadeFraction = 0.15f
private const val BottomEdgeFadeFraction = 0.15f
private const val SyncedFocusToleranceFraction = 0.15f
private const val PlainFocusToleranceFraction = 0.15f
private const val PlainLeadInFraction = 0.05f
private const val PlainLeadOutFraction = 0.05f

private const val FocusSpringStiffness = 120f
private const val FocusSpringDampingRatio = 0.82f
private const val FocusSpringVisibilityThreshold = 0.0015f
private const val FocusSnapJumpRows = 6f
private const val OpeningFocusVirtualIndex = 0f
private const val SupportingScale = 0.90f
private const val CurrentScale = 1.15f
private const val TimedTextWidthFraction = 1f / CurrentScale
private const val PastSupportingAlpha = 0.48f
private const val FutureSupportingAlpha = 0.70f
private const val PlainFollowScrollDurationMillis = 350
private const val ReturnScrollDurationMillis = 420
private const val ReturnControlFadeMillis = 140
private const val ReturnBounceStartDelayMillis = 90L

private val PlainLyricsFontSize = 18.sp
private val StableLyricsFontSize = 20.sp
private val StableLyricsLineHeight = 30.sp
private val TranslationFontSize = 15.sp
private val TranslationLineHeight = 21.sp
private val TranslationIntraRowGap = 4.dp
private const val TranslationTextAlpha = 0.76f
private val ReturnControlVisualSize = 36.dp
private val ReturnChevronSize = 28.dp
private val ReturnControlBottomInset = 3.dp
private const val ReturnControlFillAlpha = 0.38f
private const val ReturnControlBorderAlpha = 0.22f
private const val ReturnChevronAlpha = 0.94f
private const val ReturnBounceDistanceDp = 3f
private const val ReturnBounceHalfCycleMillis = 200
