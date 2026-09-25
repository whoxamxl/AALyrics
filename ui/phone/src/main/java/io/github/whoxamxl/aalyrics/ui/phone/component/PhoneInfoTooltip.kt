package io.github.whoxamxl.aalyrics.ui.phone.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.whoxamxl.aalyrics.ui.designsystem.icon.AALyricsIcons
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsColors
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsSpacing
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsTypography

/**
 * Shared compact information tooltip for Phone surfaces.
 *
 * Use the shared semantic Info icon so Settings, Details, and future
 * failure-capable diagnostic rows use one visual vocabulary.
 */
@Composable
internal fun PhoneInfoTooltip(
    text: String,
    contentDescription: String,
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier.size(AALyricsSpacing.Space48),
        ) {
            Icon(
                imageVector = AALyricsIcons.Info,
                contentDescription = contentDescription,
                tint = AALyricsColors.TextSecondary,
                modifier = Modifier.size(AALyricsSpacing.Space20),
            )
        }

        PhonePopupMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.widthIn(max = 280.dp),
        ) {
            Text(
                text = text,
                style = AALyricsTypography.TrackArtist,
                color = AALyricsColors.TextPrimary,
                modifier = Modifier.padding(AALyricsSpacing.Space16),
            )
        }
    }
}
