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
import kotlin.math.min
import kotlin.math.sin
import kotlinx.coroutines.isActive

/**
 * One UI-inspired monochrome media waveform.
 *
 * The active segment is a filled pill that swells into two or three broad animated blobs while the
 * inactive segment stays completely straight. Animation changes the blob profile/position slightly;
 * it never grows the waveform progressively from a flat line.
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
        val active = if (enabled) activeColor else disabledColor
        val inactive = if (enabled) inactiveColor else disabledColor.copy(alpha = 0.48f)

        val baseHalfHeight = 4.dp.toPx()
        val maxBlobExpansion = if (enabled) 7.dp.toPx() else 0f
        val centerDrift = if (enabled) 1.25.dp.toPx() else 0f
        val sampleStep = 1.5.dp.toPx().coerceAtLeast(1f)

        if (thumbX > 0f) {
            if (thumbX < 48.dp.toPx() || maxBlobExpansion <= 0f) {
                drawLine(
                    color = active,
                    start = Offset(0f, centerY),
                    end = Offset(thumbX, centerY),
                    strokeWidth = baseHalfHeight * 2f,
                    cap = StrokeCap.Round,
                )
            } else {
                val activePath = Path()
                var x = 0f
                var firstPoint = true

                // Upper edge.
                while (x <= thumbX) {
                    val ratio = (x / thumbX).coerceIn(0f, 1f)
                    val profile = blobProfile(
                        ratio = ratio,
                        phase = phase,
                    )
                    val edgeFade = edgeFade(ratio)
                    val halfHeight = baseHalfHeight +
                        maxBlobExpansion * profile * edgeFade
                    val centerOffset = centerDrift *
                        sin(ratio * TWO_PI * 1.35f + phase * 0.45f) *
                        edgeFade
                    val y = centerY + centerOffset - halfHeight

                    if (firstPoint) {
                        activePath.moveTo(x, y)
                        firstPoint = false
                    } else {
                        activePath.lineTo(x, y)
                    }
                    x += sampleStep
                }
                activePath.lineTo(thumbX, centerY - baseHalfHeight)

                // Lower edge, reversed.
                x = thumbX
                while (x >= 0f) {
                    val ratio = (x / thumbX).coerceIn(0f, 1f)
                    val profile = blobProfile(
                        ratio = ratio,
                        phase = phase + LOWER_PROFILE_PHASE_OFFSET,
                    )
                    val edgeFade = edgeFade(ratio)
                    val halfHeight = baseHalfHeight +
                        maxBlobExpansion * profile * edgeFade
                    val centerOffset = centerDrift *
                        sin(ratio * TWO_PI * 1.18f + phase * 0.38f + 0.7f) *
                        edgeFade
                    val y = centerY + centerOffset + halfHeight

                    activePath.lineTo(x, y)
                    x -= sampleStep
                }
                activePath.lineTo(0f, centerY + baseHalfHeight)
                activePath.close()

                drawPath(
                    path = activePath,
                    color = active,
                )
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

private fun blobProfile(
    ratio: Float,
    phase: Float,
): Float {
    val first = gaussianBlob(
        ratio = ratio,
        center = 0.22f + 0.018f * sin(phase * 0.72f),
        width = 0.105f,
        strength = 0.74f + 0.18f * sin(phase + 0.2f),
    )
    val second = gaussianBlob(
        ratio = ratio,
        center = 0.50f + 0.024f * sin(phase * 0.61f + 1.9f),
        width = 0.13f,
        strength = 0.86f + 0.14f * sin(phase + 2.1f),
    )
    val third = gaussianBlob(
        ratio = ratio,
        center = 0.77f + 0.019f * sin(phase * 0.67f + 3.6f),
        width = 0.11f,
        strength = 0.72f + 0.20f * sin(phase + 4.2f),
    )

    return min(1f, first + second + third)
}

private fun gaussianBlob(
    ratio: Float,
    center: Float,
    width: Float,
    strength: Float,
): Float {
    val normalizedDistance = (ratio - center) / width
    return (
        strength * exp((-0.5f * normalizedDistance * normalizedDistance).toDouble())
    ).toFloat()
}

private fun edgeFade(ratio: Float): Float {
    val leading = smoothStep((ratio / EDGE_FADE_FRACTION).coerceIn(0f, 1f))
    val trailing = smoothStep(((1f - ratio) / EDGE_FADE_FRACTION).coerceIn(0f, 1f))
    return min(leading, trailing)
}

private fun smoothStep(value: Float): Float =
    value * value * (3f - 2f * value)

private const val BLOB_CYCLE_SECONDS = 2.6f
private const val EDGE_FADE_FRACTION = 0.10f
private const val LOWER_PROFILE_PHASE_OFFSET = 0.85f
private const val TWO_PI = (2.0 * PI).toFloat()
