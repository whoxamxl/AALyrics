package io.github.whoxamxl.aalyrics.ui.phone.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.github.whoxamxl.aalyrics.ui.designsystem.icon.AALyricsIcons
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsColors
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsSpacing
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsStroke
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsTypography
import io.github.whoxamxl.aalyrics.ui.phone.navigation.PhoneDestination

/** Compact persistent four-destination selector. Navigation/back-stack ownership is external. */
@Composable
fun PhoneNavigationBar(
    selectedDestination: PhoneDestination,
    onDestinationSelected: (PhoneDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = AALyricsColors.BackgroundChrome,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(PhoneNavigationBarHeight)
                .selectableGroup(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PhoneDestination.entries.forEach { destination ->
                val selected = destination == selectedDestination
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .selectable(
                            selected = selected,
                            onClick = { onDestinationSelected(destination) },
                            role = Role.Tab,
                        )
                        .padding(
                            horizontal = AALyricsSpacing.Space4,
                            vertical = AALyricsSpacing.Space4,
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Icon(
                        imageVector = destination.icon,
                        contentDescription = null,
                        tint = if (selected) {
                            AALyricsColors.AccentCyan
                        } else {
                            AALyricsColors.TextTertiary
                        },
                        modifier = Modifier.size(AALyricsSpacing.Space24),
                    )
                    Text(
                        text = destination.label,
                        style = AALyricsTypography.Label,
                        color = if (selected) {
                            AALyricsColors.TextPrimary
                        } else {
                            AALyricsColors.TextSecondary
                        },
                        maxLines = 1,
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(AALyricsStroke.Strong)
                            .background(
                                if (selected) AALyricsColors.AccentCyan else Color.Transparent,
                            ),
                    )
                }
            }
        }
    }
}

private val PhoneNavigationBarHeight = 56.dp

private val PhoneDestination.icon: ImageVector
    get() = when (this) {
        PhoneDestination.Lyrics -> AALyricsIcons.Lyrics
        PhoneDestination.Sync -> AALyricsIcons.Sync
        PhoneDestination.Details -> AALyricsIcons.Details
        PhoneDestination.Settings -> AALyricsIcons.Settings
    }
