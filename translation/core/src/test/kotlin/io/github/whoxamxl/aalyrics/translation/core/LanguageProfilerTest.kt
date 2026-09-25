package io.github.whoxamxl.aalyrics.translation.core

import io.github.whoxamxl.aalyrics.core.model.LyricsDocument
import io.github.whoxamxl.aalyrics.core.model.PlainLyricLine
import io.github.whoxamxl.aalyrics.translation.api.IdentifiedLanguage
import io.github.whoxamxl.aalyrics.translation.api.LanguageIdentifier
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class LanguageProfilerTest {
    @Test
    fun `Primary uses the complete document instead of the first five lines`() = runTest {
        val lyrics = document(
            List(5) { index -> "Early line $index" } +
                List(8) { index -> "かなでる夜のことばを明日まで届ける$index" },
        )
        val profiler = LanguageProfiler(identifier { text ->
            when {
                '\n' in text -> candidates("ja" to 0.99f, "en" to 0.70f)
                text.any { it in 'a'..'z' || it in 'A'..'Z' } -> candidates("en" to 0.98f)
                else -> candidates("ja" to 0.98f)
            }
        })

        val profile = profiler.profile(lyrics, targetLanguage = "fr")

        assertEquals("ja", profile.primary)
        assertEquals("en", profile.secondaryCandidate)
        assertEquals(8, profile.lines.count { it.role == ProfiledLineRole.PRIMARY })
    }

    @Test
    fun `single kana character does not override strong Latin identifier evidence`() = runTest {
        val lyrics = document(
            listOf(
                "Synthetic English line あ",
                "Another synthetic English line",
            ),
        )
        val profiler = LanguageProfiler(
            identifier { candidates("en" to 0.99f) },
        )

        val profile = profiler.profile(lyrics, targetLanguage = "ja")

        assertEquals("en", profile.primary)
        assertEquals(ProfiledLineRole.PRIMARY, profile.lines[0].role)
        assertEquals("en", profile.lines[0].languageTag)
    }

    @Test
    fun `meaningful bilingual passage activates one Secondary language`() = runTest {
        val lyrics = document(
            listOf(
                "We follow the morning light",
                "The city opens every door",
                "Our voices travel through the rain",
                "A final signal crosses home",
                "The quiet avenue is bright",
                "우리는 함께 노래하며 걷는다",
                "새로운 아침을 향해서 간다",
                "오늘의 목소리를 오래 기억한다",
            ),
        )
        val profiler = LanguageProfiler(identifier { text ->
            if ('\n' in text || text.any { it in 'a'..'z' || it in 'A'..'Z' }) {
                candidates("en" to 0.97f)
            } else {
                candidates("ko" to 0.97f)
            }
        })

        val profile = profiler.profile(lyrics, targetLanguage = "ja")

        assertEquals("en", profile.primary)
        assertEquals("ko", profile.secondaryCandidate)
        assertEquals(SecondaryActivation.ACTIVE, profile.secondaryActivation)
        assertEquals(3, profile.lines.count { it.role == ProfiledLineRole.SECONDARY })
    }

    @Test
    fun `two high-confidence false Secondary lines remain INCIDENTAL`() = runTest {
        val falseSecondaryLines = setOf(
            "canto breve na rua",
            "volto cedo para casa",
        )
        val lyrics = document(
            listOf(
                "a noite segue devagar",
                "o vento passa pela janela",
                "canto breve na rua",
                "volto cedo para casa",
                "amanha nasce outro dia",
                "a cidade dorme tranquila",
            ),
        )
        val profiler = LanguageProfiler(identifier { text ->
            when {
                '\n' in text -> candidates("pt" to 0.99f)
                text in falseSecondaryLines -> candidates("ar" to 0.93f)
                else -> candidates("pt" to 0.98f)
            }
        })

        val profile = profiler.profile(lyrics, targetLanguage = "ja")

        assertEquals("pt", profile.primary)
        assertEquals("ar", profile.secondaryCandidate)
        assertEquals(SecondaryActivation.INCIDENTAL, profile.secondaryActivation)
    }

    @Test
    fun `low-confidence Secondary passage remains INCIDENTAL even with three lines`() = runTest {
        val weakSecondaryLines = setOf(
            "first weak secondary line",
            "second weak secondary line",
            "third weak secondary line",
        )
        val lyrics = document(
            listOf(
                "main language line one",
                "main language line two",
                "main language line three",
                "first weak secondary line",
                "second weak secondary line",
                "third weak secondary line",
                "main language line four",
            ),
        )
        val profiler = LanguageProfiler(identifier { text ->
            when {
                '\n' in text -> candidates("en" to 0.99f)
                text in weakSecondaryLines -> candidates("es" to 0.60f)
                else -> candidates("en" to 0.98f)
            }
        })

        val profile = profiler.profile(lyrics, targetLanguage = "ja")

        assertEquals("en", profile.primary)
        assertEquals("es", profile.secondaryCandidate)
        assertEquals(SecondaryActivation.INCIDENTAL, profile.secondaryActivation)
    }

    @Test
    fun `borrowed foreign phrases remain INCIDENTAL`() = runTest {
        val lyrics = document(
            listOf(
                "静かな夜に歌を届ける",
                "Oh",
                "遠い空まで声が響く",
                "Yeah",
                "明日の光を探して歩く",
            ),
        )
        val profiler = LanguageProfiler(identifier { text ->
            when {
                text.equals("Oh", ignoreCase = true) || text.equals("Yeah", ignoreCase = true) ->
                    candidates("en" to 0.95f)
                else -> candidates("ja" to 0.98f)
            }
        })

        val profile = profiler.profile(lyrics, targetLanguage = "ko")

        assertEquals("ja", profile.primary)
        assertEquals("en", profile.secondaryCandidate)
        assertEquals(SecondaryActivation.INCIDENTAL, profile.secondaryActivation)
        assertEquals(
            listOf(ProfiledLineRole.SECONDARY, ProfiledLineRole.SECONDARY),
            listOf(profile.lines[1].role, profile.lines[3].role),
        )
    }

    @Test
    fun `uncertain lines remain explicitly uncertain`() = runTest {
        val lyrics = document(
            listOf(
                "A synthetic English lyric line",
                "Another synthetic English line",
                "1234 ???",
                "The final synthetic English line",
            ),
        )
        val profiler = LanguageProfiler(identifier { text ->
            if (text.any(Char::isLetter)) candidates("en" to 0.98f) else emptyList()
        })

        val profile = profiler.profile(lyrics, targetLanguage = "ja")

        assertEquals(ProfiledLineRole.UNCERTAIN, profile.lines[2].role)
        assertEquals(null, profile.lines[2].languageTag)
    }

    @Test
    fun `identifier failure is distinct from undetermined language`() = runTest {
        val lyrics = document(
            listOf(
                "Synthetic Latin lyric line",
                "Another synthetic Latin lyric line",
            ),
        )
        val profiler = LanguageProfiler(
            LanguageIdentifier { error("synthetic language-id failure") },
        )

        assertFailsWith<LanguageProfilingException> {
            profiler.profile(lyrics, targetLanguage = "ja")
        }
    }

    @Test
    fun `borrowed-only evidence does not hide meaningful identifier failure`() = runTest {
        val lyrics = document(
            listOf(
                "Synthetic Latin lyric line",
                "Yeah",
            ),
        )
        val profiler = LanguageProfiler(
            LanguageIdentifier { text ->
                if (text.equals("Yeah", ignoreCase = true)) {
                    candidates("en" to 0.99f)
                } else {
                    error("synthetic language-id failure")
                }
            },
        )

        assertFailsWith<LanguageProfilingException> {
            profiler.profile(lyrics, targetLanguage = "ja")
        }
    }

    @Test
    fun `target-language document produces no translatable blocks`() = runTest {
        val lyrics = document(
            listOf(
                "A synthetic English lyric line",
                "Another synthetic English line",
            ),
        )
        val profile = LanguageProfiler(identifier { candidates("en" to 0.99f) })
            .profile(lyrics, targetLanguage = "en")

        val plan = TranslationBlockPlanner().plan(lyrics, profile, targetLanguage = "en")

        assertTrue(plan.blocks.isEmpty())
        assertTrue(plan.lines.all { it.disposition == TranslationLineDisposition.PRESERVE })
    }

    private fun document(lines: List<String>): LyricsDocument = LyricsDocument(
        lines = lines.map(::PlainLyricLine),
    )

    private fun identifier(
        block: suspend (String) -> List<IdentifiedLanguage>,
    ): LanguageIdentifier = LanguageIdentifier(block)

    private fun candidates(vararg values: Pair<String, Float>): List<IdentifiedLanguage> =
        values.map { (language, confidence) -> IdentifiedLanguage(language, confidence) }
}
