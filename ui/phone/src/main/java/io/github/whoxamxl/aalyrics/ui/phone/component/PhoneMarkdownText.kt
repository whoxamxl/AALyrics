package io.github.whoxamxl.aalyrics.ui.phone.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsColors
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsSpacing
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsTypography

/**
 * Lightweight Phone-local Markdown renderer for bundled project documents.
 *
 * Supports the Markdown currently used by the repository LICENSE without rewriting
 * the source text: H1/H2 headings, paragraphs, block quotes, bold/italic,
 * inline code, Markdown links, and angle-bracket URLs.
 */
@Composable
internal fun PhoneMarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
) {
    val blocks = parseMarkdownBlocks(markdown)

    Column(modifier = modifier.fillMaxWidth()) {
        blocks.forEachIndexed { index, block ->
            when (block) {
                is MarkdownBlock.Heading -> Text(
                    text = markdownInline(block.text),
                    style = if (block.level == 1) {
                        AALyricsTypography.LyricsSupporting
                    } else {
                        AALyricsTypography.TrackTitle
                    },
                    color = AALyricsColors.TextPrimary,
                )

                is MarkdownBlock.Paragraph -> Text(
                    text = markdownInline(block.text),
                    style = AALyricsTypography.TrackArtist,
                    color = AALyricsColors.TextPrimary,
                )

                is MarkdownBlock.Quote -> Text(
                    text = markdownInline(block.text),
                    style = AALyricsTypography.TrackArtist.copy(
                        fontStyle = FontStyle.Italic,
                    ),
                    color = AALyricsColors.TextSecondary,
                )
            }

            if (index != blocks.lastIndex) {
                Spacer(
                    Modifier.height(
                        if (block is MarkdownBlock.Heading) {
                            AALyricsSpacing.Space8
                        } else {
                            AALyricsSpacing.Space12
                        },
                    ),
                )
            }
        }
    }
}

private sealed interface MarkdownBlock {
    data class Heading(
        val level: Int,
        val text: String,
    ) : MarkdownBlock

    data class Paragraph(
        val text: String,
    ) : MarkdownBlock

    data class Quote(
        val text: String,
    ) : MarkdownBlock
}

private fun parseMarkdownBlocks(markdown: String): List<MarkdownBlock> {
    val blocks = mutableListOf<MarkdownBlock>()
    val paragraph = mutableListOf<String>()

    fun flushParagraph() {
        if (paragraph.isNotEmpty()) {
            blocks += MarkdownBlock.Paragraph(paragraph.joinToString(" "))
            paragraph.clear()
        }
    }

    markdown
        .replace("\r\n", "\n")
        .replace('\r', '\n')
        .lineSequence()
        .forEach { rawLine ->
            val line = rawLine.trimEnd()
            when {
                line.isBlank() -> flushParagraph()
                line.startsWith("# ") -> {
                    flushParagraph()
                    blocks += MarkdownBlock.Heading(
                        level = 1,
                        text = line.removePrefix("# ").trim(),
                    )
                }
                line.startsWith("## ") -> {
                    flushParagraph()
                    blocks += MarkdownBlock.Heading(
                        level = 2,
                        text = line.removePrefix("## ").trim(),
                    )
                }
                line.startsWith("> ") -> {
                    flushParagraph()
                    blocks += MarkdownBlock.Quote(
                        text = line.removePrefix("> ").trim(),
                    )
                }
                else -> paragraph += line.trim()
            }
        }

    flushParagraph()
    return blocks
}

private val inlineMarkdownPattern = Regex(
    """(\*\*\*[^*]+\*\*\*|\*\*[^*]+\*\*|\*[^*]+\*|`[^`]+`|\[[^]]+]\([^)]+\)|<https?://[^>]+>)""",
)

private fun markdownInline(text: String): AnnotatedString = buildAnnotatedString {
    var cursor = 0

    inlineMarkdownPattern.findAll(text).forEach { match ->
        append(text.substring(cursor, match.range.first))
        val token = match.value

        when {
            token.startsWith("***") -> withStyle(
                SpanStyle(
                    fontWeight = FontWeight.Bold,
                    fontStyle = FontStyle.Italic,
                ),
            ) {
                append(token.removePrefix("***").removeSuffix("***"))
            }

            token.startsWith("**") -> withStyle(
                SpanStyle(fontWeight = FontWeight.Bold),
            ) {
                append(token.removePrefix("**").removeSuffix("**"))
            }

            token.startsWith("*") -> withStyle(
                SpanStyle(fontStyle = FontStyle.Italic),
            ) {
                append(token.removePrefix("*").removeSuffix("*"))
            }

            token.startsWith("`") -> withStyle(
                SpanStyle(
                    fontFamily = FontFamily.Monospace,
                    background = AALyricsColors.OverlaySoft,
                ),
            ) {
                append(token.removePrefix("`").removeSuffix("`"))
            }

            token.startsWith("[") -> {
                val separator = token.indexOf("](")
                val label = token.substring(1, separator)
                withStyle(
                    SpanStyle(
                        color = AALyricsColors.AccentCyan,
                        textDecoration = TextDecoration.Underline,
                    ),
                ) {
                    append(label)
                }
            }

            token.startsWith("<") -> withStyle(
                SpanStyle(
                    color = AALyricsColors.AccentCyan,
                    textDecoration = TextDecoration.Underline,
                ),
            ) {
                append(token.removePrefix("<").removeSuffix(">"))
            }

            else -> append(token)
        }

        cursor = match.range.last + 1
    }

    append(text.substring(cursor))
}
