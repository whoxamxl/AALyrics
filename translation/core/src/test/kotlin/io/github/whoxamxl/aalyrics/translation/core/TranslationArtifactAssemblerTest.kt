package io.github.whoxamxl.aalyrics.translation.core

import io.github.whoxamxl.aalyrics.core.model.LyricsDocument
import io.github.whoxamxl.aalyrics.core.model.PlainLyricLine
import io.github.whoxamxl.aalyrics.translation.api.TranslationProvider
import io.github.whoxamxl.aalyrics.translation.api.TranslationProviderId
import io.github.whoxamxl.aalyrics.translation.api.TranslationRoute
import io.github.whoxamxl.aalyrics.translation.api.TranslationSession
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TranslationArtifactAssemblerTest {
    @Test
    fun `valid aligned blocks publish Core results and ignore Halo output`() = runTest {
        val session = RecordingSession { input, call -> alignedOutput(input, "call$call") }
        val canonical = canonical(3)
        val artifact = TranslationArtifactAssembler().assemble(
            request = request(canonical),
            canonical = canonical,
            profile = profile(3),
            plan = plan(
                lineCount = 3,
                blocks = listOf(
                    TranslationBlock("en", coreLineIndices = listOf(0, 1), contextLineIndices = listOf(2)),
                    TranslationBlock("en", coreLineIndices = listOf(2), contextLineIndices = listOf(1)),
                ),
            ),
            provider = provider("aligned", session),
        )

        assertNotNull(artifact)
        assertEquals("aligned", artifact.providerId.value)
        assertEquals(listOf("call1-0", "call1-1", "call2-2"), artifact.lines.map { it.text })
        assertTrue(artifact.lines.all { it.translated })
    }

    @Test
    fun `invalid contextual alignment degrades to smaller aligned blocks`() = runTest {
        val markerCounts = mutableListOf<Int>()
        val session = RecordingSession { input, call ->
            val count = markers(input).size
            markerCounts += count
            if (call == 1) "invalid structure" else alignedOutput(input, "split")
        }

        val canonical = canonical(3)
        val artifact = TranslationArtifactAssembler().assemble(
            request = request(canonical),
            canonical = canonical,
            profile = profile(3),
            plan = plan(
                lineCount = 3,
                blocks = listOf(TranslationBlock("en", listOf(0, 1, 2), emptyList())),
            ),
            provider = provider("split", session),
        )

        assertNotNull(artifact)
        assertEquals(listOf(3, 2, 1), markerCounts)
        assertEquals(listOf("split-0", "split-1", "split-2"), artifact.lines.map { it.text })
    }

    @Test
    fun `invalid smaller blocks use final per-line fallback`() = runTest {
        val markerCalls = mutableListOf<Int>()
        val rawCalls = mutableListOf<String>()
        val session = RecordingSession { input, _ ->
            if (markers(input).isNotEmpty()) {
                markerCalls += markers(input).size
                "markers removed"
            } else {
                rawCalls += input
                "translated:$input"
            }
        }

        val canonical = canonical(2)
        val artifact = TranslationArtifactAssembler().assemble(
            request = request(canonical),
            canonical = canonical,
            profile = profile(2),
            plan = plan(
                lineCount = 2,
                blocks = listOf(TranslationBlock("en", listOf(0, 1), emptyList())),
            ),
            provider = provider("per-line", session),
        )

        assertNotNull(artifact)
        assertEquals(listOf(2, 1, 1), markerCalls)
        assertEquals(listOf("Original 0", "Original 1"), rawCalls)
        assertEquals(listOf("translated:Original 0", "translated:Original 1"), artifact.lines.map { it.text })
    }

    @Test
    fun `unrecoverable line keeps canonical text without shifting neighbors`() = runTest {
        val session = RecordingSession { input, _ ->
            when {
                markers(input).isNotEmpty() -> "invalid"
                input == "Original 1" -> error("synthetic failure")
                else -> "translated:$input"
            }
        }

        val canonical = canonical(2)
        val artifact = TranslationArtifactAssembler().assemble(
            request = request(canonical),
            canonical = canonical,
            profile = profile(2),
            plan = plan(
                lineCount = 2,
                blocks = listOf(TranslationBlock("en", listOf(0, 1), emptyList())),
            ),
            provider = provider("fallback", session),
        )

        assertNotNull(artifact)
        assertEquals("translated:Original 0", artifact.lines[0].text)
        assertTrue(artifact.lines[0].translated)
        assertEquals("Original 1", artifact.lines[1].text)
        assertFalse(artifact.lines[1].translated)
    }

    @Test
    fun `provider with no acceptable translated line is rejected`() = runTest {
        val session = RecordingSession { _, _ -> error("synthetic failure") }

        val canonical = canonical(1)
        val artifact = TranslationArtifactAssembler().assemble(
            request = request(canonical),
            canonical = canonical,
            profile = profile(1),
            plan = plan(
                lineCount = 1,
                blocks = listOf(TranslationBlock("en", listOf(0), emptyList())),
            ),
            provider = provider("failed", session),
        )

        assertNull(artifact)
        assertTrue(session.closed)
    }

    private fun canonical(lineCount: Int): CanonicalLyrics = CanonicalLyrics.create(
        ownerId = "lookup-1",
        document = LyricsDocument(List(lineCount) { index -> PlainLyricLine("Original $index") }),
    )

    private fun request(canonical: CanonicalLyrics): TranslationRequestIdentity = TranslationRequestIdentity(
        id = TranslationRequestId(1),
        canonicalLyrics = canonical.identity,
        targetLanguage = "ja",
    )

    private fun profile(lineCount: Int): LanguageProfile = LanguageProfile(
        primary = "en",
        secondaryCandidate = null,
        secondaryActivation = SecondaryActivation.NONE,
        lines = List(lineCount) { index ->
            ProfiledLyricLine(index, "en", 0.99f, ProfiledLineRole.PRIMARY)
        },
    )

    private fun plan(
        lineCount: Int,
        blocks: List<TranslationBlock>,
    ): TranslationPlan = TranslationPlan(
        targetLanguage = "ja",
        lines = List(lineCount) { index ->
            TranslationLinePlan(index, "en", TranslationLineDisposition.TRANSLATE)
        },
        blocks = blocks,
    )

    private fun provider(
        id: String,
        session: TranslationSession,
    ): TranslationProvider = object : TranslationProvider {
        override val id = TranslationProviderId(id)

        override suspend fun openSession(route: TranslationRoute): TranslationSession = session
    }

    private class RecordingSession(
        private val response: (String, Int) -> String,
    ) : TranslationSession {
        var calls = 0
        var closed = false

        override suspend fun translate(text: String): String {
            calls++
            return response(text, calls)
        }

        override fun close() {
            closed = true
        }
    }

    private fun alignedOutput(input: String, prefix: String): String =
        markers(input).joinToString("\n") { (marker, index) -> "$marker $prefix-$index" }

    private fun markers(input: String): List<Pair<String, Int>> =
        Regex("\\[\\[AALYRICS_LINE_(\\d+)]]")
            .findAll(input)
            .map { match -> match.value to match.groupValues[1].toInt() }
            .toList()
}
