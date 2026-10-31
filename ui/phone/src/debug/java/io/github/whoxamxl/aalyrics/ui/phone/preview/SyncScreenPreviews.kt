package io.github.whoxamxl.aalyrics.ui.phone.preview

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsTheme
import io.github.whoxamxl.aalyrics.ui.phone.lyrics.LyricsViewportLineUiState
import io.github.whoxamxl.aalyrics.ui.phone.sync.SyncAvailability
import io.github.whoxamxl.aalyrics.ui.phone.sync.SyncOffsetEditorDialog
import io.github.whoxamxl.aalyrics.ui.phone.sync.SyncScreen
import io.github.whoxamxl.aalyrics.ui.phone.sync.SyncScreenUiState
import io.github.whoxamxl.aalyrics.ui.phone.sync.TapSyncAvailability
import io.github.whoxamxl.aalyrics.ui.phone.sync.TapSyncUiState

@Preview(name = "Timed lyrics and translation", group = "Sync", widthDp = 412, heightDp = 820)
@Preview(name = "Narrow and large font", group = "Sync", widthDp = 320, heightDp = 640, fontScale = 1.3f)
@Preview(name = "Landscape", group = "Sync", widthDp = 800, heightDp = 360)
@Composable
private fun SyncTimedPreview() = SyncPreview(
    SyncScreenUiState(
        offsetMs = 300L,
        availability = SyncAvailability.TIMED,
        trackTitle = "Evening lights",
        canAdvance = true,
        canDelay = true,
        previousLine = LyricsViewportLineUiState("A quiet road"),
        currentLine = LyricsViewportLineUiState("The lights are turning blue", translatedText = "灯光渐渐变蓝"),
        nextLine = LyricsViewportLineUiState("We walk toward the morning"),
        tapSync = TapSyncUiState(availability = TapSyncAvailability.READY),
    ),
)

@Preview(name = "No media with saved offset", group = "Sync", widthDp = 360, heightDp = 740)
@Composable
private fun SyncNoMediaPreview() = SyncPreview(SyncScreenUiState(offsetMs = -500L))

@Preview(name = "Plain lyrics", group = "Sync", widthDp = 360, heightDp = 740)
@Composable
private fun SyncPlainPreview() = SyncPreview(
    SyncScreenUiState(availability = SyncAvailability.PLAIN, trackTitle = "Plain lyrics"),
)

@Preview(name = "Advance limit", group = "Sync", widthDp = 360, heightDp = 740)
@Composable
private fun SyncLimitPreview() = SyncPreview(
    SyncScreenUiState(offsetMs = 30_000L, availability = SyncAvailability.TIMED, canDelay = true),
)

@Preview(name = "Tap calibration - second line", group = "Sync", widthDp = 412, heightDp = 820)
@Preview(name = "Tap calibration - narrow large font", group = "Sync", widthDp = 320, heightDp = 640, fontScale = 1.3f)
@Composable
private fun SyncTapListeningPreview() = SyncPreview(
    SyncScreenUiState(
        offsetMs = 300L,
        availability = SyncAvailability.TIMED,
        canAdvance = true,
        canDelay = true,
        tapSync = TapSyncUiState(
            availability = TapSyncAvailability.READY,
            active = true,
            lines = listOf("A quiet road", "The lights are turning blue", "We walk toward the morning"),
            completedTaps = 1,
        ),
    ),
)

@Preview(name = "Tap calibration saved", group = "Sync", widthDp = 412, heightDp = 820)
@Composable
private fun SyncTapSavedPreview() = SyncPreview(
    SyncScreenUiState(
        offsetMs = -450L,
        tapSync = TapSyncUiState(availability = TapSyncAvailability.READY, savedOffsetMs = -450L),
    ),
)

@Preview(name = "Not enough remaining lines", group = "Sync", widthDp = 360, heightDp = 740)
@Composable
private fun SyncTapEndPreview() = SyncPreview(
    SyncScreenUiState(tapSync = TapSyncUiState(availability = TapSyncAvailability.NOT_ENOUGH_LINES)),
)

@Preview(name = "Exact offset editor", group = "Sync", widthDp = 412, heightDp = 820)
@Preview(name = "Offset editor - narrow large font", group = "Sync", widthDp = 320, heightDp = 640, fontScale = 1.3f)
@Preview(name = "Offset editor - landscape", group = "Sync", widthDp = 800, heightDp = 360)
@Composable
private fun SyncOffsetEditorPreview() = OffsetEditorPreview("-275")

@Preview(name = "Offset outside range", group = "Sync", widthDp = 360, heightDp = 740)
@Composable
private fun SyncOffsetRangeErrorPreview() = OffsetEditorPreview("30001")

@Preview(name = "Offset needs an integer", group = "Sync", widthDp = 360, heightDp = 740)
@Composable
private fun SyncOffsetInvalidPreview() = OffsetEditorPreview("-")

@Composable
private fun OffsetEditorPreview(text: String) {
    var draft by remember { mutableStateOf(TextFieldValue(text, TextRange(0, text.length))) }
    AALyricsTheme {
        SyncOffsetEditorDialog(
            draft = draft,
            maxOffsetMs = 30_000L,
            onDraftChange = { draft = it },
            onSave = {},
            onDismissRequest = {},
        )
    }
}

@Composable
private fun SyncPreview(state: SyncScreenUiState) {
    AALyricsTheme {
        Surface {
            SyncScreen(
                state = state,
                onAdjust = {},
                onReset = {},
                onSetOffset = {},
                maxOffsetMs = 30_000L,
                bottomOverlayInset = 80.dp,
            )
        }
    }
}
