package io.github.whoxamxl.aalyrics.ui.phone.lyrics

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import io.github.whoxamxl.aalyrics.core.model.LyricsSyncType
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsColors
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsSpacing
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsTypography
import kotlin.math.abs

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
    onInteractionModeChange: (LyricsViewportInteractionMode) -> Unit = {},
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val viewportHeightPx = with(density) { maxHeight.roundToPx() }
        val anchorHeight = maxHeight * FollowAnchorFraction
        val bottomAnchorSpace = maxHeight * (1f - FollowAnchorFraction)
        val rowSpacingPx = with(density) { AALyricsSpacing.Space16.roundToPx() }
        val scrollState = rememberScrollState()
        val lineHeights = remember(state.lines) { mutableStateMapOf<Int, Int>() }

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
        )

        LaunchedEffect(
            state.interactionMode,
            syncTargetScrollPx,
            measuredHeightSignature,
            scrollState.maxValue,
        ) {
            if (
                state.interactionMode == LyricsViewportInteractionMode.FOLLOW &&
                state.syncType != LyricsSyncType.PLAIN &&
                syncTargetScrollPx != null
            ) {
                val target = syncTargetScrollPx.coerceIn(0, scrollState.maxValue)
                if (abs(scrollState.value - target) > 1) {
                    scrollState.animateScrollTo(
                        value = target,
                        animationSpec = tween(
                            durationMillis = FollowScrollDurationMillis,
                            easing = FastOutSlowInEasing,
                        ),
                    )
                }
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
                        top = anchorHeight,
                        end = AALyricsSpacing.Space20,
                        bottom = bottomAnchorSpace,
                    ),
                verticalArrangement = Arrangement.spacedBy(AALyricsSpacing.Space16),
            ) {
                state.lines.forEachIndexed { index, line ->
                    LyricsViewportRow(
                        line = line,
                        isCurrent = index == state.currentLineIndex,
                        modifier = Modifier
                            .fillMaxWidth()
                            .onSizeChanged { size ->
                                lineHeights[index] = size.height
                            },
                    )
                }
            }
        }

        // Read the height here as well so zero-height Preview/layout states remain deterministic.
        @Suppress("UNUSED_VARIABLE")
        val responsiveViewportHeight = viewportHeightPx
    }
}

@Composable
private fun LyricsViewportRow(
    line: LyricsViewportLineUiState,
    isCurrent: Boolean,
    modifier: Modifier = Modifier,
) {
    Text(
        text = line.text,
        modifier = modifier,
        style = if (isCurrent) {
            AALyricsTypography.LyricsCurrent
        } else {
            AALyricsTypography.LyricsSupporting
        },
        color = if (isCurrent) {
            AALyricsColors.TextPrimary
        } else {
            AALyricsColors.TextSecondary
        },
        textAlign = TextAlign.Center,
    )
}

private fun syncedTargetScrollPx(
    state: LyricsViewportUiState,
    lineHeights: Map<Int, Int>,
    rowSpacingPx: Int,
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

    return beforeHeight +
        (rowSpacingPx * currentIndex) +
        (currentHeight / 2)
}

private const val FollowAnchorFraction = 0.42f
private const val EdgeFadeFraction = 0.15f
private const val FollowScrollDurationMillis = 420
