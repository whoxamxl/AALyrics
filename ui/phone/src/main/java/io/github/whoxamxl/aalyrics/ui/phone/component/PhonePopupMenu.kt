package io.github.whoxamxl.aalyrics.ui.phone.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsColors
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsRadius
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsSpacing

/**
 * Shared Phone popup surface used by compact anchored menus and tooltips.
 *
 * Keeps Quick Controls and Settings information popups visually identical.
 */
@Composable
internal fun PhonePopupMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(AALyricsRadius.Radius16)
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier
            .clip(shape),
        shape = shape,
        containerColor = AALyricsColors.BackgroundSurfaceStrong,
        tonalElevation = 0.dp,
        shadowElevation = AALyricsSpacing.Space12,
        border = BorderStroke(
            width = 1.dp,
            color = AALyricsColors.BorderSoft,
        ),
        content = content,
    )
}
