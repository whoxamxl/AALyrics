package io.github.whoxamxl.aalyrics.ui.phone.lyrics

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.scrollBy
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
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
import kotlin.math.floor
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
    listState: LazyListState = rememberLazyListState(),
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

        // Keep only measurements we have actually materialized. Lazy composition must not
        // require the whole lyrics document to be measured before it can be presented.
        // Translation is deliberately excluded from canonical row identity so adding secondary
        // text remeasures the existing keyed item instead of resetting the viewport geometry.
        val canonicalRows = remember(state.lines) {
            state.lines.map { it.text to it.words }
        }
        val lineHeights = remember(canonicalRows) { mutableStateMapOf<Int, Int>() }
        val measuredHeightSignature = lineHeights.entries.sumOf { (index, height) ->
            (index + 1) * 31 + height
        }
        val lastLineHeightPx = lineHeights[state.lines.lastIndex] ?: 0
        val openingContentStartPx = (viewportHeightPx * TopEdgeFadeFraction).roundToInt()
        val endingBoundaryStartPx = (
            viewportHeightPx * EndBoundaryStartFraction
            ).roundToInt()
        val topContentPaddingPx = lazyTopContentPaddingPx(
            openingContentStartPx = openingContentStartPx,
            openingFocusRowHeightPx = openingFocusRowHeightPx,
            rowSpacingPx = rowSpacingPx,
            minimumContentPaddingPx = minimumContentPaddingPx,
            hasOpeningFocusRow = hasOpeningFocusRow,
        )
        val bottomContentPaddingPx = lazyBottomContentPaddingPx(
            endingBoundaryStartPx = endingBoundaryStartPx,
            lastLineHeightPx = lastLineHeightPx,
            minimumContentPaddingPx = minimumContentPaddingPx,
        )
        val topContentPadding = with(density) { topContentPaddingPx.toDp() }
        val bottomContentPadding = with(density) { bottomContentPaddingPx.toDp() }

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

        val scope = rememberCoroutineScope()
        val timedPlaybackItemIndex = timedPlaybackItemIndex(state)
        val fallbackPlainRowHeightPx = with(density) {
            StableLyricsLineHeight.roundToPx()
        }
        val plainEstimatedRowStridePx = (
            if (lineHeights.isEmpty()) {
                fallbackPlainRowHeightPx
            } else {
                lineHeights.values.average().roundToInt()
            } + rowSpacingPx
            ).coerceAtLeast(1)
        val plainTarget = if (
            state.syncType == LyricsSyncType.PLAIN &&
            state.plainAutoScrollEnabled
        ) {
            plainLazyTarget(
                lineCount = state.lines.size,
                playbackProgress = state.playbackProgress,
                estimatedRowStridePx = plainEstimatedRowStridePx,
            )
        } else {
            null
        }
        val returnDirection by remember(
            state.interactionMode,
            state.syncType,
            timedPlaybackItemIndex,
            plainTarget,
            listState,
        ) {
            derivedStateOf {
                if (state.interactionMode != LyricsViewportInteractionMode.BROWSE) {
                    null
                } else {
                    val geometry = listState.lazyViewportGeometry()
                    when (state.syncType) {
                        LyricsSyncType.LINE,
                        LyricsSyncType.WORD -> timedPlaybackItemIndex?.let { targetIndex ->
                            timedPlaybackRegionDirection(
                                targetIndex = targetIndex,
                                visibleItems = geometry.items,
                                viewportStartOffset = geometry.viewportStartOffset,
                                viewportEndOffset = geometry.viewportEndOffset,
                            )
                        }
                        LyricsSyncType.PLAIN -> plainTarget?.let { target ->
                            plainPlaybackRegionDirection(
                                target = target,
                                firstVisibleItemIndex = listState.firstVisibleItemIndex,
                                firstVisibleItemScrollOffset =
                                    listState.firstVisibleItemScrollOffset,
                                viewportSizePx =
                                    geometry.viewportEndOffset -
                                        geometry.viewportStartOffset,
                            )
                        }
                    }
                }
            }
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
            state.lines.size,
            hasOpeningFocusRow,
            topContentPaddingPx,
            bottomContentPaddingPx,
            lastLineHeightPx,
            measuredHeightSignature,
        ) {
            if (
                state.interactionMode != LyricsViewportInteractionMode.FOLLOW ||
                state.syncType == LyricsSyncType.PLAIN ||
                !hasOpeningFocusRow
            ) {
                return@LaunchedEffect
            }

            val maxItemIndex = state.lines.size
            snapshotFlow { animatedFocusIndex.value }.collect { focusIndex ->
                listState.followTimedLazyFocus(
                    focusIndex = focusIndex,
                    maxItemIndex = maxItemIndex,
                )
            }
        }

        LaunchedEffect(
            state.interactionMode,
            state.syncType,
            plainTarget,
        ) {
            if (
                state.interactionMode != LyricsViewportInteractionMode.FOLLOW ||
                state.syncType != LyricsSyncType.PLAIN
            ) {
                return@LaunchedEffect
            }
            val target = plainTarget ?: return@LaunchedEffect
            if (
                listState.firstVisibleItemIndex != target.index ||
                abs(listState.firstVisibleItemScrollOffset - target.scrollOffsetPx) > 1
            ) {
                listState.animateScrollToItem(
                    index = target.index,
                    scrollOffset = target.scrollOffsetPx,
                )
            }
        }

        LaunchedEffect(
            state.interactionMode,
            returnDirection,
            listState.isScrollInProgress,
            timedPlaybackItemIndex,
            plainTarget,
        ) {
            val hasPlaybackTarget = when (state.syncType) {
                LyricsSyncType.LINE,
                LyricsSyncType.WORD -> timedPlaybackItemIndex != null
                LyricsSyncType.PLAIN -> plainTarget != null
            }
            if (
                state.interactionMode == LyricsViewportInteractionMode.BROWSE &&
                hasPlaybackTarget &&
                returnDirection == null &&
                !listState.isScrollInProgress
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
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = AALyricsSpacing.Space20,
                    top = topContentPadding,
                    end = AALyricsSpacing.Space20,
                    bottom = bottomContentPadding,
                ),
                verticalArrangement = Arrangement.spacedBy(AALyricsSpacing.Space16),
            ) {
                if (hasOpeningFocusRow) {
                    item(key = OpeningLazyItemKey) {
                        OpeningFocusRow(
                            focusPosition = animatedFocusIndex,
                            modifier = Modifier.fillMaxWidth(TimedTextWidthFraction),
                        )
                    }
                }

                itemsIndexed(
                    items = state.lines,
                    key = { index, line -> lyricsLazyItemKey(index, line) },
                ) { index, line ->
                    val rowKaraokeLine = state.karaokeLine?.takeIf {
                        state.syncType != LyricsSyncType.PLAIN &&
                            index == state.currentLineIndex
                    }
                    LyricsViewportRow(
                        line = line,
                        syncType = state.syncType,
                        karaokeLine = rowKaraokeLine,
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
                    bottom = (maxHeight * (BottomEdgeFadeFraction / 3f)) +
                        ReturnControlBottomInset,
                ),
            enter = fadeIn(animationSpec = tween(ReturnControlFadeMillis)),
            exit = fadeOut(animationSpec = tween(ReturnControlFadeMillis)),
        ) {
            returnDirection?.let { direction ->
                ReturnToPlaybackControl(
                    direction = direction,
                    onClick = {
                        scope.launch {
                            when (state.syncType) {
                                LyricsSyncType.LINE,
                                LyricsSyncType.WORD -> {
                                    val targetIndex =
                                        timedPlaybackItemIndex ?: return@launch
                                    listState.animateScrollToItem(targetIndex)
                                    val correction = lazyFocusScrollDelta(
                                        focusIndex = targetIndex.toFloat(),
                                        visibleItems =
                                            listState.lazyViewportGeometry().items,
                                        viewportStartOffset =
                                            listState.layoutInfo.viewportStartOffset,
                                        viewportEndOffset =
                                            listState.layoutInfo.viewportEndOffset,
                                    )
                                    if (correction != null && abs(correction) > 0.5f) {
                                        listState.animateScrollBy(
                                            value = correction,
                                            animationSpec = tween(
                                                durationMillis = ReturnScrollDurationMillis,
                                                easing = FastOutSlowInEasing,
                                            ),
                                        )
                                    }
                                }
                                LyricsSyncType.PLAIN -> {
                                    val target = plainTarget ?: return@launch
                                    listState.animateScrollToItem(
                                        index = target.index,
                                        scrollOffset = target.scrollOffsetPx,
                                    )
                                }
                            }
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
    line: LyricsViewportLineUiState,
    syncType: LyricsSyncType,
    karaokeLine: KaraokeLineUiState?,
    index: Int,
    focusPosition: Animatable<Float, AnimationVector1D>,
    rowSpacingPx: Int,
    modifier: Modifier = Modifier,
    onTextHeightChanged: (Int) -> Unit,
) {
    val virtualIndex = index + 1f
    val safeKaraokeLine = karaokeLine?.takeIf { karaoke ->
        val sweepValid = karaoke.sweep?.let { sweep ->
            sweep.start >= 0 && sweep.end <= line.text.length &&
                sweep.start < sweep.end && sweep.progress.isFinite()
        } ?: true
        karaoke.completedEnd in 0..line.text.length && sweepValid
    }

    val isTimed = syncType != LyricsSyncType.PLAIN

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
                    fraction = if (syncType == LyricsSyncType.PLAIN) {
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
                    if (syncType == LyricsSyncType.PLAIN) {
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
                    fontSize = if (syncType == LyricsSyncType.PLAIN) {
                        PlainLyricsFontSize
                    } else {
                        StableLyricsFontSize
                    },
                    lineHeight = StableLyricsLineHeight,
                    fontWeight = if (syncType == LyricsSyncType.PLAIN) {
                        FontWeight.Medium
                    } else {
                        FontWeight.Bold
                    },
                )
            val lyricColor = if (syncType == LyricsSyncType.PLAIN) {
                    AALyricsColors.TextSecondary
                } else {
                    AALyricsColors.TextPrimary
                }
            if (safeKaraokeLine != null) {
                KaraokeLineText(
                    text = line.text,
                    karaoke = safeKaraokeLine,
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
                    color = AALyricsColors.BackgroundSurfaceStrong.copy(
                        alpha = ReturnControlFillAlpha,
                    ),
                    shape = CircleShape,
                )
                .border(
                    width = AALyricsStroke.Thin,
                    color = AALyricsColors.BorderSoft.copy(
                        alpha = ReturnControlBorderAlpha,
                    ),
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

private const val OpeningLazyItemKey = "lyrics-opening"

internal fun lyricsLazyItemKey(
    index: Int,
    line: LyricsViewportLineUiState,
): String {
    require(index >= 0) { "Lyrics row index must not be negative" }
    return buildString {
        append("lyrics:")
        append(index)
        append(':')
        append(line.text)
        append(':')
        line.words.forEach { word ->
            append(word)
            append('\u0000')
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

internal fun lazyTopContentPaddingPx(
    openingContentStartPx: Int,
    openingFocusRowHeightPx: Int,
    rowSpacingPx: Int,
    minimumContentPaddingPx: Int,
    hasOpeningFocusRow: Boolean,
): Int {
    require(openingContentStartPx >= 0)
    require(openingFocusRowHeightPx >= 0)
    require(rowSpacingPx >= 0)
    require(minimumContentPaddingPx >= 0)

    return if (hasOpeningFocusRow) {
        (
            openingContentStartPx -
                openingFocusRowHeightPx -
                rowSpacingPx
            ).coerceAtLeast(minimumContentPaddingPx)
    } else {
        openingContentStartPx.coerceAtLeast(minimumContentPaddingPx)
    }
}

internal fun lazyBottomContentPaddingPx(
    endingBoundaryStartPx: Int,
    lastLineHeightPx: Int,
    minimumContentPaddingPx: Int,
): Int {
    require(endingBoundaryStartPx >= 0)
    require(lastLineHeightPx >= 0)
    require(minimumContentPaddingPx >= 0)

    return (
        endingBoundaryStartPx - lastLineHeightPx
        ).coerceAtLeast(minimumContentPaddingPx)
}

internal data class LazyViewportItemGeometry(
    val index: Int,
    val offset: Int,
    val size: Int,
)

private data class LazyViewportGeometry(
    val items: List<LazyViewportItemGeometry>,
    val viewportStartOffset: Int,
    val viewportEndOffset: Int,
)

internal data class PlainLazyTarget(
    val index: Int,
    val scrollOffsetPx: Int,
)

internal enum class PlaybackRegionDirection {
    ABOVE,
    BELOW,
}

private fun LazyListState.lazyViewportGeometry(): LazyViewportGeometry {
    val info = layoutInfo
    return LazyViewportGeometry(
        items = info.visibleItemsInfo.map { item ->
            LazyViewportItemGeometry(
                index = item.index,
                offset = item.offset,
                size = item.size,
            )
        },
        viewportStartOffset = info.viewportStartOffset,
        viewportEndOffset = info.viewportEndOffset,
    )
}

internal fun lazyFocusScrollDelta(
    focusIndex: Float,
    visibleItems: List<LazyViewportItemGeometry>,
    viewportStartOffset: Int,
    viewportEndOffset: Int,
    focusFraction: Float = FocusCenterFraction,
): Float? {
    if (
        !focusIndex.isFinite() ||
        focusFraction !in 0f..1f ||
        viewportEndOffset <= viewportStartOffset
    ) {
        return null
    }

    val lowerIndex = floor(focusIndex).toInt()
    val upperIndex = ceil(focusIndex).toInt()
    val lower = visibleItems.firstOrNull { it.index == lowerIndex } ?: return null
    val upper = visibleItems.firstOrNull { it.index == upperIndex } ?: return null
    if (lower.size <= 0 || upper.size <= 0) return null

    val lowerCenter = lower.offset + (lower.size / 2f)
    val upperCenter = upper.offset + (upper.size / 2f)
    val fraction = focusIndex - lowerIndex
    val focusCenter = lowerCenter + ((upperCenter - lowerCenter) * fraction)
    val viewportFocusCenter =
        viewportStartOffset +
            ((viewportEndOffset - viewportStartOffset) * focusFraction)

    return focusCenter - viewportFocusCenter
}

private suspend fun LazyListState.followTimedLazyFocus(
    focusIndex: Float,
    maxItemIndex: Int,
) {
    if (maxItemIndex < 0 || !focusIndex.isFinite()) return
    val clampedFocus = focusIndex.coerceIn(0f, maxItemIndex.toFloat())

    var geometry = lazyViewportGeometry()
    var delta = lazyFocusScrollDelta(
        focusIndex = clampedFocus,
        visibleItems = geometry.items,
        viewportStartOffset = geometry.viewportStartOffset,
        viewportEndOffset = geometry.viewportEndOffset,
    )

    if (delta == null) {
        scrollToItem(clampedFocus.roundToInt().coerceIn(0, maxItemIndex))
        geometry = lazyViewportGeometry()
        delta = lazyFocusScrollDelta(
            focusIndex = clampedFocus,
            visibleItems = geometry.items,
            viewportStartOffset = geometry.viewportStartOffset,
            viewportEndOffset = geometry.viewportEndOffset,
        )
    }

    if (delta != null && abs(delta) > 0.5f) {
        scrollBy(delta)
    }
}

private fun timedPlaybackItemIndex(state: LyricsViewportUiState): Int? {
    if (state.syncType == LyricsSyncType.PLAIN || state.lines.isEmpty()) return null
    val currentLineIndex = state.currentLineIndex
        ?.takeIf { it in state.lines.indices }
    return currentLineIndex?.plus(1) ?: 0
}

internal fun plainLazyTarget(
    lineCount: Int,
    playbackProgress: Float?,
    estimatedRowStridePx: Int,
): PlainLazyTarget? {
    if (
        lineCount <= 0 ||
        playbackProgress == null ||
        !playbackProgress.isFinite() ||
        estimatedRowStridePx <= 0
    ) {
        return null
    }

    if (lineCount == 1) return PlainLazyTarget(index = 0, scrollOffsetPx = 0)

    val documentProgress = (
        (playbackProgress.coerceIn(0f, 1f) - PlainLeadInFraction) /
            (1f - PlainLeadInFraction - PlainLeadOutFraction)
        ).coerceIn(0f, 1f)
    val itemProgress = documentProgress * (lineCount - 1)
    val index = floor(itemProgress).toInt().coerceIn(0, lineCount - 1)
    val localProgress = itemProgress - index
    val scrollOffsetPx = (localProgress * estimatedRowStridePx)
        .roundToInt()
        .coerceAtLeast(0)

    return PlainLazyTarget(
        index = index,
        scrollOffsetPx = scrollOffsetPx,
    )
}

internal fun timedPlaybackRegionDirection(
    targetIndex: Int,
    visibleItems: List<LazyViewportItemGeometry>,
    viewportStartOffset: Int,
    viewportEndOffset: Int,
    focusFraction: Float = FocusCenterFraction,
    toleranceFraction: Float = SyncedFocusToleranceFraction,
): PlaybackRegionDirection? {
    if (
        targetIndex < 0 ||
        visibleItems.isEmpty() ||
        viewportEndOffset <= viewportStartOffset
    ) {
        return null
    }

    val sorted = visibleItems.sortedBy { it.index }
    if (targetIndex < sorted.first().index) return PlaybackRegionDirection.ABOVE
    if (targetIndex > sorted.last().index) return PlaybackRegionDirection.BELOW

    val target = sorted.firstOrNull { it.index == targetIndex } ?: return null
    val viewportSize = viewportEndOffset - viewportStartOffset
    val targetCenter = target.offset + (target.size / 2f)
    val expectedCenter = viewportStartOffset + (viewportSize * focusFraction)
    val tolerance = viewportSize * toleranceFraction
    val delta = targetCenter - expectedCenter

    return when {
        delta < -tolerance -> PlaybackRegionDirection.ABOVE
        delta > tolerance -> PlaybackRegionDirection.BELOW
        else -> null
    }
}

internal fun plainPlaybackRegionDirection(
    target: PlainLazyTarget,
    firstVisibleItemIndex: Int,
    firstVisibleItemScrollOffset: Int,
    viewportSizePx: Int,
    toleranceFraction: Float = PlainFocusToleranceFraction,
): PlaybackRegionDirection? {
    if (
        target.index < 0 ||
        firstVisibleItemIndex < 0 ||
        firstVisibleItemScrollOffset < 0 ||
        viewportSizePx <= 0
    ) {
        return null
    }

    if (target.index < firstVisibleItemIndex) return PlaybackRegionDirection.ABOVE
    if (target.index > firstVisibleItemIndex) return PlaybackRegionDirection.BELOW

    val tolerance = viewportSizePx * toleranceFraction
    val delta = target.scrollOffsetPx - firstVisibleItemScrollOffset
    return when {
        delta < -tolerance -> PlaybackRegionDirection.ABOVE
        delta > tolerance -> PlaybackRegionDirection.BELOW
        else -> null
    }
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
