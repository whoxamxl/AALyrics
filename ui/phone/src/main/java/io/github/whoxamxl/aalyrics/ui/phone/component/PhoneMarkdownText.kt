package io.github.whoxamxl.aalyrics.ui.phone.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mikepenz.markdown.m3.Markdown

/**
 * Shared Phone Markdown renderer for bundled and presentation-provided documents.
 *
 * Parsing and Markdown syntax support are delegated to multiplatform-markdown-renderer.
 * AALyrics owns only the surrounding presentation/layout contract.
 */
@Composable
internal fun PhoneMarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
) {
    Markdown(
        content = markdown,
        modifier = modifier,
        retainState = true,
    )
}
