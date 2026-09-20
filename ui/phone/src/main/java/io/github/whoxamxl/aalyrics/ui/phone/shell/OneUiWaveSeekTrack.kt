package io.github.whoxamxl.aalyrics.ui.phone.shell

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

/**
 * One UI-inspired asymmetric media seek track.
 *
 * The active segment is always drawn at its final amplitude envelope. Playback animation only
 * advances the sine phase, so the wave travels without "growing" over time. The envelope starts
 * flat, crests through the active segment, and returns to zero amplitude at the thumb.
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
    val infiniteTransition = rememberInfiniteTransition(label = "playback-wave-phase")
    val animatedPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = TWO_PI,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = WAVE_PHASE_DURATION_MS,
                easing = LinearEasing,
            ),
            repeatMode = RepeatMode.Restart,
        ),
        label = "playback-wave-phase-value",
    )
    val phase = if (isPlaying && enabled) animatedPhase else 0f

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(32.dp),
    ) {
        val fraction = progressFraction.coerceIn(0f, 1f)
        val thumbX = size.width * fraction
        val centerY = size.height / 2f
        val active = if (enabled) activeColor else disabledColor
        val inactive = if (enabled) inactiveColor else disabledColor.copy(alpha = 0.48f)
        val strokeWidthPx = 4.dp.toPx()
        val maxAmplitudePx = if (isPlaying && enabled) 7.dp.toPx() else 0f
        val wavelengthPx = 34.dp.toPx()

        if (thumbX > 0f) {
            if (maxAmplitudePx <= 0f || thumbX < wavelengthPx * 0.75f) {
                drawLine(
                    color = active,
                    start = Offset(0f, centerY),
                    end = Offset(thumbX, centerY),
                    strokeWidth = strokeWidthPx,
                    cap = StrokeCap.Round,
                )
            } else {
                val wavePath = Path().apply {
                    moveTo(0f, centerY)
                }
                val sampleStep = 1.5.dp.toPx().coerceAtLeast(1f)
                var x = sampleStep

                while (x < thumbX) {
                    val ratio = (x / thumbX).coerceIn(0f, 1f)
                    val envelope = sin(ratio * PI).toFloat()
                    val carrier = sin(
                        (x / wavelengthPx) * TWO_PI - phase,
                    )
                    val y = centerY + (envelope * maxAmplitudePx * carrier)
                    wavePath.lineTo(x, y)
                    x += sampleStep
                }
                wavePath.lineTo(thumbX, centerY)

                drawPath(
                    path = wavePath,
                    color = active,
                    style = Stroke(
                        width = strokeWidthPx,
                        cap = StrokeCap.Round,
                    ),
                )
            }
        }

        if (thumbX < size.width) {
            drawLine(
                color = inactive,
                start = Offset(thumbX, centerY),
                end = Offset(size.width, centerY),
                strokeWidth = strokeWidthPx,
                cap = StrokeCap.Round,
            )
        }
    }
}

private const val WAVE_PHASE_DURATION_MS = 1_250
private const val TWO_PI = (2.0 * PI).toFloat()
