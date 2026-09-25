package io.github.whoxamxl.aalyrics.ui.phone.lyrics

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsSpacing

/** Production content for the Phone Lyrics destination. Persistent shell chrome stays outside. */
@Composable
fun LyricsScreen(
    state: LyricsScreenUiState,
    modifier: Modifier = Modifier,
    bottomOverlayInset: Dp = 0.dp,
    artwork: (@Composable BoxScope.() -> Unit)? = null,
    onViewportInteractionModeChange: (LyricsViewportInteractionMode) -> Unit = {},
    onTranslationRetry: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxSize(),
    ) {
        TrackCard(
            state = state.trackCard,
            modifier = Modifier.padding(
                start = AALyricsSpacing.Space16,
                top = AALyricsSpacing.Space16,
                end = AALyricsSpacing.Space16,
            ),
            artwork = artwork,
            onTranslationRetry = onTranslationRetry,
        )

        Spacer(Modifier.height(AALyricsSpacing.Space12))

        LyricsViewport(
            state = state.viewport,
            modifier = Modifier
                .weight(1f)
                .padding(bottom = bottomOverlayInset),
            onInteractionModeChange = onViewportInteractionModeChange,
        )
    }
}
