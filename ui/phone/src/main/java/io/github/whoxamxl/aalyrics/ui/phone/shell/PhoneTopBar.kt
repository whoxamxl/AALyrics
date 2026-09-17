package io.github.whoxamxl.aalyrics.ui.phone.shell

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsColors
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsRadius
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsSpacing
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsTypography

/** Compact persistent app identity and runtime-status presentation. */
@Composable
fun PhoneTopBar(
    statusText: String?,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = AALyricsColors.BackgroundSurface,
    ) {
        Row(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(
                    horizontal = AALyricsSpacing.Space16,
                    vertical = AALyricsSpacing.Space8,
                ),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(AALyricsSpacing.Space32),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(AALyricsRadius.Radius8),
                color = AALyricsColors.AccentCyan,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "A",
                        style = AALyricsTypography.AppTitle,
                        color = AALyricsColors.BackgroundBase,
                    )
                }
            }
            Spacer(Modifier.width(AALyricsSpacing.Space8))
            Text(
                text = "AALyrics",
                style = AALyricsTypography.AppTitle,
                color = AALyricsColors.TextPrimary,
                maxLines = 1,
            )
            Spacer(Modifier.weight(1f))
            if (!statusText.isNullOrBlank()) {
                Surface(
                    modifier = Modifier.widthIn(max = 160.dp),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(AALyricsRadius.Full),
                    color = AALyricsColors.OverlaySoft,
                ) {
                    Text(
                        modifier = Modifier.padding(
                            horizontal = AALyricsSpacing.Space12,
                            vertical = AALyricsSpacing.Space4,
                        ),
                        text = statusText,
                        style = AALyricsTypography.Label,
                        color = AALyricsColors.AccentCyan,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
