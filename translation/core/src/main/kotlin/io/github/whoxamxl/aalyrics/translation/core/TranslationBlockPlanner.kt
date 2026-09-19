package io.github.whoxamxl.aalyrics.translation.core

import io.github.whoxamxl.aalyrics.core.model.LyricsDocument
import io.github.whoxamxl.aalyrics.core.model.TimedLyricLine
import io.github.whoxamxl.aalyrics.translation.api.TranslationLanguages

data class TranslationBlockPolicy(
    val preferredCoreLines: Int = 3,
    val maximumCoreCharacters: Int = 240,
    val contextHaloLines: Int = 1,
    val largeTimestampGapMs: Long = 10_000L,
) {
    init {
        require(preferredCoreLines > 0) { "Preferred Translation block size must be positive" }
        require(maximumCoreCharacters > 0) { "Maximum Translation block characters must be positive" }
        require(contextHaloLines >= 0) { "Context Halo size must not be negative" }
        require(largeTimestampGapMs >= 0L) { "Large timestamp gap must not be negative" }
    }
}

/** Plans deterministic Core ownership before any provider work starts. */
class TranslationBlockPlanner(
    private val policy: TranslationBlockPolicy = TranslationBlockPolicy(),
) {
    fun plan(
        lyrics: LyricsDocument,
        profile: LanguageProfile,
        targetLanguage: String,
    ): TranslationPlan {
        require(profile.lines.size == lyrics.lines.size) {
            "Language profile must match the canonical lyric document"
        }
        val target = TranslationLanguages.normalizeLanguageTag(targetLanguage)
            ?: targetLanguage
        val linePlans = profile.lines.map { line ->
            val shouldTranslate = when (line.role) {
                ProfiledLineRole.PRIMARY -> line.languageTag != target
                ProfiledLineRole.SECONDARY ->
                    profile.secondaryActivation == SecondaryActivation.ACTIVE &&
                        line.languageTag != target
                ProfiledLineRole.TARGET,
                ProfiledLineRole.UNCERTAIN -> false
            }
            TranslationLinePlan(
                canonicalLineIndex = line.index,
                sourceLanguage = line.languageTag,
                disposition = if (shouldTranslate) {
                    TranslationLineDisposition.TRANSLATE
                } else {
                    TranslationLineDisposition.PRESERVE
                },
            )
        }

        val hardGroups = mutableListOf<List<Int>>()
        var currentGroup = mutableListOf<Int>()

        fun flushGroup() {
            if (currentGroup.isNotEmpty()) hardGroups += currentGroup.toList()
            currentGroup = mutableListOf()
        }

        linePlans.forEachIndexed { index, linePlan ->
            if (linePlan.disposition != TranslationLineDisposition.TRANSLATE) {
                flushGroup()
                return@forEachIndexed
            }

            val previousIndex = currentGroup.lastOrNull()
            val crossesHardBoundary = previousIndex != null && (
                linePlans[previousIndex].sourceLanguage != linePlan.sourceLanguage ||
                    hasLargeTimestampGap(lyrics, previousIndex, index)
                )
            if (crossesHardBoundary) flushGroup()
            currentGroup += index
        }
        flushGroup()

        val blocks = hardGroups.flatMap { group ->
            splitSoft(group, lyrics).map { core ->
                val firstPosition = group.indexOf(core.first())
                val lastPosition = group.indexOf(core.last())
                val context = buildList {
                    for (offset in policy.contextHaloLines downTo 1) {
                        group.getOrNull(firstPosition - offset)?.let(::add)
                    }
                    for (offset in 1..policy.contextHaloLines) {
                        group.getOrNull(lastPosition + offset)?.let(::add)
                    }
                }
                TranslationBlock(
                    sourceLanguage = requireNotNull(linePlans[core.first()].sourceLanguage),
                    coreLineIndices = core,
                    contextLineIndices = context,
                )
            }
        }

        return TranslationPlan(
            targetLanguage = target,
            lines = linePlans,
            blocks = blocks,
        )
    }

    private fun splitSoft(
        group: List<Int>,
        lyrics: LyricsDocument,
    ): List<List<Int>> {
        val chunks = mutableListOf<List<Int>>()
        var current = mutableListOf<Int>()
        var characters = 0

        fun flush() {
            if (current.isNotEmpty()) chunks += current.toList()
            current = mutableListOf()
            characters = 0
        }

        group.forEach { index ->
            val lineCharacters = lyrics.lines[index].text.length
            val exceedsSize = current.size >= policy.preferredCoreLines
            val exceedsCharacters = current.isNotEmpty() &&
                characters + lineCharacters > policy.maximumCoreCharacters
            if (exceedsSize || exceedsCharacters) flush()
            current += index
            characters += lineCharacters
        }
        flush()
        return chunks
    }

    private fun hasLargeTimestampGap(
        lyrics: LyricsDocument,
        previousIndex: Int,
        currentIndex: Int,
    ): Boolean {
        val previous = lyrics.lines[previousIndex] as? TimedLyricLine ?: return false
        val current = lyrics.lines[currentIndex] as? TimedLyricLine ?: return false
        val previousBoundary = previous.endMs ?: previous.startMs
        return current.startMs - previousBoundary > policy.largeTimestampGapMs
    }
}
