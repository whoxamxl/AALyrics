package io.github.whoxamxl.aalyrics.translation.core

import io.github.whoxamxl.aalyrics.core.model.LyricsDocument
import io.github.whoxamxl.aalyrics.translation.api.IdentifiedLanguage
import io.github.whoxamxl.aalyrics.translation.api.LanguageIdentifier
import io.github.whoxamxl.aalyrics.translation.api.TranslationLanguages
import kotlinx.coroutines.CancellationException
import java.util.Locale

data class LanguageProfilerPolicy(
    val minimumLatinLineCharacters: Int = 4,
    val minimumDistinctiveScriptCharacters: Int = 2,
    val minimumLineConfidence: Float = 0.45f,
    val aggregateEvidenceWeight: Float = 0.20f,
    val minimumPrimaryEvidence: Float = 4f,
    val minimumSecondaryEvidence: Float = 0.25f,
    val minimumActiveSecondaryLines: Int = 2,
    val minimumActiveSecondaryCharacters: Int = 12,
    val minimumActiveSecondaryCharacterShare: Float = 0.15f,
    val minimumActiveSecondaryContiguousRun: Int = 2,
    val minimumActiveSecondaryRegions: Int = 2,
) {
    init {
        require(minimumLatinLineCharacters > 0)
        require(minimumDistinctiveScriptCharacters > 0)
        require(minimumLineConfidence in 0f..1f)
        require(aggregateEvidenceWeight in 0f..1f)
        require(minimumPrimaryEvidence > 0f)
        require(minimumSecondaryEvidence > 0f)
        require(minimumActiveSecondaryLines > 0)
        require(minimumActiveSecondaryCharacters > 0)
        require(minimumActiveSecondaryCharacterShare in 0f..1f)
        require(minimumActiveSecondaryContiguousRun > 0)
        require(minimumActiveSecondaryRegions > 0)
    }
}

/** Complete-document profiler with conservative line routing for lyric text. */
class LanguageProfilingException(message: String) : IllegalStateException(message)

