package io.github.whoxamxl.aalyrics.ui.designsystem.icon

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import io.github.whoxamxl.aalyrics.ui.designsystem.R

private val AALyricsBrandMarkSize = 45.dp

/** Foreground-only AALyrics brand mark derived 1:1 from the Android foreground source. */
@Composable
fun AALyricsBrandMark(
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    Image(
        painter = painterResource(R.drawable.ic_aalyrics_mark),
        contentDescription = contentDescription,
        modifier = modifier.size(AALyricsBrandMarkSize),
    )
}
