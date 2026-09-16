package io.github.whoxamxl.aalyrics.provider.matching

import java.text.Normalizer
import java.util.Locale

private const val ENGLISH_VERSION_MARKER =
    "(?:live|acoustic|remix(?:ed)?|remaster(?:ed)?|instrumental|edit(?:ed)?|extended|demo)"
private const val JAPANESE_VERSION_MARKER =
    "(?:ライブ|アコースティック|リミックス|リマスター|インストゥルメンタル|インスト|エディット|エクステンデッド|デモ)(?:版|盤|バージョン)?"

private val BRACKETED_VERSION_CONTEXT = Regex("""[\(\[].*?[\)\]]""")
private val VERSION_SEPARATOR = Regex("""\s[-–—]\s|:\s*""")
private val LIVE_LOCATION_CONTEXT = Regex(
    """^\s*(?:live|ライブ)\s+(?:at|from|in)\b.*$""",
    RegexOption.IGNORE_CASE,
)
private val NAMED_REMIX_TITLE_CONTEXT = Regex(
    """^\s*.+\s+remix\s*$""",
    RegexOption.IGNORE_CASE,
)
private val ENGLISH_VERSION_TOKEN = Regex(
    """\b$ENGLISH_VERSION_MARKER\b""",
    RegexOption.IGNORE_CASE,
)
private val CONTEXT_TOKEN = Regex("""[\p{L}\p{N}]+""")
private val ENGLISH_ORDINAL_TOKEN = Regex("""\d+(?:st|nd|rd|th)""", RegexOption.IGNORE_CASE)
private val JAPANESE_VERSION_CONTEXT = Regex(
    """^\s*(?:(?:\d{4}年?|\d+\s*周年)\s*)?$JAPANESE_VERSION_MARKER(?:\s*[・･/+＆&]\s*$JAPANESE_VERSION_MARKER)*(?:\s*(?:記念|エディション))?\s*$""",
)
private val ALLOWED_ENGLISH_CONTEXT_WORDS = setOf(
    "live",
    "acoustic",
    "remix",
    "remixed",
    "remaster",
    "remastered",
    "instrumental",
    "edit",
    "edited",
    "extended",
    "demo",
    "deluxe",
    "expanded",
    "bonus",
    "anniversary",
    "edition",
    "explicit",
    "version",
    "ver",
    "radio",
    "mix",
    "performance",
    "session",
    "concert",
)

object RecordingVersionContext {
    fun extractAlbumVersionQualifiers(value: String): Set<String> {
        if (value.isBlank()) return emptySet()

        val normalized = Normalizer.normalize(value, Normalizer.Form.NFKC)
        val contexts = buildList {
            BRACKETED_VERSION_CONTEXT.findAll(normalized).forEach { match ->
                val inner = match.value.substring(1, match.value.length - 1).trim()
                trailingExplicitVersionContext(inner)?.let(::add)
            }
            trailingExplicitVersionContext(normalized)?.let(::add)
        }

        return contexts
            .flatMap { MetadataMatching.extractVersionQualifiers(it).toList() }
            .toSet()
    }

    fun extractTitleVersionQualifiers(value: String): Set<String> {
        if (value.isBlank()) return emptySet()

        val normalized = Normalizer.normalize(value, Normalizer.Form.NFKC)
        val contexts = buildList {
            BRACKETED_VERSION_CONTEXT.findAll(normalized).forEach { match ->
                val inner = match.value.substring(1, match.value.length - 1).trim()
                trailingExplicitVersionContext(
                    value = inner,
                    classifier = ::isExplicitTitleVersionContext,
                )?.let(::add)
            }

            trailingExplicitVersionContext(
                value = normalized,
                allowWholeContext = false,
                classifier = ::isExplicitTitleVersionContext,
            )?.let(::add)
        }

        return contexts
            .flatMap { MetadataMatching.extractVersionQualifiers(it).toList() }
            .toSet()
    }

    private fun trailingExplicitVersionContext(
        value: String,
        allowWholeContext: Boolean = true,
        classifier: (String) -> Boolean = ::isExplicitVersionContext,
    ): String? {
        val text = value.trim()
        if (text.isBlank()) return null
        if (allowWholeContext && classifier(text)) return text

        val separatorMatches = VERSION_SEPARATOR.findAll(text).toList()
        if (separatorMatches.isEmpty()) return null

        val segments = mutableListOf<String>()
        var start = 0
        separatorMatches.forEach { separator ->
            segments += text.substring(start, separator.range.first).trim()
            start = separator.range.last + 1
        }
        segments += text.substring(start).trim()

        if (segments.size < 2 || !classifier(segments.last())) return null

        var firstVersionSegment = segments.lastIndex
        while (
            firstVersionSegment - 1 >= 0 &&
            classifier(segments[firstVersionSegment - 1])
        ) {
            firstVersionSegment--
        }

        return segments.subList(firstVersionSegment, segments.size)
            .joinToString(": ")
    }

    private fun isExplicitTitleVersionContext(value: String): Boolean {
        val text = value.trim()
        if (isExplicitVersionContext(text)) return true
        return NAMED_REMIX_TITLE_CONTEXT.matches(text)
    }

    private fun isExplicitVersionContext(value: String): Boolean {
        val text = value.trim()
        if (text.isBlank()) return false

        if (JAPANESE_VERSION_CONTEXT.matches(text)) return true
        if (LIVE_LOCATION_CONTEXT.matches(text)) return true
        if (!ENGLISH_VERSION_TOKEN.containsMatchIn(text)) return false

        val tokens = CONTEXT_TOKEN.findAll(text.lowercase(Locale.ROOT))
            .map { it.value }
            .toList()
        if (tokens.isEmpty()) return false

        val anniversaryContext = "anniversary" in tokens
        return tokens.all { token ->
            token in ALLOWED_ENGLISH_CONTEXT_WORDS ||
                token.toIntOrNull()?.let { it in 1900..2199 } == true ||
                (anniversaryContext && ENGLISH_ORDINAL_TOKEN.matches(token))
        }
    }
}
