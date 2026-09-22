package io.github.whoxamxl.aalyrics.ui.phone.preview

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsColors
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsSpacing
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsTheme
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsTypography
import io.github.whoxamxl.aalyrics.ui.phone.R
import io.github.whoxamxl.aalyrics.ui.phone.settings.SettingsSection
import io.github.whoxamxl.aalyrics.ui.phone.settings.SettingsSubscreenHeader

private const val BMC_BUTTON_ASPECT_RATIO = 545f / 153f

/**
 * Preview-only proposal for replacing the current GIF CTA.
 *
 * The production Support screen is intentionally left unchanged until this
 * visual direction is approved.
 */
@Composable
private fun SupportBmcVectorCtaProposal(
    onClick: () -> Unit,
    shimmerProgress: Float,
    pulseScale: Float,
    rotationDegrees: Float,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(percent = 16)
    val progress = shimmerProgress.coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = pulseScale
                scaleY = pulseScale
                rotationZ = rotationDegrees
            }
            .clip(shape)
            .clickable(
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.bmc_button_preview),
            contentDescription =
                stringResource(R.string.settings_support_button_description),
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
        )

        Canvas(Modifier.matchParentSize()) {
            val bandWidth = size.width * 0.24f
            val travel = size.width + (bandWidth * 2f)
            val leadingX = (travel * progress) - bandWidth

            drawRoundRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.White.copy(alpha = 0.05f),
                        Color.White.copy(alpha = 0.30f),
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

@Composable
private fun SupportBmcVectorAnimatedCtaProposal(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "supportBmcPreview")

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
        initialValue = 0.992f,
        targetValue = 1.008f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1_250,
                easing = FastOutSlowInEasing,
            ),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "supportBmcPulse",
    )

    val rotationDegrees by transition.animateFloat(
        initialValue = -0.35f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1_450,
                easing = FastOutSlowInEasing,
            ),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "supportBmcTilt",
    )

    SupportBmcVectorCtaProposal(
        onClick = onClick,
        shimmerProgress = shimmerProgress,
        pulseScale = pulseScale,
        rotationDegrees = rotationDegrees,
        modifier = modifier,
    )
}

@Composable
private fun SupportAALyricsVectorProposalScreen(
    shimmerProgress: Float,
    pulseScale: Float,
    rotationDegrees: Float,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(
                start = AALyricsSpacing.Space16,
                top = AALyricsSpacing.Space8,
                end = AALyricsSpacing.Space16,
                bottom = AALyricsSpacing.Space16,
            ),
    ) {
        SettingsSubscreenHeader(
            title = stringResource(R.string.settings_support_aalyrics),
            backContentDescription = stringResource(R.string.settings_back),
            onBack = {},
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

                SupportBmcVectorCtaProposal(
                    onClick = {},
                    shimmerProgress = shimmerProgress,
                    pulseScale = pulseScale,
                    rotationDegrees = rotationDegrees,
                    modifier = Modifier
                        .fillMaxWidth(0.58f)
                        .widthIn(max = 188.dp)
                        .aspectRatio(BMC_BUTTON_ASPECT_RATIO),
                )

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

@Preview(
    name = "CTA · Static",
    group = "Support SVG Proposal",
    widthDp = 412,
    heightDp = 180,
)
@Composable
private fun SupportBmcVectorCtaStaticPreview() {
    AALyricsTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AALyricsColors.BackgroundBase)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SupportBmcVectorCtaProposal(
                onClick = {},
                shimmerProgress = 0f,
                pulseScale = 1f,
                rotationDegrees = 0f,
                modifier = Modifier
                    .fillMaxWidth(0.58f)
                    .widthIn(max = 188.dp)
                    .aspectRatio(BMC_BUTTON_ASPECT_RATIO),
            )
        }
    }
}

@Preview(
    name = "CTA · Interactive animation",
    group = "Support SVG Proposal",
    widthDp = 412,
    heightDp = 180,
)
@Composable
private fun SupportBmcVectorCtaInteractivePreview() {
    AALyricsTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AALyricsColors.BackgroundBase)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SupportBmcVectorAnimatedCtaProposal(
                onClick = {},
                modifier = Modifier
                    .fillMaxWidth(0.58f)
                    .widthIn(max = 188.dp)
                    .aspectRatio(BMC_BUTTON_ASPECT_RATIO),
            )
        }
    }
}

@Preview(
    name = "CTA · Motion snapshot",
    group = "Support SVG Proposal",
    widthDp = 412,
    heightDp = 180,
)
@Composable
private fun SupportBmcVectorCtaMotionPreview() {
    AALyricsTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AALyricsColors.BackgroundBase)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SupportBmcVectorCtaProposal(
                onClick = {},
                shimmerProgress = 0.48f,
                pulseScale = 1.012f,
                rotationDegrees = 0.35f,
                modifier = Modifier
                    .fillMaxWidth(0.58f)
                    .widthIn(max = 188.dp)
                    .aspectRatio(BMC_BUTTON_ASPECT_RATIO),
            )
        }
    }
}

@Preview(
    name = "Screen · Proposed",
    group = "Support SVG Proposal",
    widthDp = 412,
    heightDp = 760,
)
@Composable
private fun SupportAALyricsVectorProposalPreview() {
    AALyricsTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AALyricsColors.BackgroundBase),
        ) {
            SupportAALyricsVectorProposalScreen(
                shimmerProgress = 0.48f,
                pulseScale = 1.012f,
                rotationDegrees = 0.35f,
            )
        }
    }
}

@Preview(
    name = "Screen · Interactive animation",
    group = "Support SVG Proposal",
    widthDp = 412,
    heightDp = 760,
)
@Composable
private fun SupportAALyricsVectorInteractivePreview() {
    val transition = rememberInfiniteTransition(label = "supportBmcScreenPreview")

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
        label = "supportBmcScreenShimmer",
    )

    val pulseScale by transition.animateFloat(
        initialValue = 0.992f,
        targetValue = 1.008f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1_250,
                easing = FastOutSlowInEasing,
            ),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "supportBmcScreenPulse",
    )

    val rotationDegrees by transition.animateFloat(
        initialValue = -0.35f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1_450,
                easing = FastOutSlowInEasing,
            ),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "supportBmcScreenTilt",
    )

    AALyricsTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AALyricsColors.BackgroundBase),
        ) {
            SupportAALyricsVectorProposalScreen(
                shimmerProgress = shimmerProgress,
                pulseScale = pulseScale,
                rotationDegrees = rotationDegrees,
            )
        }
    }
}

@Preview(
    name = "Screen · Narrow 320dp",
    group = "Support SVG Proposal",
    widthDp = 320,
    heightDp = 700,
)
@Composable
private fun SupportAALyricsVectorProposalNarrowPreview() {
    AALyricsTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AALyricsColors.BackgroundBase),
        ) {
            SupportAALyricsVectorProposalScreen(
                shimmerProgress = 0.48f,
                pulseScale = 1.012f,
                rotationDegrees = 0.35f,
            )
        }
    }
}

@Preview(
    name = "Screen · Enlarged font",
    group = "Support SVG Proposal",
    widthDp = 412,
    heightDp = 820,
    fontScale = 1.4f,
)
@Composable
private fun SupportAALyricsVectorProposalLargeFontPreview() {
    AALyricsTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AALyricsColors.BackgroundBase),
        ) {
            SupportAALyricsVectorProposalScreen(
                shimmerProgress = 0.48f,
                pulseScale = 1.012f,
                rotationDegrees = 0.35f,
            )
        }
    }
}
