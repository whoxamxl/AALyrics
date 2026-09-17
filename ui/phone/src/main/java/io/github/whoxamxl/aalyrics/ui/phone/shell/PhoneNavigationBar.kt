package io.github.whoxamxl.aalyrics.ui.phone.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsColors
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsSpacing
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsStroke
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsTypography
import io.github.whoxamxl.aalyrics.ui.phone.navigation.PhoneDestination

/** Persistent four-destination selector. Navigation/back-stack ownership is external. */
@Composable
fun PhoneNavigationBar(
    selectedDestination: PhoneDestination,
    onDestinationSelected: (PhoneDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(
        modifier = modifier,
        containerColor = AALyricsColors.BackgroundSurface,
        contentColor = AALyricsColors.TextPrimary,
    ) {
        PhoneDestination.entries.forEach { destination ->
            val selected = destination == selectedDestination
            NavigationBarItem(
                selected = selected,
                onClick = { onDestinationSelected(destination) },
                icon = {
                    Box(
                        modifier = Modifier
                            .width(AALyricsSpacing.Space24)
                            .height(AALyricsStroke.Strong)
                            .background(if (selected) AALyricsColors.AccentCyan else Color.Transparent),
                    )
                },
                label = {
                    Text(
                        text = destination.label,
                        style = AALyricsTypography.Label,
                        maxLines = 1,
                    )
                },
                alwaysShowLabel = true,
                colors = NavigationBarItemDefaults.colors(
                    selectedTextColor = AALyricsColors.TextPrimary,
                    unselectedTextColor = AALyricsColors.TextSecondary,
                    indicatorColor = Color.Transparent,
                ),
            )
        }
    }
}
