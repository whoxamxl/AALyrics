package io.github.whoxamxl.aalyrics.ui.phone.sync

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsColors
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsSpacing
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsTypography
import androidx.compose.ui.res.stringResource
import io.github.whoxamxl.aalyrics.ui.phone.R

/**
 * Deliberate non-functional placeholder while timing/calibration ownership is redesigned.
 */
@Composable
fun SyncScreen(
    modifier: Modifier = Modifier,
    bottomOverlayInset: Dp = 0.dp,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(
                start = AALyricsSpacing.Space20,
                top = AALyricsSpacing.Space20,
                end = AALyricsSpacing.Space20,
                bottom = bottomOverlayInset + AALyricsSpacing.Space20,
            ),
    ) {
        Text(
            text = stringResource(R.string.sync_title),
            style = AALyricsTypography.LyricsSupporting,
            color = AALyricsColors.TextPrimary,
        )
        Spacer(Modifier.height(AALyricsSpacing.Space12))
        Text(
            text = stringResource(R.string.sync_unavailable),
            style = AALyricsTypography.TrackArtist,
            color = AALyricsColors.TextSecondary,
        )
    }
}
