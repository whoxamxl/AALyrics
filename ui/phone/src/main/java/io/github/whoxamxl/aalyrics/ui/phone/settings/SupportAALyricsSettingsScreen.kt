package io.github.whoxamxl.aalyrics.ui.phone.settings

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsColors
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsSpacing
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsTypography
import io.github.whoxamxl.aalyrics.ui.phone.R

private const val BMC_BUTTON_ASPECT_RATIO = 545f / 153f

/** Native Settings landing surface for the external AALyrics support destination. */
@Composable
internal fun SupportAALyricsSettingsScreen(
    onSupport: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    bottomOverlayInset: Dp = 0.dp,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                start = AALyricsSpacing.Space16,
                top = AALyricsSpacing.Space8,
                end = AALyricsSpacing.Space16,
                bottom = bottomOverlayInset + AALyricsSpacing.Space16,
            ),
    ) {
        SettingsSubscreenHeader(
            title = stringResource(R.string.settings_support_aalyrics),
            backContentDescription = stringResource(R.string.settings_back),
            onBack = onBack,
        )

        Spacer(Modifier.height(AALyricsSpacing.Space12))

        SettingsSection(title = null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AALyricsSpacing.Space20),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.settings_support_body),
                    style = AALyricsTypography.AppTitle,
                    color = AALyricsColors.TextPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(AALyricsSpacing.Space12))

                Text(
                    text = stringResource(R.string.settings_support_subtext),
                    style = AALyricsTypography.TrackArtist,
                    color = AALyricsColors.TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(AALyricsSpacing.Space32))

                SupportStickerButton(onSupport = onSupport)

                Spacer(Modifier.height(AALyricsSpacing.Space8))

                Text(
                    text = stringResource(R.string.settings_support_open_note),
                    style = AALyricsTypography.Label,
                    color = AALyricsColors.TextTertiary,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun SupportStickerButton(
    onSupport: () -> Unit,
) {
    val transition = rememberInfiniteTransition(label = "supportBmcCta")

    val shimmerProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1_800,
                easing = LinearEasing,
            ),
            repeatMode = RepeatMode.Restart,
        ),
        label = "supportBmcShimmer",
    )

    val pulseScale by transition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1_250,
                easing = FastOutSlowInEasing,
            ),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "supportBmcPulse",
    )

    val shape = RoundedCornerShape(percent = 16)

    Box(
        modifier = Modifier
            .fillMaxWidth(0.58f)
            .widthIn(max = 188.dp)
            .aspectRatio(BMC_BUTTON_ASPECT_RATIO)
            .graphicsLayer {
                scaleX = pulseScale
                scaleY = pulseScale
            }
            .clip(shape)
            .clickable(
                role = Role.Button,
                onClick = onSupport,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.bmc_button),
            contentDescription = stringResource(R.string.settings_support_button_description),
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
        )

        Canvas(Modifier.matchParentSize()) {
            val bandWidth = size.width * 0.30f
            val travel = size.width + (bandWidth * 2f)
            val leadingX = (travel * shimmerProgress) - bandWidth

            drawRoundRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.White.copy(alpha = 0.05f),
                        Color.White.copy(alpha = 0.38f),
                        Color.White.copy(alpha = 0.05f),
                        Color.Transparent,
                    ),
                    start = Offset(
                        x = leadingX - bandWidth,
                        y = size.height,
                    ),
                    end = Offset(
                        x = leadingX + bandWidth,
                        y = 0f,
                    ),
                ),
                cornerRadius = CornerRadius(
                    x = size.height * 0.16f,
                    y = size.height * 0.16f,
                ),
            )
        }
    }
}
