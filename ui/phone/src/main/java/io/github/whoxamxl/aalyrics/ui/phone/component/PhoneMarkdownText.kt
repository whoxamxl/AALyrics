package io.github.whoxamxl.aalyrics.ui.phone.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography
import com.mikepenz.markdown.model.markdownPadding
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsColors
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsTypography

/**
 * Shared Phone Markdown renderer for bundled and presentation-provided documents.
 *
 * Markdown syntax is delegated to multiplatform-markdown-renderer. AALyrics owns the
 * compact Phone presentation theme so documents remain readable inside Settings and
 * future surfaces such as Changelog.
 */
@Composable
internal fun PhoneMarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
) {
    val body = AALyricsTypography.TrackArtist
    val compactHeading = AALyricsTypography.AppTitle

    Markdown(
        content = markdown,
        modifier = modifier.fillMaxWidth(),
        colors = markdownColor(
            text = AALyricsColors.TextPrimary,
            codeBackground = AALyricsColors.OverlaySoft,
            inlineCodeBackground = AALyricsColors.OverlaySoft,
            dividerColor = AALyricsColors.BorderSoft,
            tableBackground = AALyricsColors.BackgroundSurface,
        ),
        typography = markdownTypography(
            h1 = AALyricsTypography.LyricsSupporting.copy(
                fontSize = 24.sp,
                lineHeight = 30.sp,
                fontWeight = FontWeight.Bold,
            ),
            h2 = AALyricsTypography.TrackTitle.copy(
                fontSize = 20.sp,
                lineHeight = 26.sp,
                fontWeight = FontWeight.Medium,
            ),
            h3 = compactHeading.copy(
                fontSize = 17.sp,
                lineHeight = 22.sp,
            ),
            h4 = compactHeading,
            h5 = compactHeading,
            h6 = compactHeading,
            text = body,
            paragraph = body,
            ordered = body,
            bullet = body,
            list = body,
            quote = body.copy(fontStyle = FontStyle.Italic),
            code = body.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                lineHeight = 18.sp,
            ),
            inlineCode = body.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                lineHeight = 18.sp,
            ),
            textLink = TextLinkStyles(
                style = SpanStyle(
                    color = AALyricsColors.AccentCyan,
                    fontWeight = FontWeight.Medium,
                    textDecoration = TextDecoration.Underline,
                ),
            ),
            table = body,
        ),
        padding = markdownPadding(
            block = 6.dp,
            list = 4.dp,
            listItemTop = 2.dp,
            listItemBottom = 2.dp,
            listIndent = 12.dp,
            codeBlock = PaddingValues(10.dp),
            blockQuote = PaddingValues(horizontal = 8.dp),
            blockQuoteText = PaddingValues(vertical = 4.dp),
            blockQuoteBar = PaddingValues.Absolute(
                left = 0.dp,
                top = 2.dp,
                right = 8.dp,
                bottom = 2.dp,
            ),
        ),
    )
}
