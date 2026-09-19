package io.github.whoxamxl.aalyrics.translation.core

import io.github.whoxamxl.aalyrics.translation.api.TranslationProvider
import io.github.whoxamxl.aalyrics.translation.api.TranslationRoute
import io.github.whoxamxl.aalyrics.translation.api.TranslationSession
import kotlinx.coroutines.CancellationException

/** Executes one complete plan with one provider and never exposes partial work. */
class TranslationArtifactAssembler {
    suspend fun assemble(
        request: TranslationRequestIdentity,
        canonical: CanonicalLyrics,
        profile: LanguageProfile,
        plan: TranslationPlan,
        provider: TranslationProvider,
    ): TranslationArtifact? {
        require(request.canonicalLyrics == canonical.identity) {
            "Translation request must match the exact canonical lyrics identity"
        }
        require(request.targetLanguage == plan.targetLanguage) {
            "Translation request target must match the Translation plan"
        }
        require(plan.lines.size == canonical.document.lines.size) {
            "Translation plan must match the canonical lyric document"
        }
        val sessions = linkedMapOf<String, TranslationSession>()
        try {
            for (sourceLanguage in plan.blocks.map { it.sourceLanguage }.distinct()) {
                val session = try {
                    provider.openSession(
                        TranslationRoute(
                            sourceLanguage = sourceLanguage,
                            targetLanguage = plan.targetLanguage,
                        ),
                    )
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (_: Exception) {
                    null
                } ?: return null
                sessions[sourceLanguage] = session
            }

            val output = canonical.document.lines.map { it.text }.toMutableList()
            val translated = BooleanArray(output.size)
            var successfulTranslations = 0

            plan.blocks.forEach { block ->
                val session = requireNotNull(sessions[block.sourceLanguage])
                val blockResult = translateBlock(
                    session = session,
                    canonical = canonical,
                    block = block,
                )
                block.coreLineIndices.forEach { index ->
                    blockResult[index]?.let { result ->
                        output[index] = result.text
                        translated[index] = result.accepted
                        if (result.accepted) successfulTranslations++
                    }
                }
            }

            if (plan.blocks.isNotEmpty() && successfulTranslations == 0) return null

            return TranslationArtifact(
                request = request,
                providerId = provider.id,
                profile = profile,
                lines = output.mapIndexed { index, text ->
                    TranslationArtifactLine(
                        canonicalLineIndex = index,
                        text = text,
                        sourceLanguage = plan.lines[index].sourceLanguage,
                        translated = translated[index],
                    )
                },
            )
        } finally {
            sessions.values.forEach { session ->
                runCatching { session.close() }
            }
        }
    }

    private suspend fun translateBlock(
        session: TranslationSession,
        canonical: CanonicalLyrics,
        block: TranslationBlock,
    ): Map<Int, TranslationResult> {
        val contextual = translateAligned(
            session = session,
            canonical = canonical,
            indices = block.orderedLineIndices,
        )
        if (contextual != null) {
            return block.coreLineIndices.associateWith { index ->
                TranslationResult(requireNotNull(contextual[index]), accepted = true)
            }
        }

        val smallerChunks = when {
            block.coreLineIndices.size > 1 -> {
                val splitSize = (block.coreLineIndices.size + 1) / 2
                block.coreLineIndices.chunked(splitSize)
            }
            block.orderedLineIndices != block.coreLineIndices -> listOf(block.coreLineIndices)
            else -> emptyList()
        }
        val result = linkedMapOf<Int, TranslationResult>()

        smallerChunks.forEach { chunk ->
            val aligned = translateAligned(session, canonical, chunk)
            if (aligned != null) {
                chunk.forEach { index ->
                    result[index] = TranslationResult(
                        text = requireNotNull(aligned[index]),
                        accepted = true,
                    )
                }
            } else {
                result += translatePerLine(session, canonical, chunk)
            }
        }

        val unresolved = block.coreLineIndices.filterNot(result::containsKey)
        if (unresolved.isNotEmpty()) {
            result += translatePerLine(session, canonical, unresolved)
        }
        return result
    }

    private suspend fun translateAligned(
        session: TranslationSession,
        canonical: CanonicalLyrics,
        indices: List<Int>,
    ): Map<Int, String>? {
        val encoded = LineMarkerCodec.encode(
            indices.associateWith { canonical.document.lines[it].text.trim() },
        )
        val translated = try {
            session.translate(encoded)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            return null
        }
        return LineMarkerCodec.decode(translated, indices)
    }

    private suspend fun translatePerLine(
        session: TranslationSession,
        canonical: CanonicalLyrics,
        indices: List<Int>,
    ): Map<Int, TranslationResult> = indices.associateWith { index ->
        val original = canonical.document.lines[index].text
        val translated = try {
            session.translate(original.trim()).trim()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            ""
        }
        if (translated.isBlank()) {
            TranslationResult(original, accepted = false)
        } else {
            TranslationResult(translated, accepted = true)
        }
    }

    private data class TranslationResult(
        val text: String,
        val accepted: Boolean,
    )
}

internal object LineMarkerCodec {
    private val markerPattern = Regex("\\[\\[AALYRICS_LINE_(\\d+)]]")

    fun encode(lines: Map<Int, String>): String = lines.entries.joinToString("\n") { (index, text) ->
        "${marker(index)} $text"
    }

    fun decode(
        translated: String,
        expectedIndices: List<Int>,
    ): Map<Int, String>? {
        val matches = markerPattern.findAll(translated).toList()
        val actualIndices = matches.map { match -> match.groupValues[1].toInt() }
        if (actualIndices != expectedIndices) return null
        if (matches.firstOrNull()?.range?.first?.let { translated.substring(0, it).isNotBlank() } == true) {
            return null
        }

        val result = linkedMapOf<Int, String>()
        matches.forEachIndexed { position, match ->
            val start = match.range.last + 1
            val end = matches.getOrNull(position + 1)?.range?.first ?: translated.length
            val text = translated.substring(start, end).trim()
            if (text.isBlank()) return null
            result[actualIndices[position]] = text
        }
        return result
    }

    private fun marker(index: Int): String = "[[AALYRICS_LINE_$index]]"
}
