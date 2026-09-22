package io.github.whoxamxl.aalyrics.ui.phone.settings

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
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
                SupportCoffeeAnimation()

                Spacer(Modifier.height(AALyricsSpacing.Space16))

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

                Image(
                    painter = painterResource(R.drawable.bmc_button),
                    contentDescription =
                        stringResource(R.string.settings_support_button_description),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth(0.72f)
                        .widthIn(max = 244.dp)
                        .aspectRatio(720f / 202f)
                        .clip(RoundedCornerShape(percent = 50))
                        .clickable(
                            role = Role.Button,
                            onClick = onSupport,
                        ),
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

@Composable
private fun SupportCoffeeAnimation() {
    val transition = rememberInfiniteTransition(label = "supportCoffee")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1_350,
                easing = FastOutSlowInEasing,
            ),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "supportCoffeePhase",
    )
    val density = LocalDensity.current
    val liftPx = with(density) { 4.dp.toPx() }

    Image(
        painter = painterResource(R.drawable.bmc_logo),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .width(56.dp)
            .aspectRatio(177f / 256f)
            .graphicsLayer {
                translationY = -liftPx * phase
                rotationZ = -1.5f + (3f * phase)
                scaleX = 0.98f + (0.02f * phase)
                scaleY = scaleX
            },
    )
}
