package io.github.whoxamxl.aalyrics.ui.phone.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.github.whoxamxl.aalyrics.ui.designsystem.icon.AALyricsIcons
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsColors
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsSpacing
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsTypography

/**
 * Standard header for Phone custom dialogs that expose an explicit close affordance.
 *
 * Keeps the eyebrow and close action on one vertically centered row and fixes the
 * close affordance to a 24dp icon inside the shared 48dp touch target.
 */
@Composable
internal fun PhoneDialogHeader(
    eyebrow: String,
    closeContentDescription: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = eyebrow,
            style = AALyricsTypography.Label,
            color = AALyricsColors.AccentCyan,
            modifier = Modifier.weight(1f),
        )

        IconButton(
            onClick = onClose,
            modifier = Modifier.size(AALyricsSpacing.Space48),
        ) {
            Icon(
                imageVector = AALyricsIcons.Close,
                contentDescription = closeContentDescription,
                tint = AALyricsColors.TextSecondary,
                modifier = Modifier.size(AALyricsSpacing.Space24),
            )
        }
    }
}
