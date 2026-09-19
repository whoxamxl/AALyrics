package io.github.whoxamxl.aalyrics.ui.phone.setup

import android.content.Context
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import io.github.whoxamxl.aalyrics.ui.designsystem.icon.AALyricsBrandMark
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsColors
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsSpacing
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsTheme
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsTypography
import io.github.whoxamxl.aalyrics.ui.phone.R

/**
 * Android View adapter used by the application-entry Activity.
 *
 * Compatibility policy and acknowledgement persistence stay in :app.
 */
fun createAndroidAutoCompatibilitySetupView(
    context: Context,
    onEnabled: () -> Unit,
    onContinueWithout: () -> Unit,
): View =
    ComposeView(context).apply {
        setContent {
            AALyricsTheme {
                AndroidAutoCompatibilitySetupScreen(
                    onEnabled = onEnabled,
                    onContinueWithout = onContinueWithout,
                )
            }
        }
    }

@Composable
fun AndroidAutoCompatibilitySetupScreen(
    onEnabled: () -> Unit,
    onContinueWithout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AALyricsColors.BackgroundBase)
            .verticalScroll(rememberScrollState())
            .padding(
                horizontal = AALyricsSpacing.Space32,
                vertical = AALyricsSpacing.Space48,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        AALyricsBrandMark(contentDescription = null)

        Spacer(Modifier.height(AALyricsSpacing.Space24))

        Text(
            text = stringResource(R.string.android_auto_compat_eyebrow),
            style = AALyricsTypography.Label,
            color = AALyricsColors.AccentCyan,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(AALyricsSpacing.Space12))

        Text(
            text = stringResource(R.string.android_auto_compat_title),
            style = AALyricsTypography.LyricsCurrent,
            color = AALyricsColors.TextPrimary,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(AALyricsSpacing.Space16))

        Text(
            text = stringResource(R.string.android_auto_compat_body),
            style = AALyricsTypography.TrackArtist,
            color = AALyricsColors.TextSecondary,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(AALyricsSpacing.Space24))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(AALyricsSpacing.Space12),
        ) {
            CompatibilityStep(1, stringResource(R.string.android_auto_compat_step_1))
            CompatibilityStep(2, stringResource(R.string.android_auto_compat_step_2))
            CompatibilityStep(3, stringResource(R.string.android_auto_compat_step_3))
            CompatibilityStep(4, stringResource(R.string.android_auto_compat_step_4))
        }

        Spacer(Modifier.height(AALyricsSpacing.Space24))

        Text(
            text = stringResource(R.string.android_auto_compat_verification_note),
            style = AALyricsTypography.Label,
            color = AALyricsColors.TextTertiary,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(AALyricsSpacing.Space32))

        Button(
            onClick = onEnabled,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = stringResource(R.string.android_auto_compat_enabled))
        }

        Spacer(Modifier.height(AALyricsSpacing.Space12))

        OutlinedButton(
            onClick = onContinueWithout,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = stringResource(R.string.android_auto_compat_skip))
        }

        Spacer(Modifier.height(AALyricsSpacing.Space16))

        Text(
            text = stringResource(R.string.android_auto_compat_footer),
            style = AALyricsTypography.Label,
            color = AALyricsColors.TextTertiary,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun CompatibilityStep(
    number: Int,
    text: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier.size(AALyricsSpacing.Space32),
            shape = CircleShape,
            color = AALyricsColors.BackgroundSurfaceStrong,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = number.toString(),
                    style = AALyricsTypography.Label,
                    color = AALyricsColors.AccentCyan,
                )
            }
        }

        Text(
            text = text,
            style = AALyricsTypography.TrackArtist,
            color = AALyricsColors.TextPrimary,
            modifier = Modifier.padding(start = AALyricsSpacing.Space12),
        )
    }
}
