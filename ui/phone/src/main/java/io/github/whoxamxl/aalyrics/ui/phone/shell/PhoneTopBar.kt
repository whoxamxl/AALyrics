package io.github.whoxamxl.aalyrics.ui.phone.shell

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.whoxamxl.aalyrics.ui.designsystem.icon.AALyricsBrandMark
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsColors
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsRadius
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsSpacing
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsStroke
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsTypography

/** Compact persistent app identity and connected-media-source presentation. */
@Composable
fun PhoneTopBar(
    mediaSourceLabel: String?,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = AALyricsColors.BackgroundChrome,
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
            AALyricsBrandMark(contentDescription = "AALyrics")
            Spacer(Modifier.width(AALyricsSpacing.Space8))
            Text(
                text = "AALyrics",
                style = AALyricsTypography.AppTitle,
                color = AALyricsColors.TextPrimary,
                maxLines = 1,
            )
            Spacer(Modifier.weight(1f))
            if (!mediaSourceLabel.isNullOrBlank()) {
                Surface(
                    modifier = Modifier.widthIn(max = 168.dp),
                    shape = RoundedCornerShape(AALyricsRadius.Full),
                    color = AALyricsColors.OverlaySoft,
                    border = BorderStroke(
                        width = AALyricsStroke.Thin,
                        color = AALyricsColors.BorderSoft,
                    ),
                ) {
                    Row(
                        modifier = Modifier.padding(
                            horizontal = AALyricsSpacing.Space8,
                            vertical = AALyricsSpacing.Space4,
                        ),
                        horizontalArrangement = Arrangement.spacedBy(AALyricsSpacing.Space4),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(AALyricsSpacing.Space4)
                                .background(
                                    color = AALyricsColors.AccentCyan,
                                    shape = CircleShape,
                                ),
                        )
                        Text(
                            text = mediaSourceLabel,
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
}