class LanguageProfiler(
    private val identifier: LanguageIdentifier,
    private val policy: LanguageProfilerPolicy = LanguageProfilerPolicy(),
) {
    suspend fun profile(
        lyrics: LyricsDocument,
        targetLanguage: String,
    ): LanguageProfile {
        val target = TranslationLanguages.normalizeLanguageTag(targetLanguage)
            ?: targetLanguage.lowercase(Locale.US)
        val lineEvidence = lyrics.lines.mapIndexed { index, line ->
            identifyLine(index, line.text)
        }
        val substantive = lineEvidence.filter { it.substantiveCharacters > 0 }
        val meaningful = substantive.filterNot { it.borrowedPhrase }
        val evidenceByLanguage = linkedMapOf<String, Float>()
        var identifierFailed = lineEvidence.any { it.identifierFailed }

        substantive.forEach { evidence ->
            val language = evidence.languageTag ?: return@forEach
            evidenceByLanguage[language] = evidenceByLanguage.getOrDefault(language, 0f) +
                evidence.substantiveCharacters * evidence.confidence *
                if (evidence.borrowedPhrase) BORROWED_PHRASE_WEIGHT else 1f
        }

        val completeText = meaningful.joinToString("\n") { lyrics.lines[it.index].text.trim() }
        if (completeText.isNotBlank()) {
            val totalCharacters = meaningful.sumOf { it.substantiveCharacters }
            val aggregateAttempt = identifySafely(completeText)
            identifierFailed = identifierFailed || aggregateAttempt.failed
            aggregateAttempt.candidates.firstOrNull()?.let { aggregate ->
                evidenceByLanguage[aggregate.languageTag] =
                    evidenceByLanguage.getOrDefault(aggregate.languageTag, 0f) +
                    totalCharacters * policy.aggregateEvidenceWeight * aggregate.confidence
            }
        }

        if (meaningful.isNotEmpty() && evidenceByLanguage.isEmpty() && identifierFailed) {
            throw LanguageProfilingException("Language identification failed for meaningful lyric content")
        }

        val primary = evidenceByLanguage.maxByOrNull { it.value }
            ?.takeIf { it.value >= policy.minimumPrimaryEvidence }
            ?.key
        val secondary = evidenceByLanguage
            .filterKeys { it != primary }
            .maxByOrNull { it.value }
            ?.takeIf { it.value >= policy.minimumSecondaryEvidence }
            ?.key
        val activation = secondary?.let {
            secondaryActivation(
                candidate = it,
                evidence = substantive,
                totalCharacters = meaningful.sumOf { line -> line.substantiveCharacters },
                totalLineCount = lyrics.lines.size,
            )
        } ?: SecondaryActivation.NONE

        return LanguageProfile(
            primary = primary,
            secondaryCandidate = secondary,
            secondaryActivation = activation,
            lines = lineEvidence.map { evidence ->
                val role = when {
                    evidence.languageTag == null -> ProfiledLineRole.UNCERTAIN
                    evidence.languageTag == target -> ProfiledLineRole.TARGET
                    evidence.languageTag == primary -> ProfiledLineRole.PRIMARY
                    evidence.languageTag == secondary -> ProfiledLineRole.SECONDARY
                    else -> ProfiledLineRole.UNCERTAIN
                }
                ProfiledLyricLine(
                    index = evidence.index,
                    languageTag = evidence.languageTag,
                    confidence = evidence.confidence,
                    role = role,
                )
            },
        )
    }

    private suspend fun identifyLine(index: Int, text: String): LineEvidence {
        val substantiveCharacters = text.codePoints()
            .filter { Character.isLetterOrDigit(it) }
            .count()
            .toInt()
        if (substantiveCharacters == 0) {
            return LineEvidence(
                index = index,
                languageTag = null,
                confidence = 0f,
                substantiveCharacters = 0,
                borrowedPhrase = false,
                identifierFailed = false,
            )
        }

        val borrowed = normalizePhrase(text) in BORROWED_PHRASES
        if (borrowed) {
            val attempt = identifySafely(text)
            val detected = attempt.candidates.firstOrNull()
            return LineEvidence(
                index = index,
                languageTag = detected?.languageTag,
                confidence = detected?.confidence ?: 0f,
                substantiveCharacters = substantiveCharacters,
                borrowedPhrase = true,
                identifierFailed = attempt.failed,
            )
        }

        val scripts = scriptCounts(text)
        val attempt = identifySafely(text)
        val detected = attempt.candidates.firstOrNull()
        val scriptEvidence = when {
            scripts.kana > 0 -> IdentifiedLanguage("ja", DISTINCTIVE_SCRIPT_CONFIDENCE)
            scripts.hangul >= policy.minimumDistinctiveScriptCharacters &&
                scripts.hangul.toFloat() / substantiveCharacters >= DISTINCTIVE_SCRIPT_SHARE ->
                IdentifiedLanguage("ko", DISTINCTIVE_SCRIPT_CONFIDENCE)
            scripts.han >= policy.minimumDistinctiveScriptCharacters ->
                detected?.takeIf { it.languageTag == "ja" || it.languageTag == "zh" }
                    ?: IdentifiedLanguage("zh", HAN_ONLY_CONFIDENCE)
            else -> null
        }
        val selected = scriptEvidence ?: detected
        val isDistinctive = scriptEvidence != null
        val usable = selected?.takeIf {
            it.confidence >= policy.minimumLineConfidence &&
                (isDistinctive || substantiveCharacters >= policy.minimumLatinLineCharacters)
        }

        return LineEvidence(
            index = index,
            languageTag = usable?.languageTag,
            confidence = usable?.confidence ?: 0f,
            substantiveCharacters = substantiveCharacters,
            borrowedPhrase = false,
            identifierFailed = attempt.failed,
        )
    }

    private fun secondaryActivation(
        candidate: String,
        evidence: List<LineEvidence>,
        totalCharacters: Int,
        totalLineCount: Int,
    ): SecondaryActivation {
        val candidateLines = evidence.filter { it.languageTag == candidate && !it.borrowedPhrase }
        if (candidateLines.isEmpty()) return SecondaryActivation.INCIDENTAL

        val candidateCharacters = candidateLines.sumOf { it.substantiveCharacters }
        val characterShare = if (totalCharacters == 0) {
            0f
        } else {
            candidateCharacters.toFloat() / totalCharacters
        }
        val indices = candidateLines.map { it.index }.sorted()
        var longestRun = 0
        var currentRun = 0
        var previousIndex: Int? = null
        indices.forEach { index ->
            currentRun = if (previousIndex != null && index == previousIndex + 1) currentRun + 1 else 1
            longestRun = maxOf(longestRun, currentRun)
            previousIndex = index
        }
        val regionDivisor = maxOf(totalLineCount, 1)
        val regions = indices.map { index -> (index * SONG_REGION_COUNT) / regionDivisor }
            .distinct()
            .size

        val active = candidateLines.size >= policy.minimumActiveSecondaryLines &&
            candidateCharacters >= policy.minimumActiveSecondaryCharacters &&
            characterShare >= policy.minimumActiveSecondaryCharacterShare &&
            (
                longestRun >= policy.minimumActiveSecondaryContiguousRun ||
                    regions >= policy.minimumActiveSecondaryRegions
                )
        return if (active) SecondaryActivation.ACTIVE else SecondaryActivation.INCIDENTAL
    }

    private suspend fun identifySafely(text: String): IdentificationAttempt = try {
        IdentificationAttempt(
            candidates = identifier.identifyPossibleLanguages(text)
                .mapNotNull { it.normalized() }
                .sortedByDescending { it.confidence },
            failed = false,
        )
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (_: Exception) {
        IdentificationAttempt(candidates = emptyList(), failed = true)
    }

    private fun IdentifiedLanguage.normalized(): IdentifiedLanguage? {
        val normalized = TranslationLanguages.normalizeLanguageTag(languageTag) ?: return null
        if (normalized == UNDETERMINED_LANGUAGE) return null
        return copy(languageTag = normalized)
    }

    private fun scriptCounts(text: String): ScriptCounts {
        var hangul = 0
        var kana = 0
        var han = 0
        text.codePoints().forEach { codePoint ->
            when (Character.UnicodeScript.of(codePoint)) {
                Character.UnicodeScript.HANGUL -> hangul++
                Character.UnicodeScript.HIRAGANA,
                Character.UnicodeScript.KATAKANA -> kana++
                Character.UnicodeScript.HAN -> han++
                else -> Unit
            }
        }
        return ScriptCounts(hangul = hangul, kana = kana, han = han)
    }

    private fun normalizePhrase(text: String): String = text
        .lowercase(Locale.US)
        .filter { it.isLetterOrDigit() }

    private data class LineEvidence(
        val index: Int,
        val languageTag: String?,
        val confidence: Float,
        val substantiveCharacters: Int,
        val borrowedPhrase: Boolean,
        val identifierFailed: Boolean,
    )

    private data class IdentificationAttempt(
        val candidates: List<IdentifiedLanguage>,
        val failed: Boolean,
    )

    private data class ScriptCounts(
        val hangul: Int,
        val kana: Int,
        val han: Int,
    )

    private companion object {
        const val DISTINCTIVE_SCRIPT_CONFIDENCE = 0.98f
        const val DISTINCTIVE_SCRIPT_SHARE = 0.35f
        const val HAN_ONLY_CONFIDENCE = 0.75f
        const val BORROWED_PHRASE_WEIGHT = 0.10f
        const val SONG_REGION_COUNT = 3
        const val UNDETERMINED_LANGUAGE = "und"
        val BORROWED_PHRASES = setOf("oh", "ooh", "yeah", "baby", "hey", "la", "na")
    }
}
