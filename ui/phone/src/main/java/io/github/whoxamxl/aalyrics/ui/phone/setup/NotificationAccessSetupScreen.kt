package io.github.whoxamxl.aalyrics.ui.phone.setup

import android.content.Context
import android.view.View
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
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
 * Notification-access ownership stays in :app; this module owns only the phone presentation.
 */
fun createNotificationAccessSetupView(
    context: Context,
    onGrantAccess: () -> Unit,
): View =
    ComposeView(context).apply {
        setContent {
            AALyricsTheme {
                NotificationAccessSetupScreen(onGrantAccess = onGrantAccess)
            }
        }
    }

@Composable
fun NotificationAccessSetupScreen(
    onGrantAccess: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
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
            text = stringResource(R.string.notification_access_eyebrow),
            style = AALyricsTypography.Label,
            color = AALyricsColors.AccentCyan,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(AALyricsSpacing.Space12))

        Text(
            text = stringResource(R.string.notification_access_title),
            style = AALyricsTypography.LyricsCurrent,
            color = AALyricsColors.TextPrimary,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(AALyricsSpacing.Space16))

        Text(
            text = stringResource(R.string.notification_access_body),
            style = AALyricsTypography.TrackArtist,
            color = AALyricsColors.TextSecondary,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(AALyricsSpacing.Space12))

        Text(
            text = stringResource(R.string.notification_access_reason),
            style = AALyricsTypography.Label,
            color = AALyricsColors.TextTertiary,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(AALyricsSpacing.Space32))

        Button(
            onClick = onGrantAccess,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = stringResource(R.string.notification_access_grant))
        }

        Spacer(Modifier.height(AALyricsSpacing.Space16))

        Text(
            text = stringResource(R.string.notification_access_footer),
            style = AALyricsTypography.Label,
            color = AALyricsColors.TextTertiary,
            textAlign = TextAlign.Center,
        )
    }
}
