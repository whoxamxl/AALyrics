package io.github.whoxamxl.aalyrics.ui.phone.lyrics

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
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
import kotlinx.coroutines.launch
import kotlin.math.abs
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
        val currentLineNeighborMarginPx = with(density) {
            CurrentLineNeighborMargin.roundToPx()
        }
        val lineHeights = remember(state.lines) { mutableStateMapOf<Int, Int>() }
        val firstLineHeightPx = lineHeights[0] ?: 0
        val lastLineHeightPx = lineHeights[state.lines.lastIndex] ?: 0
        val boundaryCenterPx = (viewportHeightPx * BoundaryCenterFraction).roundToInt()
        val topContentPaddingPx = (
            boundaryCenterPx - firstLineHeightPx
            ).coerceAtLeast(minimumContentPaddingPx)
        val bottomContentPaddingPx = (
            boundaryCenterPx - lastLineHeightPx
            ).coerceAtLeast(minimumContentPaddingPx)
        val topContentPadding = with(density) { topContentPaddingPx.toDp() }
        val bottomContentPadding = with(density) { bottomContentPaddingPx.toDp() }
        val scope = rememberCoroutineScope()

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
        }
        val syncTargetScrollPx = syncedTargetScrollPx(
            state = state,
            lineHeights = lineHeights,
            rowSpacingPx = rowSpacingPx,
            topContentPaddingPx = topContentPaddingPx,
            viewportHeightPx = viewportHeightPx,
            currentLineNeighborMarginPx = currentLineNeighborMarginPx,
        )
        val plainTargetScrollPx = plainTargetScrollPx(
            state = state,
            maxScrollPx = scrollState.maxValue,
        )
        val playbackTargetScrollPx = when (state.syncType) {
            LyricsSyncType.PLAIN -> plainTargetScrollPx
            LyricsSyncType.LINE,
            LyricsSyncType.WORD -> syncTargetScrollPx
        }

        LaunchedEffect(
            state.interactionMode,
            playbackTargetScrollPx,
            measuredHeightSignature,
            scrollState.maxValue,
            state.syncType,
        ) {
            if (
                state.interactionMode == LyricsViewportInteractionMode.FOLLOW &&
                playbackTargetScrollPx != null
            ) {
                val target = playbackTargetScrollPx.coerceIn(0, scrollState.maxValue)
                if (abs(scrollState.value - target) > 1) {
                    scrollState.animateScrollTo(
                        value = target,
                        animationSpec = tween(
                            durationMillis = if (state.syncType == LyricsSyncType.PLAIN) {
                                PlainFollowScrollDurationMillis
                            } else {
                                SyncedFollowScrollDurationMillis
                            },
                            easing = if (state.syncType == LyricsSyncType.PLAIN) {
                                LinearEasing
                            } else {
                                FastOutSlowInEasing
                            },
                        ),
                    )
                }
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
                            EdgeFadeFraction to Color.Black,
                            (1f - EdgeFadeFraction) to Color.Black,
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
                state.lines.forEachIndexed { index, line ->
                    LyricsViewportRow(
                        state = state,
                        line = line,
                        index = index,
                        isFirst = index == 0,
                        isLast = index == state.lines.lastIndex,
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
                    bottom = (maxHeight * (EdgeFadeFraction / 3f)) + ReturnControlBottomInset,
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
    isFirst: Boolean,
    isLast: Boolean,
    modifier: Modifier = Modifier,
    onTextHeightChanged: (Int) -> Unit,
) {
    val isCurrent = state.syncType != LyricsSyncType.PLAIN && index == state.currentLineIndex
    val emphasis by animateFloatAsState(
        targetValue = if (isCurrent) 1f else 0f,
        animationSpec = tween(
            durationMillis = CurrentEmphasisTransitionMillis,
            easing = FastOutSlowInEasing,
        ),
        label = "lyrics-current-emphasis",
    )
    val animatedNeighborMargin = (CurrentLineNeighborMargin.value * emphasis).dp
    val neighborPadding = PaddingValues(
        top = if (isFirst) 0.dp else animatedNeighborMargin,
        bottom = if (isLast) 0.dp else animatedNeighborMargin,
    )

    val text = if (
        isCurrent &&
        state.syncType == LyricsSyncType.WORD &&
        line.words.isNotEmpty()
    ) {
        buildWordProgressText(
            words = line.words,
            currentWordIndex = state.currentWordIndex,
            currentWordProgress = state.currentWordProgress,
        )
    } else {
        buildAnnotatedString { append(line.text) }
    }

    Box(
        modifier = modifier.padding(neighborPadding),
    ) {
        Text(
            text = text,
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged { size ->
                    onTextHeightChanged(size.height)
                },
            style = AALyricsTypography.LyricsSupporting.copy(
                fontSize = (18f + (4f * emphasis)).sp,
                lineHeight = (26f + (4f * emphasis)).sp,
                fontWeight = FontWeight(
                    (FontWeight.Medium.weight +
                        ((FontWeight.Bold.weight - FontWeight.Medium.weight) * emphasis))
                        .roundToInt(),
                ),
            ),
            color = lerp(
                AALyricsColors.TextSecondary,
                AALyricsColors.TextPrimary,
                emphasis,
            ),
            textAlign = TextAlign.Start,
        )
    }
}

private fun buildWordProgressText(
    words: List<String>,
    currentWordIndex: Int?,
    currentWordProgress: Float,
) = buildAnnotatedString {
    val activeIndex = currentWordIndex ?: -1
    val activeProgress = currentWordProgress.coerceIn(0f, 1f)
    val activeColor = lerp(
        AALyricsColors.TextPrimary,
        AALyricsColors.AccentCyan,
        0.55f + (activeProgress * 0.45f),
    )

    words.forEachIndexed { index, word ->
        if (index > 0) append(" ")
        val color = when {
            index < activeIndex -> AALyricsColors.TextPrimary
            index == activeIndex -> activeColor
            else -> AALyricsColors.TextSecondary.copy(alpha = 0.72f)
        }
        withStyle(SpanStyle(color = color)) {
            append(word)
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

private fun syncedTargetScrollPx(
    state: LyricsViewportUiState,
    lineHeights: Map<Int, Int>,
    rowSpacingPx: Int,
    topContentPaddingPx: Int,
    viewportHeightPx: Int,
    currentLineNeighborMarginPx: Int,
): Int? {
    if (state.syncType == LyricsSyncType.PLAIN) return null

    val currentIndex = state.currentLineIndex
        ?.takeIf { it in state.lines.indices }
        ?: return null
    val currentHeight = lineHeights[currentIndex] ?: return null

    var beforeHeight = 0
    for (index in 0 until currentIndex) {
        beforeHeight += lineHeights[index] ?: return null
    }

    val currentTopMarginPx = if (currentIndex > 0) {
        currentLineNeighborMarginPx
    } else {
        0
    }
    val currentBottomPx = topContentPaddingPx +
        beforeHeight +
        (rowSpacingPx * currentIndex) +
        currentTopMarginPx +
        currentHeight
    val focusBottomPx = resolvedFocusBottomPx(
        viewportHeightPx = viewportHeightPx,
        currentHeightPx = currentHeight,
    )

    return currentBottomPx - focusBottomPx
}

private fun resolvedFocusBottomPx(
    viewportHeightPx: Int,
    currentHeightPx: Int,
): Int {
    val nominalBottomPx = viewportHeightPx * FocusBottomFraction
    val focusTopPx = viewportHeightPx * FocusZoneStartFraction
    val focusBottomLimitPx = viewportHeightPx * FocusZoneEndFraction
    val minimumBottomPx = focusTopPx + currentHeightPx

    return if (minimumBottomPx <= focusBottomLimitPx) {
        nominalBottomPx
            .coerceIn(minimumBottomPx, focusBottomLimitPx)
            .roundToInt()
    } else {
        val focusCenterPx = viewportHeightPx *
            ((FocusZoneStartFraction + FocusZoneEndFraction) / 2f)
        (focusCenterPx + (currentHeightPx / 2f)).roundToInt()
    }
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

private const val FocusBottomFraction = 0.52f
private const val BoundaryCenterFraction = 0.50f
private const val EdgeFadeFraction = 0.20f
private const val FocusZoneStartFraction = 0.28f
private const val FocusZoneEndFraction = 0.60f
private const val SyncedFocusToleranceFraction = 0.15f
private const val PlainFocusToleranceFraction = 0.15f
private const val PlainLeadInFraction = 0.05f
private const val PlainLeadOutFraction = 0.05f

private const val CurrentEmphasisTransitionMillis = 320
private const val SyncedFollowScrollDurationMillis = 420
private const val PlainFollowScrollDurationMillis = 350
private const val ReturnScrollDurationMillis = 420
private const val ReturnControlFadeMillis = 140
private const val ReturnBounceStartDelayMillis = 90L

private val CurrentLineNeighborMargin = 16.dp
private val ReturnControlVisualSize = 36.dp
private val ReturnChevronSize = 28.dp
private val ReturnControlBottomInset = 3.dp
private const val ReturnControlFillAlpha = 0.38f
private const val ReturnControlBorderAlpha = 0.22f
private const val ReturnChevronAlpha = 0.94f
private const val ReturnBounceDistanceDp = 3f
private const val ReturnBounceHalfCycleMillis = 200
