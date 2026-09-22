package io.github.whoxamxl.aalyrics.ui.phone.settings

import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.os.Build
import android.widget.ImageView
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
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
    val context = LocalContext.current
    val contentDescription = stringResource(R.string.settings_support_button_description)

    val animatedDrawable = remember(context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            runCatching {
                ImageDecoder.decodeDrawable(
                    ImageDecoder.createSource(
                        context.resources,
                        R.drawable.bmc_support_sticker,
                    ),
                )
            }.getOrNull()
        } else {
            null
        }
    }

    DisposableEffect(animatedDrawable) {
        (animatedDrawable as? AnimatedImageDrawable)?.start()
        onDispose {
            (animatedDrawable as? AnimatedImageDrawable)?.stop()
        }
    }

    if (animatedDrawable != null) {
        AndroidView(
            modifier = Modifier
                .fillMaxWidth(0.58f)
                .widthIn(max = 188.dp)
                .aspectRatio(125f / 59f),
            factory = { viewContext ->
                ImageView(viewContext).apply {
                    adjustViewBounds = false
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    isClickable = true
                    isFocusable = true
                    this.contentDescription = contentDescription
                    setOnClickListener { onSupport() }
                    setImageDrawable(animatedDrawable)
                    (animatedDrawable as? AnimatedImageDrawable)?.start()
                }
            },
            update = { imageView ->
                imageView.contentDescription = contentDescription
                imageView.setOnClickListener { onSupport() }
                if (imageView.drawable !== animatedDrawable) {
                    imageView.setImageDrawable(animatedDrawable)
                }
                (animatedDrawable as? AnimatedImageDrawable)?.start()
            },
        )
    } else {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.58f)
                .widthIn(max = 188.dp)
                .aspectRatio(125f / 59f)
                .background(
                    color = Color(0xFFFFDD00),
                    shape = RoundedCornerShape(percent = 50),
                )
                .clickable(
                    role = Role.Button,
                    onClick = onSupport,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.settings_support_fallback_label),
                style = AALyricsTypography.Label,
                color = Color(0xFF0D0C22),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = AALyricsSpacing.Space12),
            )
        }
    }
}
