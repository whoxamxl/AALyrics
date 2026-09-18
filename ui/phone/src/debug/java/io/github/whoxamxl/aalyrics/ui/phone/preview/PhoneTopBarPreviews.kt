package io.github.whoxamxl.aalyrics.ui.phone.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsTheme
import io.github.whoxamxl.aalyrics.ui.phone.shell.PhoneTopBar

@Preview(
    name = "No status",
    group = "PhoneTopBar",
    widthDp = 412,
    heightDp = 160,
    showBackground = true,
)
@Composable
private fun PhoneTopBarNoStatusPreview() {
    PhoneTopBarPreview(statusText = null)
}

@Preview(
    name = "Word sync",
    group = "PhoneTopBar",
    widthDp = 412,
    heightDp = 160,
    showBackground = true,
)
@Composable
private fun PhoneTopBarWordSyncPreview() {
    PhoneTopBarPreview(statusText = "WORD SYNC")
}

@Preview(
    name = "Line sync",
    group = "PhoneTopBar",
    widthDp = 412,
    heightDp = 160,
    showBackground = true,
)
@Composable
private fun PhoneTopBarLineSyncPreview() {
    PhoneTopBarPreview(statusText = "LINE SYNC")
}

@Preview(
    name = "Loading",
    group = "PhoneTopBar",
    widthDp = 412,
    heightDp = 160,
    showBackground = true,
)
@Composable
private fun PhoneTopBarLoadingPreview() {
    PhoneTopBarPreview(statusText = "Loading lyrics")
}

@Preview(
    name = "Long status",
    group = "PhoneTopBar",
    widthDp = 412,
    heightDp = 160,
    showBackground = true,
)
@Composable
private fun PhoneTopBarLongStatusPreview() {
    PhoneTopBarPreview(statusText = "Synchronizing word-by-word lyrics")
}

@Preview(
    name = "Narrow",
    group = "PhoneTopBar",
    widthDp = 320,
    heightDp = 160,
    showBackground = true,
)
@Composable
private fun PhoneTopBarNarrowPreview() {
    PhoneTopBarPreview(statusText = "WORD SYNC")
}

@Composable
private fun PhoneTopBarPreview(statusText: String?) {
    AALyricsTheme {
        PhoneTopBar(statusText = statusText)
    }
}
