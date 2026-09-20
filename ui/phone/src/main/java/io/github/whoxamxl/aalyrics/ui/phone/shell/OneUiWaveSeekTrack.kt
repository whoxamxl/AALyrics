package io.github.whoxamxl.aalyrics.ui.phone.shell

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlinx.coroutines.isActive

/**
 * One UI-inspired monochrome media waveform.
 *
 * The active segment keeps one AALyrics cyan hue. A translucent cyan base pill is overlaid with
 * three broad cyan blobs that differ by opacity, reproducing the separation of One UI's multicolor
 * waveform without introducing artwork-derived colors.
 */
@Composable
internal fun OneUiWaveSeekTrack(
    progressFraction: Float,
    isPlaying: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    activeColor: Color,
    inactiveColor: Color,
    disabledColor: Color,
) {
    var phase by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(isPlaying, enabled) {
        if (!isPlaying || !enabled) return@LaunchedEffect

        var previousFrameNanos = withFrameNanos { it }
        while (isActive) {
            val frameNanos = withFrameNanos { it }
            val elapsedSeconds = (frameNanos - previousFrameNanos) / 1_000_000_000f
            previousFrameNanos = frameNanos

            phase = (
                phase + elapsedSeconds * TWO_PI / BLOB_CYCLE_SECONDS
            ) % TWO_PI
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp),
    ) {
        val fraction = progressFraction.coerceIn(0f, 1f)
        val thumbX = size.width * fraction
        val centerY = size.height / 2f
        val inactive = if (enabled) inactiveColor else disabledColor.copy(alpha = 0.48f)
        val baseHalfHeight = 4.dp.toPx()

        if (thumbX > 0f) {
            val baseColor = if (enabled) {
                activeColor.copy(alpha = ACTIVE_BASE_ALPHA)
            } else {
                disabledColor
            }

            drawLine(
                color = baseColor,
                start = Offset(0f, centerY),
                end = Offset(thumbX, centerY),
                strokeWidth = baseHalfHeight * 2f,
                cap = StrokeCap.Round,
            )

            if (enabled && thumbX >= 48.dp.toPx()) {
                val blobs = listOf(
                    BlobSpec(
                        center = 0.22f + 0.018f * sin(phase * 0.72f),
                        width = 0.115f,
                        expansion = 7.0f + 1.1f * sin(phase + 0.2f),
                        alpha = 0.36f,
                        lowerPhaseOffset = 0.45f,
                    ),
                    BlobSpec(
                        center = 0.50f + 0.024f * sin(phase * 0.61f + 1.9f),
                        width = 0.145f,
                        expansion = 8.2f + 1.0f * sin(phase + 2.1f),
                        alpha = 0.62f,
                        lowerPhaseOffset = 0.82f,
                    ),
                    BlobSpec(
                        center = 0.77f + 0.019f * sin(phase * 0.67f + 3.6f),
                        width = 0.12f,
                        expansion = 7.3f + 1.2f * sin(phase + 4.2f),
                        alpha = 0.46f,
                        lowerPhaseOffset = 1.12f,
                    ),
                )

                blobs.forEach { blob ->
                    val path = buildBlobPath(
                        thumbX = thumbX,
                        centerY = centerY,
                        baseHalfHeight = baseHalfHeight,
                        blob = blob,
                        phase = phase,
                        sampleStepPx = 1.5.dp.toPx().coerceAtLeast(1f),
                        expansionScalePx = 1.dp.toPx(),
                    )
                    drawPath(
                        path = path,
                        color = activeColor.copy(alpha = blob.alpha),
                    )
                }
            }
        }

        if (thumbX < size.width) {
            drawLine(
                color = inactive,
                start = Offset(thumbX, centerY),
                end = Offset(size.width, centerY),
                strokeWidth = baseHalfHeight * 2f,
                cap = StrokeCap.Round,
            )
        }
    }
}

private fun buildBlobPath(
    thumbX: Float,
    centerY: Float,
    baseHalfHeight: Float,
    blob: BlobSpec,
    phase: Float,
    sampleStepPx: Float,
    expansionScalePx: Float,
): Path {
    val startRatio = max(0f, blob.center - blob.width * 3.0f)
    val endRatio = min(1f, blob.center + blob.width * 3.0f)
    val startX = thumbX * startRatio
    val endX = thumbX * endRatio
    val path = Path()
    var x = startX
    var firstPoint = true

    while (x <= endX) {
        val ratio = (x / thumbX).coerceIn(0f, 1f)
        val shape = gaussianBlob(
            ratio = ratio,
            center = blob.center,
            width = blob.width,
        ) * edgeFade(ratio)
        val centerOffset = 1.1f * expansionScalePx *
            sin(ratio * TWO_PI * 1.25f + phase * 0.35f) *
            shape
        val halfHeight = baseHalfHeight +
            max(0f, blob.expansion) * expansionScalePx * shape
        val y = centerY + centerOffset - halfHeight

        if (firstPoint) {
            path.moveTo(x, centerY - baseHalfHeight)
            path.lineTo(x, y)
            firstPoint = false
        } else {
            path.lineTo(x, y)
        }
        x += sampleStepPx
    }
    path.lineTo(endX, centerY - baseHalfHeight)

    x = endX
    while (x >= startX) {
        val ratio = (x / thumbX).coerceIn(0f, 1f)
        val shape = gaussianBlob(
            ratio = ratio,
            center = blob.center,
            width = blob.width,
        ) * edgeFade(ratio)
        val centerOffset = 1.1f * expansionScalePx *
            sin(
                ratio * TWO_PI * 1.13f +
                    phase * 0.31f +
                    blob.lowerPhaseOffset,
            ) * shape
        val halfHeight = baseHalfHeight +
            max(0f, blob.expansion * 0.90f) * expansionScalePx * shape
        val y = centerY + centerOffset + halfHeight
        path.lineTo(x, y)
        x -= sampleStepPx
    }

    path.lineTo(startX, centerY + baseHalfHeight)
    path.close()
    return path
}

private fun gaussianBlob(
    ratio: Float,
    center: Float,
    width: Float,
): Float {
    val normalizedDistance = (ratio - center) / width
    return exp((-0.5f * normalizedDistance * normalizedDistance).toDouble()).toFloat()
}

private fun edgeFade(ratio: Float): Float {
    val leading = smoothStep((ratio / EDGE_FADE_FRACTION).coerceIn(0f, 1f))
    val trailing = smoothStep(((1f - ratio) / EDGE_FADE_FRACTION).coerceIn(0f, 1f))
    return min(leading, trailing)
}

private fun smoothStep(value: Float): Float =
    value * value * (3f - 2f * value)

private data class BlobSpec(
    val center: Float,
    val width: Float,
    val expansion: Float,
    val alpha: Float,
    val lowerPhaseOffset: Float,
)

private const val ACTIVE_BASE_ALPHA = 0.52f
private const val BLOB_CYCLE_SECONDS = 2.8f
private const val EDGE_FADE_FRACTION = 0.10f
private const val TWO_PI = (2.0 * PI).toFloat()
